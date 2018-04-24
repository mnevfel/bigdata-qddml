package controllers

import tables._

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent._
import play.api.libs.oauth._
import play.api.libs.json._

import play.api.libs.ws.WSClient
import helpers.TwitterHelper

import databases.BigDataDb
import databases.BigDataDb._

@Singleton
class TwitterController @Inject() (ws: WSClient, cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  def index = Action.async { request: Request[AnyContent] =>
    request.getQueryString("oauth_verifier").map { verifier =>
      // Runs > If callback returns with verifier
      val tokenPair = TwitterHelper.TwitterPair(request).get
      TwitterHelper.TwitterOAuth.retrieveAccessToken(tokenPair, verifier) match {
        case Right(t) => {
          ws.url(TwitterHelper.TwApi + "account/verify_credentials.json?include_email=true")
            .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(t.token, t.secret)))
            .get
            .map(result => {
              //Save to database from json object.
              val obj = result.json.as[JsObject]
              BigDataDb.TwitterUser.Insert(new TwitterUser {
                Name = (obj \ "name").as[String];
                ScreenName = (obj \ "screen_name").as[String];
                Identity = (obj \ "id_str").as[String];
                Token = t.token;
                Secret = Some(t.secret);
                FriendsCount = (obj \ "friends_count").as[Long];
                FollowersCount = (obj \ "followers_count").as[Long];
                StatusesCount = (obj \ "statuses_count").as[Long];
                Location = (obj \ "location").asOpt[String];
                Description = (obj \ "description").asOpt[String];
              }).run

              Ok("User saved.")
            })
        }
        case _ => { Future.successful(Forbidden("Key not found.")) }
      }
    }.getOrElse(
      // Runs > If first request is calling
      TwitterHelper.TwitterOAuth.retrieveRequestToken(TwitterHelper.TwCallBackUrl) match {
        case Right(t) => {
          Future.successful(Redirect(TwitterHelper.TwitterOAuth.redirectUrl(t.token))
            .withSession("token" -> t.token, "secret" -> t.secret))
        }
        case Left(e) => { Future.successful(NotFound("Url isn't match.")) }
      })
  }
}
