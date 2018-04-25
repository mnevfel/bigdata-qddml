package services

import tables._
import types._
import databases.BigDataDb._
import slick.driver.MySQLDriver.api._
import play.api.libs.oauth._

import helpers.TwitterHelper
import databases.BigDataDb

import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext

class TwitterUserService {
  def RegisterUserFromJSObject(ws: WSClient, obj: JsObject, ec: ExecutionContext) {
    val identity = (obj \ "id").as[Long];
    BigDataDb.TwitterUser.Filter(x => x.ID === identity).delete.run
    val id = BigDataDb.TwitterUser.Insert(new TwitterUser {
      Name = (obj \ "name").as[String];
      ScreenName = (obj \ "screen_name").as[String];
      Identity = identity;
      Token = (obj \ "token").as[String];
      Secret = (obj \ "secret").as[String];
      Location = (obj \ "location").asOpt[String];
      Description = (obj \ "description").asOpt[String];
    }).run
    this.UpdateRequest(id, TwitterFollowType.Friend, ws, ec)
    this.UpdateRequest(id, TwitterFollowType.Follower, ws, ec)
  }

  def UpdateRequest(id: Long, typeId: Short, ws: WSClient, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    val url = typeId match {
      case TwitterFollowType.Follower => "followers"
      case TwitterFollowType.Friend   => "friends"
    }
    val user = BigDataDb.TwitterUser.Filter(x => x.ID === id).result.headOption.run
    if (user.isDefined) {
      ws.url(TwitterHelper.TwApi + url + "/ids.json?cursor=-1&user_id=" + user.get.Identity + "&count=5000")
        .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(
          user.get.Token,
          user.get.Secret)))
        .get
        .map(result => {
          val obj = result.json.as[JsObject]
          val ids = (obj \ "ids").as[List[Long]];
          BigDataDb.TwitterFollow.Filter(x => x.UserID === id).delete.run
          ids.foreach(x => {
            BigDataDb.TwitterFollow.Insert(new TwitterFollow {
              UserID = id;
              Identity = x;
              FollowType = typeId;
            }).run
          })
        })
      this.UpdateRequestType(id, typeId)
    }
  }

  def UpdateRequestType(id: Long, typeId: Short) {
    BigDataDb.TwitterRequest.Filter(x => x.UserID === id
      && x.RequestType === typeId).delete.run
    BigDataDb.TwitterRequest.Insert(new TwitterRequest {
      UserID = id;
      RequestType = typeId
    }).run;
  }
}