package tasks

import javax.inject.{ Inject, Named }

import akka.actor.{ ActorRef, ActorSystem }

import types._
import databases.BigDataDb._
import slick.driver.MySQLDriver.api._
import scala.concurrent.duration._
import play.api.libs.oauth._

import scala.concurrent.ExecutionContext
import helpers.TwitterHelper
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient

import databases.BigDataDb
import org.joda.time.DateTime
import services.TwitterServiceProvider

class TwitterAnalyzeTask @Inject() (ws: WSClient, actSys: ActorSystem)(implicit ec: ExecutionContext) {
  actSys.scheduler.schedule(
    initialDelay = 5.seconds,
    interval = 1.minutes) {
   
  }
}