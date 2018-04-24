package tasks

import javax.inject.{ Inject, Named }

import akka.actor.{ ActorRef, ActorSystem }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class TwitterTask @Inject() (actSys: ActorSystem)(implicit executionContext: ExecutionContext) {
  actSys.scheduler.schedule(
    initialDelay = 5.seconds,
    interval = 30.seconds) {

  }
}