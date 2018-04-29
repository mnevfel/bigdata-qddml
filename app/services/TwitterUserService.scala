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
  // Register New Api User Auth -> With JsObject : Returned From Tw Api
  def RegisterUserFromJSObject(obj: JsObject, ws: WSClient, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    val identity = (obj \ "id").as[Long];
    BigDataDb.TwitterUser.Filter(x => x.Identity === identity).delete.runAsync.map(del => {
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
        TwitterServiceProvider.Api.UpdateFollowers(id, ws, ec)
        TwitterServiceProvider.Api.UpdateFriends(id, true, ws, ec)
        TwitterServiceProvider.Api.ImportTimeLine(id, ws, ec)
        this.UpdateRequestType(id, true, TwitterRequestType.AnalyzeStatuses, ec)

        var keywordText = nwUser.Name + " " + nwUser.ScreenName
        if (nwUser.Location.isDefined)
          keywordText += " " + nwUser.Location.get
        if (nwUser.Description.isDefined)
          keywordText += " " + nwUser.Description.get
        this.InsertKeywords(id, keywordText, ec)
      })
    })
  }

  // Insert New Tweet With Identity & Analyze and Import Tweet Keywords
  def InsertStatus(userId: Long, obj: JsObject, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    val identity = (obj \ "id").as[Long]
    val text = (obj \ "text").as[String]
    val user = (obj \ "user").as[JsObject]
    val targetId = (user \ "id").as[Long]
    this.InsertTarget(userId, targetId, ec)
    BigDataDb.TwitterStatus
      .Filter(x => x.Identity === identity)
      .result.headOption.runAsync.map(status => {
        // If identity isn't already exists
        if (!status.isDefined) {
          BigDataDb.TwitterStatus.Insert(new TwitterStatus {
            Identity = identity;
            UserID = userId;
          }).runAsync
          this.InsertKeywords(userId, text, ec)
        }
      })
  }

  // Insert Analyzed Keyword To Rank Table
  def InsertKeywords(userId: Long, text: String, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    val split = text.split(" ")
    split.foreach(partial => {
      if (!partial.isEmpty()) {
        val rank = TextHelper.GetRank(partial)
        val keyword = TextHelper.Normalize(partial)
        if (TextHelper.IsValid(keyword)) {
          BigDataDb.TwitterInvalidCase.Any(x => x.Keyword === keyword)
            .runAsync.map(any => {
              if (!any) {
                BigDataDb.TwitterKeyword.Filter(x => x.UserID === userId
                  && (x.Keyword === keyword
                    || (x.Keyword like "%" + keyword + "%")))
                  .result.runAsync.map(keys => {
                    keys.foreach(key => {
                      key.Rank += rank
                      BigDataDb.TwitterKeyword.Update(key).runAsync
                    })
                    if (keys.length == 0) {
                      BigDataDb.TwitterKeyword.Insert(new TwitterKeyword {
                        UserID = userId;
                        Keyword = keyword;
                        Rank = rank;
                      }).runAsync
                    }
                  })
              }
            })
        }
      }
    })
  }

  def InsertTarget(userId: Long, identity: Long, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    BigDataDb.TwitterUser.Filter(x => x.ID === userId)
      .map(x => x.Identity).result.headOption.runAsync.map(userIdentity => {
        if (userIdentity.isDefined) {
          if (userIdentity.get != identity) {
            BigDataDb.TwitterTarget.Any(x => x.UserID === userId
              && x.Identity === identity).runAsync.map(any => {
              if (!any) {
                BigDataDb.TwitterTarget.Insert(new TwitterTarget {
                  UserID = userId;
                  Identity = identity;
                }).runAsync
              }
            })
          }
        }
      })
  }

  // Clear Statuses(Remove Permanent) -> AnalyzeDate, Older Than 10 Days
  def ClearInvalidStatuses(id: Long, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    // If status analyze date before 10 days then now > remove
    BigDataDb.TwitterStatus.Filter(x => x.UserID === id
      && x.AnalyzeDate <= DateTime.now.minusDays(10).getMillis).delete.runAsync
  }

  // Clear Friends(Remove Permanent) -> RequestDate, Older Than 1 Hour(3600000)
  def ClearInvalidFriends(id: Long, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    BigDataDb.TwitterRequest.Filter(x => x.UserID === id
      && x.RequestType === TwitterRequestType.GetFriends)
      .result.headOption.runAsync.map(request => {
        var limitCallDate = request.isDefined match {
          case true  => request.get.RequestDate - 3600000
          case false => DateTime.now.getMillis()
        }
         // If friend last call date before 1 hour then request date > remove
        BigDataDb.TwitterFriend.Filter(x => x.UserID === id
          && x.LastCallDate <= limitCallDate).delete.runAsync
      })
  }

  // Update Request Type By UserID
  def UpdateRequestType(id: Long, first: Boolean, typeId: Short, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    BigDataDb.TwitterRequest.Filter(x => x.UserID === id
      && x.RequestType === typeId).delete.runAsync.map(res => {
      var request = new TwitterRequest {
        UserID = id;
        RequestType = typeId;
      }
      if (first)
        request.RequestDate = DateTime.now.minusMinutes(55).getMillis
      BigDataDb.TwitterRequest.Insert(request).runAsync;
    })
  }
}