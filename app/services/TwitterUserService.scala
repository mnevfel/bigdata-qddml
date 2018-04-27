package services

import tables._
import types._
import helpers._
import databases.BigDataDb._
import slick.driver.MySQLDriver.api._
import play.api.libs.oauth._
import play.api.libs.json._

import databases.BigDataDb

import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext
import org.joda.time.DateTime

class TwitterUserService {
  def RegisterUserFromJSObject(obj: JsObject, ws: WSClient, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    val identity = (obj \ "id").as[Long];
    BigDataDb.TwitterUser.Filter(x => x.ID === identity).delete.runAsync.map(del => {
      val nwUser = new TwitterUser {
        Name = (obj \ "name").as[String];
        ScreenName = (obj \ "screen_name").as[String];
        Identity = identity;
        Token = (obj \ "token").as[String];
        Secret = (obj \ "secret").as[String];
        Location = (obj \ "location").asOpt[String];
        Description = (obj \ "description").asOpt[String];
      }

      BigDataDb.TwitterUser.Insert(nwUser).runAsync.map(id => {
        this.UpdateFollowers(id, ws, ec)
        this.UpdateFriends(id, true, ws, ec)
        this.UpdateStatuses(id, ws, ec)

        var keywordText = nwUser.Name + " " + nwUser.ScreenName
        if (nwUser.Location.isDefined)
          keywordText += " " + nwUser.Location.get
        if (nwUser.Description.isDefined)
          keywordText += " " + nwUser.Description.get
        TwitterServiceProvider.Analyze.Keywords(id, keywordText, ec)
      })
    })
  }

