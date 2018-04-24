package tasks

import javax.inject.{ Inject, Named }

import akka.actor.{ ActorRef, ActorSystem }

import slick.driver.MySQLDriver.api._
import scala.concurrent.duration._
import play.api.libs.oauth._

import scala.concurrent.ExecutionContext
import helpers.TwitterHelper
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient

import databases.BigDataDb
import databases.BigDataDb._

class TwitterTask @Inject() (ws: WSClient, actSys: ActorSystem)(implicit executionContext: ExecutionContext) {
  actSys.scheduler.schedule(
    initialDelay = 5.seconds,
    interval = 30.seconds) {
    //    val users = BigDataDb.TwitterUser.table.result.run
    //    users.foreach(user => {
    //      ws.url(TwitterHelper.TwApi + "account/verify_credentials.json?include_email=true")
    //        .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(user.Token, user.Secret.getOrElse(""))))
    //        .get
    //        .map(result => {
    //          val obj = result.json.as[JsObject]
    //          println(obj.toString())
    //        })
    //    })
  }
}