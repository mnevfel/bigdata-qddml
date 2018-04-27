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

class TwitterUserTask @Inject() (ws: WSClient, actSys: ActorSystem)(implicit ec: ExecutionContext) {
  // GetFollowers
  actSys.scheduler.schedule(
    initialDelay = 5.seconds,
    interval = 1.minutes) {
    val yesterday = DateTime.now().minusDays(1).getMillis
    val ids = BigDataDb.TwitterUser.table
      .joinLeft(BigDataDb.TwitterRequest
        .Filter(x => x.RequestType === TwitterRequestType.GetFollowers))
      .on(_.ID === _.UserID)
      .groupBy(x => x._1)
      .map(x => (x._1, x._2.map(y => y._2.map(z => z.RequestDate)).max))
      .filter(x => x._2 <= yesterday).map(x => x._1.ID)
      .result.runAsync.map(response => response.foreach(userId => {
        TwitterServiceProvider.User.UpdateFollowers(userId, ws, ec)
      }))
  }

  // GetFriends
  actSys.scheduler.schedule(
    initialDelay = 5.seconds,
    interval = 1.minutes) {
    val yesterday = DateTime.now().minusDays(1).getMillis
    val ids = BigDataDb.TwitterUser.table
      .joinLeft(BigDataDb.TwitterRequest
        .Filter(x => x.RequestType === TwitterRequestType.GetFriends))
      .on(_.ID === _.UserID)
      .groupBy(x => x._1)
      .map(x => (x._1, x._2.map(y => y._2.map(z => z.RequestDate)).max))
      .filter(x => x._2 <= yesterday).map(x => x._1.ID)
      .result.runAsync.map(response => response.foreach(userId => {
        TwitterServiceProvider.User.UpdateFriends(userId, false, ws, ec)
      }))
  }
  
  // ClearInvalidFriends => { "If user's friends not respond from tw, clear them from db." }
  actSys.scheduler.schedule(
    initialDelay = 5.seconds,
    interval = 1.minutes) {
    BigDataDb.TwitterUser.table.result.runAsync.map(response => {
      response.foreach(user => {
        TwitterServiceProvider.User.ClearInvalidFriends(user.ID, ec)
      })
    })
  }
  
  // ClearInvalidStatuses => { "If user's analyzed statuses older than 3 days, clear them from db." }
  actSys.scheduler.schedule(
    initialDelay = 5.seconds,
    interval = 1.minutes) {
    BigDataDb.TwitterUser.table.result.runAsync.map(response => {
      response.foreach(user => {
        TwitterServiceProvider.User.ClearInvalidStatuses(user.ID, ec)
      })
    })
  }
}