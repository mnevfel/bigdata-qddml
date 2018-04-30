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
  // Anaylze RealTime Tweet Keywords -> Trigger
  actSys.scheduler.schedule(
    initialDelay = 1.minutes,
    interval = 1.minutes) {
    val limitDate = DateTime.now().minusHours(6).getMillis
    BigDataDb.TwitterUser.table
      .joinLeft(BigDataDb.TwitterRequest
        .Filter(x => x.RequestType === TwitterRequestType.AnalyzeStatuses))
      .on(_.ID === _.UserID)
      .groupBy(x => x._1)
      .map(x => (x._1, x._2.map(y => y._2.map(z => z.RequestDate)).max.getOrElse(limitDate)))
      .filter(x => x._2 <= limitDate).map(x => x._1)
      .map(x => x.ID).result.runAsync.map(ids => {
        ids.foreach(id => {
          TwitterServiceProvider.Api.AnalyzeStatuses(id, ws, ec)
        })
      })
  }
}