  def UpdateFollowers(id: Long, ws: WSClient, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    BigDataDb.TwitterUser.Filter(x => x.ID === id).result.headOption.runAsync.map(user => {
      if (user.isDefined) {
        var cursor: Long = -1
        val url = TwitterHelper.TwApi + "followers/ids.json?cursor={{cursor}}&user_id=" + user.get.Identity + "&count=5000"
        BigDataDb.TwitterFollow.Filter(x => x.UserID === id).delete.runAsync.map(del => {
          def update() {
            if (cursor != 0) {
              ws.url(url.replace("{{cursor}}", cursor.toString()))
                .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(
                  user.get.Token,
                  user.get.Secret)))
                .get
                .map(result => {
                  val obj = result.json.as[JsObject]
                  val ids = (obj \ "ids").as[List[Long]]
                  cursor = (obj \ "next_cursor").as[Long]
                  ids.foreach(x => {
                    BigDataDb.TwitterFollow.Insert(new TwitterFollow {
                      UserID = id;
                      Identity = x;
                    }).runAsync
                  })
                  update()
                })
            }
          }
          update()
        })
        this.UpdateRequestType(id, TwitterRequestType.GetFollowers)

      }
    })
  }

  def UpdateFriends(id: Long, permanent: Boolean, ws: WSClient, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    BigDataDb.TwitterUser.Filter(x => x.ID === id).result.headOption.runAsync.map(user => {
      if (user.isDefined) {
        var cursor: Long = -1
        val url = TwitterHelper.TwApi + "friends/ids.json?cursor={{cursor}}&user_id=" + user.get.Identity + "&count=5000"
        def update() {
          if (cursor != 0) {
            ws.url(url.replace("{{cursor}}", cursor.toString()))
              .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(
                user.get.Token,
                user.get.Secret)))
              .get
              .map(result => {
                val obj = result.json.as[JsObject]
                val ids = (obj \ "ids").as[List[Long]]
                cursor = (obj \ "next_cursor").as[Long]
                ids.foreach(identity => {
                  BigDataDb.TwitterFriend.Filter(x => x.Identity === identity)
                    .result.headOption.runAsync.map(friend => {
                      // If identity is already exists
                      if (friend.isDefined) {
                        // Update last call date for to learn is friend valid
                        val upFriend = friend.get.copy(LastCallDate = DateTime.now.getMillis())
                        BigDataDb.TwitterFriend.Update(upFriend).runAsync
                      } else {
                        BigDataDb.TwitterFriend.Insert(new TwitterFriend {
                          UserID = id;
                          Identity = identity;
                          Permanent = permanent;
                        }).runAsync
                      }
                    })
                })
                update()
              })
          }
        }
        update()
        this.UpdateRequestType(id, TwitterRequestType.GetFriends)
      }
    })
  }

  def UpdateStatuses(userId: Long, ws: WSClient, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    BigDataDb.TwitterUser.Filter(x => x.ID === userId).result.headOption.runAsync.map(user => {
      if (user.isDefined) {
        var sinceId: Long = 0
        val url = TwitterHelper.TwApi + "statuses/user_timeline.json?user_id=" + user.get.Identity + "{{since_id}}&include_rts=true&count=200"
        def update() {
          if (sinceId != -1) {
            var sinceIdQs: String = ""
            if (sinceId != 0)
              sinceIdQs = "&since_id=" + sinceId.toString()
            ws.url(url.replace("{{since_id}}", sinceIdQs))
              .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(
                user.get.Token,
                user.get.Secret)))
              .get
              .map(result => {
                val objs = result.json.as[JsArray]
                objs.value.foreach(obj => {
                  val id = (obj \ "id").as[Long]
                  val text = (obj \ "text").as[String]

                  this.InsertStatus(userId, id, text, ec)

                  if (id > sinceId) {
                    sinceId = id
                  }
                })
                if (objs.value.length == 0) {
                  sinceId = -1
                }
                update()
              })
          }
        }
        update()

        this.UpdateRequestType(userId, TwitterRequestType.GetStatuses)
      }
    })
  }

  def InsertStatus(userId: Long, identity: Long,
                   text: String, ec: ExecutionContext) {
    val status = BigDataDb.TwitterStatus
      .Filter(x => x.Identity === identity).result.headOption.run
    // If identity isn't already exists
    if (!status.isDefined) {
      BigDataDb.TwitterStatus.Insert(new TwitterStatus {
        Identity = identity;
        UserID = userId;
      }).run
      TwitterServiceProvider.Analyze.Keywords(userId, text, ec)
    }
  }

  def ClearInvalidFriends(id: Long, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    BigDataDb.TwitterRequest.Filter(x => x.UserID === id
      && x.RequestType === TwitterRequestType.GetFriends)
      .result.headOption.runAsync.map(request => {
        var limitCallDate = request.isDefined match {
          case true  => request.get.RequestDate - 3600000
          case false => DateTime.now.getMillis()
        }
        // If identity isn't friend for selected user since yesterday > remove friend
        BigDataDb.TwitterFriend.Filter(x => x.UserID === id
          && x.LastCallDate <= limitCallDate).delete.runAsync
      })
  }

  def ClearInvalidStatuses(id: Long, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    // If identity isn't friend for selected user since yesterday > remove friend
    BigDataDb.TwitterStatus.Filter(x => x.UserID === id
      && x.AnalyzeDate <= DateTime.now.minusDays(3).getMillis).delete.runAsync
  }

  def UpdateRequestType(id: Long, typeId: Short) {
    BigDataDb.TwitterRequest.Filter(x => x.UserID === id
      && x.RequestType === typeId).delete.run
    BigDataDb.TwitterRequest.Insert(new TwitterRequest {
      UserID = id;
      RequestType = typeId
    }).runAsync;
  }

  def InsertFriend(userId: Long, identity: Long, ws: WSClient, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    BigDataDb.TwitterUser.Filter(x => x.ID === userId)
      .result.headOption.runAsync.map(user => {
        if (user.isDefined) {
          ws.url(TwitterHelper.TwApi + "friendships/create.json?user_id=" + identity)
            .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(
              user.get.Token,
              user.get.Secret)))
            .post("ignored")
            .map(result => {
              BigDataDb.TwitterFriend.Any(x => x.UserID === userId
                && x.Identity === identity).runAsync.map(any => {
                if (!any) {
                  BigDataDb.TwitterFriend.Insert(new TwitterFriend {
                    UserID = userId;
                    Identity = identity;
                  }).runAsync
                }
              })
            })
        }
      })
  }

  def DeleteFriend(userId: Long, identity: Long, ws: WSClient, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    BigDataDb.TwitterUser.Filter(x => x.ID === userId)
      .result.headOption.runAsync.map(user => {
        if (user.isDefined) {
          ws.url(TwitterHelper.TwApi + "friendships/destroy.json?user_id=" + identity)
            .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(
              user.get.Token,
              user.get.Secret)))
            .post("ignored")
            .map(result => {
              BigDataDb.TwitterFriend.Filter(x => x.UserID === userId
                && x.Identity === identity).delete.runAsync
            })
        }
      })
  }
}