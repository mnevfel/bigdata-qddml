package helpers

import play.api.libs.oauth._
import play.api.mvc._
import play.Play


/**
 * Twitter Helper Methods Here
 * */
object TwitterHelper {
  val key = Play.application().configuration()
    .getString("twitter.consumer.key")
  val secret = Play.application().configuration()
    .getString("twitter.consumer.secret")
  val TwitterKey = ConsumerKey(key, secret)
  val TwitterOAuth = OAuth(
    ServiceInfo(
      "https://api.twitter.com/oauth/request_token",
      "https://api.twitter.com/oauth/access_token",
      "https://api.twitter.com/oauth/authorize", TwitterKey),
    true)
  def TwitterPair(implicit request: RequestHeader): Option[RequestToken] = {
    for {
      token <- request.session.get("token")
      secret <- request.session.get("secret")
    } yield {
      RequestToken(token, secret)
    }
  }
  val TwApi = "https://api.twitter.com/1.1/"
  val TwCallBackUrl = DomainHelper.Host + "twitter/auth"
}