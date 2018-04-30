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

class TwitterApiService {
  // Get Current Followers For User Account From Tw Api & Update Them On Db
  def UpdateFollowers(userId: Long, ws: WSClient, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    BigDataDb.TwitterUser.Filter(x => x.ID === userId).result.headOption.runAsync.map(user => {
      if (user.isDefined) {
        var cursor: Long = -1
        val url = TwitterHelper.TwApi + "followers/ids.json?cursor={{cursor}}&user_id=" + user.get.Identity + "&count=5000"
        BigDataDb.TwitterFollow.Filter(x => x.UserID === userId).delete.runAsync.map(del => {
          def update() {
            if (cursor != 0) {
              ws.url(url.replace("{{cursor}}", cursor.toString()))
                .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(
                  user.get.Token,
                  user.get.Secret)))
                .get
                .map(result => {
                  println("Get followers : " + result.statusText)
                  if (result.status != 401) {
                    val obj = result.json.as[JsObject]
                    val ids = (obj \ "ids").as[List[Long]]
                    cursor = (obj \ "next_cursor").as[Long]
                    ids.foreach(id => {
                      BigDataDb.TwitterFollow.Insert(new TwitterFollow {
                        UserID = userId;
                        Identity = id;
                      }).runAsync
                    })
                    update()
                  }
                })
            }
          }
          update()
        })
        TwitterServiceProvider.User.UpdateRequestType(userId, false, TwitterRequestType.GetFollowers, ec)
      }
    })
  }

  // Get Current Friends For User Account From Tw Api & Update Them On Db
  def UpdateFriends(userId: Long, permanent: Boolean, ws: WSClient, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    BigDataDb.TwitterUser.Filter(x => x.ID === userId).result.headOption.runAsync.map(fuser => {
      if (fuser.isDefined) {
        var cursor: Long = -1
        val url = TwitterHelper.TwApi + "friends/list.json?cursor={{cursor}}&user_id=" + fuser.get.Identity + "&count=5000"
        def update() {
          if (cursor != 0) {
            ws.url(url.replace("{{cursor}}", cursor.toString()))
              .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(
                fuser.get.Token,
                fuser.get.Secret)))
              .get
              .map(result => {
                println("Get friends : " + result.statusText)
                if (result.status != 401) {
                  val obj = result.json.as[JsObject]
                  val users = (obj \ "users").as[JsArray]
                  cursor = (obj \ "next_cursor").as[Long]
                  users.value.foreach(user => {
                    val identity = (user \ "id").as[Long]
                    val name = (user \ "name").as[String]
                    val screen = (user \ "screen_name").as[String]
                    val desc = (user \ "description").asOpt[String]

                    var keywordText = identity + " " + name + " " + screen
                    if (desc.isDefined)
                      keywordText += " " + desc.get

                    BigDataDb.TwitterFriend.Filter(x => x.UserID === fuser.get.ID
                      && x.Identity === identity)
                      .result.headOption.runAsync.map(friend => {
                        // If identity is already exists
                        if (friend.isDefined) {
                          // Update last call date for to learn is friend valid
                          val upFriend = friend.get.copy(LastCallDate = DateTime.now.getMillis())
                          BigDataDb.TwitterFriend.Update(upFriend).runAsync
                        } else {
                          BigDataDb.TwitterFriend.Insert(new TwitterFriend {
                            UserID = userId;
                            Identity = identity;
                            Permanent = permanent;
                          }).runAsync
                          TwitterServiceProvider.User.InsertKeywords(userId, keywordText, ec)
                        }
                      })
                  })
                  update()
                }
              })
          }
        }
        update()
        TwitterServiceProvider.User.UpdateRequestType(userId, false, TwitterRequestType.GetFriends, ec)
      }
    })
  }

  def InsertFriend(userId: Long, identity: Long, ws: WSClient, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    BigDataDb.TwitterUser.Filter(x => x.ID === userId)
      .result.headOption.runAsync.map(user => {
        if (user.isDefined) {
          ws.url(TwitterHelper.TwApi + "friendships/create.json?user_id=" + identity + "&follow=false")
            .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(
              user.get.Token,
              user.get.Secret)))
            .post("ignore")
            .map(result => {
              println("Insert friend : " + result.statusText)
              if (result.status != 401) {
                BigDataDb.TwitterFriend.Any(x => x.UserID === userId
                  && x.Identity === identity).runAsync.map(any => {
                  if (!any) {
                    BigDataDb.TwitterFriend.Insert(new TwitterFriend {
                      UserID = userId;
                      Identity = identity;
                    }).runAsync
                  }
                })
              }
            })
          TwitterServiceProvider.User.UpdateRequestType(userId, false, TwitterRequestType.PostFollow, ec)
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
              println("Delete friend : " + result.statusText)
              if (result.status != 401) {
                BigDataDb.TwitterFriend.Filter(x => x.UserID === userId
                  && x.Identity === identity).delete.runAsync
                BigDataDb.TwitterTarget.Filter(x => x.UserID === userId
                  && x.Identity === identity)
                  .result.headOption.runAsync.map(target => {
                    if (target.isDefined) {
                      val nwTarget = target.get.copy(DeleteDate = Some(DateTime.now.getMillis))
                      BigDataDb.TwitterTarget.Update(nwTarget).runAsync
                    } else {
                      val nwTarget = new TwitterTarget {
                        UserID = userId;
                        Identity = identity;
                        DeleteDate = Some(DateTime.now().getMillis);
                      }
                      BigDataDb.TwitterTarget.Insert(nwTarget).runAsync
                    }
                  })
              }
            })
          TwitterServiceProvider.User.UpdateRequestType(userId, false, TwitterRequestType.PostUnFollow, ec)
        }
      })
  }

  // Insert Current Timeline Statuses For User Account From Tw Api & Update Them On Db
  def ImportTimeLine(userId: Long, ws: WSClient, ec: ExecutionContext) {
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

            val repUrl = url.replace("{{since_id}}", sinceIdQs)
            println("TimeLine Analyzing -> " + repUrl)
            ws.url(repUrl)
              .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(
                user.get.Token,
                user.get.Secret)))
              .get
              .map(result => {
                println("Import timeline : " + result.statusText)
                if (result.status != 401) {
                  val objs = result.json.as[JsArray]
                  objs.value.foreach(obj => {
                    val id = (obj \ "id").as[Long]
                    TwitterServiceProvider.User.InsertStatus(userId, obj.as[JsObject], ec)

                    if (id > sinceId) {
                      sinceId = id
                    }
                  })
                  if (objs.value.length == 0) {
                    sinceId = -1
                  }
                  update()
                }
              })
          } else {
            println("TimeLine Analyze completed.")
          }
        }
        update()

        TwitterServiceProvider.User.UpdateRequestType(userId, false, TwitterRequestType.GetStatuses, ec)
      }
    })
  }

  // Getting realtime statuses from tw api, analyze and import keywords
  def AnalyzeStatuses(userId: Long, ws: WSClient, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec

    BigDataDb.TwitterUser.Filter(x => x.ID === userId)
      .result.headOption.runAsync.map(user => {
        if (user.isDefined) {
          BigDataDb.TwitterKeyword.Filter(x => x.UserID === userId)
            .groupBy(x => x.Keyword)
            .map(x => (x._1, x._2.map(y => y.Rank).sum))
            .sortBy(x => x._2.desc).take(5)
            .map(x => x._1).result.runAsync.map(keywords => {
              keywords.foreach(keyword => {
                var sinceId: Long = 0
                val url = TwitterHelper.TwApi + "search/tweets.json?q=" + keyword + "{{since_id}}&count=100"
                def update() {
                  if (sinceId != -1) {
                    var sinceIdQs: String = ""
                    if (sinceId != 0)
                      sinceIdQs = "&since_id=" + sinceId.toString()

                    val repUrl = url.replace("{{since_id}}", sinceIdQs)
                    println("Analyzing -> " + repUrl)
                    ws.url(repUrl)
                      .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(
                        user.get.Token,
                        user.get.Secret)))
                      .get
                      .map(result => {
                        println("Analyze statues : " + result.statusText)
                        if (result.status != 401) {
                          val obj = result.json.as[JsObject]
                          val statuses = (obj \ "statuses").as[JsArray]
                          statuses.value.foreach(status => {
                            val id = (status \ "id").as[Long]
                            TwitterServiceProvider.User.InsertStatus(userId, status.as[JsObject], ec)

                            if (id > sinceId) {
                              sinceId = id
                            }
                          })

                          if (statuses.value.length == 0) {
                            sinceId = -1
                          }
                          update()
                        }
                      })
                  } else {
                    println("Analyze completed.")
                  }
                }
                update()
              })
            })
          TwitterServiceProvider.User.UpdateRequestType(userId, false, TwitterRequestType.AnalyzeStatuses, ec)
        }
      })
  }
}