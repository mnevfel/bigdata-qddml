package controllers

import tables._

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent._
import play.api.libs.oauth._
import play.api.libs.json._
import slick.driver.MySQLDriver.api._

import services.TwitterServiceProvider
import play.api.libs.ws.WSClient
import helpers.TwitterHelper

import databases.BigDataDb
import databases.BigDataDb._
import play.api.mvc.Results.EmptyContent

@Singleton
class TwitterController @Inject() (ws: WSClient, cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  def index = Action.async { request: Request[AnyContent] =>
    request.getQueryString("oauth_verifier").map { verifier =>
      // Runs > If callback returns with verifier
      val tokenPair = TwitterHelper.TwitterPair(request).get
      TwitterHelper.TwitterOAuth.retrieveAccessToken(tokenPair, verifier) match {
        case Right(t) => {
          ws.url(TwitterHelper.TwApi + "account/verify_credentials.json")
            .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(t.token, t.secret)))
            .get
            .map(result => {
              //Save to database from json object.
              val obj = result.json.as[JsObject] + ("token" -> JsString(t.token)) + ("secret" -> JsString(t.secret))
              TwitterServiceProvider.User.RegisterUserFromJSObject(obj, ws, ec)
              Redirect("/")
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

  def dash = Action.async { request: Request[AnyContent] =>
    BigDataDb.TwitterUser.table.result.runAsync.map((users) => {
      var dashText = ""
      users.foreach(user => {
        val keywords = BigDataDb.TwitterKeyword.Filter(x => x.UserID === user.ID)
          .groupBy(x => x.Keyword)
          .map(x => (x._1, x._2.map(y => y.Rank).sum))
          .sortBy(x => x._2.desc).take(5)
          .map(x => x._1).result.run

        dashText = user.ScreenName + " : |"
        keywords.foreach(keyword => {
          dashText += keyword + "|"
        })

        val friendLen = BigDataDb.TwitterFriend
          .Filter(x => x.UserID === user.ID
            && x.Permanent).length.result.run
        val followedLen = BigDataDb.TwitterFriend
          .Filter(x => x.UserID === user.ID
            && !x.Permanent).length.result.run
        val followLen = BigDataDb.TwitterFollow
          .Filter(x => x.UserID === user.ID).length.result.run
        val unFollowLen = BigDataDb.TwitterTarget
          .Filter(x => x.UserID === user.ID
            && x.DeleteDate.isDefined).length.result.run

        dashText += "Fri(P):" + friendLen + "|Fri(!P):" + followedLen + "|Fo:" + followLen + "|UnFo:" + unFollowLen
        dashText += "</br>"
      })

      Ok(dashText)
    })
  }
}
