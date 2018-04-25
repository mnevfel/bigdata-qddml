package controllers

import tables._

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent._
import play.api.libs.oauth._
import play.api.libs.json._

import services.TwitterServiceProvider
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
          ws.url(TwitterHelper.TwApi + "account/verify_credentials.json")
            .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(t.token, t.secret)))
            .get
            .map(result => {
              //Save to database from json object.
              val obj = result.json.as[JsObject] + ("token" -> JsString(t.token)) + ("secret" -> JsString(t.secret))
              TwitterServiceProvider.User.RegisterUserFromJSObject(ws, obj, ec)
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
}
