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

class TwitterTargetTask @Inject() (ws: WSClient, actSys: ActorSystem)(implicit ec: ExecutionContext) {
  // FollowTarget
  actSys.scheduler.schedule(
    initialDelay = 5.seconds,
    interval = 20.seconds) {
    val limitDate = DateTime.now().minusMinutes(2).getMillis
    BigDataDb.TwitterUser.table
      .joinLeft(BigDataDb.TwitterRequest
        .Filter(x => x.RequestType === TwitterRequestType.PostFollow))
      .on(_.ID === _.UserID)
      .groupBy(x => x._1)
      .map(x => (x._1, x._2.map(y => y._2.map(z => z.RequestDate)).max.getOrElse(limitDate)))
      .filter(x => x._2 <= limitDate).map(x => x._1.ID)
      .result.runAsync.map(response => response.foreach(userId => {
        BigDataDb.TwitterTarget.Filter(x => x.UserID === userId
          && !x.DeleteDate.isDefined
          && BigDataDb.TwitterFriend.Filter(y => y.UserID === userId
            && y.Identity === x.Identity).length === 0)
          .result.headOption.runAsync.map(target => {
            if (target.isDefined) {
              TwitterServiceProvider.Api.InsertFriend(userId, target.get.Identity, ws, ec)
            }
          })
      }))
  }

  // UnFollowTarget
  actSys.scheduler.schedule(
    initialDelay = 5.seconds,
    interval = 20.seconds) {
    val limitDate = DateTime.now().minusHours(3).getMillis
    val limitFriendDate = DateTime.now().minusHours(6).getMillis
    BigDataDb.TwitterUser.table
      .joinLeft(BigDataDb.TwitterRequest
        .Filter(x => x.RequestType === TwitterRequestType.PostUnFollow))
      .on(_.ID === _.UserID)
      .groupBy(x => x._1)
      .map(x => (x._1, x._2.map(y => y._2.map(z => z.RequestDate)).max.getOrElse(limitDate)))
      .filter(x => x._2 <= limitDate).map(x => x._1.ID)
      .result.runAsync.map(response => response.foreach(userId => {
        BigDataDb.TwitterFriend.Filter(x => x.UserID === userId
          && x.FriendDate <= limitFriendDate
          && !x.Permanent
          && BigDataDb.TwitterTarget.Filter(y => y.UserID === userId
            && y.DeleteDate.isDefined
            && y.Identity === x.Identity).length === 0)
          .sortBy(x=> x.FriendDate).take(100).result.runAsync.map(friends => {
            friends.foreach(friend => {
              TwitterServiceProvider.Api.DeleteFriend(userId, friend.Identity, ws, ec)
            })
          })
      }))
  }

  //ClearDeletedTargets
  actSys.scheduler.schedule(
    initialDelay = 5.seconds,
    interval = 1.minutes) {
    val limitDate = DateTime.now().minusDays(30).getMillis
    BigDataDb.TwitterTarget.Filter(x => x.DeleteDate <= limitDate).delete.runAsync
  }
}