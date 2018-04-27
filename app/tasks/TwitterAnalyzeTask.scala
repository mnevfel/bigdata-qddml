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
    val limitDate = DateTime.now().minusDays(1).getMillis
    val ids = BigDataDb.TwitterUser.table
      .joinLeft(BigDataDb.TwitterRequest
        .Filter(x => x.RequestType === TwitterRequestType.GetStatuses))
      .on(_.ID === _.UserID)
      .groupBy(x => x._1)
      .map(x => (x._1, x._2.map(y => y._2.map(z => z.RequestDate)).max))
      .filter(x => x._2 <= limitDate).map(x => x._1)
      .result.runAsync.map(users => {
        users.foreach(user => {
          TwitterServiceProvider.Analyze.AnalyzeStatuses(user.ID, ws, ec)
        })
      })
  }
}