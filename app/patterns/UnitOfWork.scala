package patterns

import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import play.api.Play

/**
 * Multiple Database Management Pattern
 */
abstract class UnitOfWork(dbName: String) {
  private lazy val _db = DatabaseConfigProvider.get[JdbcProfile](dbName)(Play.current).db
  implicit class ExtRunner[R](action: DBIOAction[R, NoStream, Nothing]) {
    def run: R = {
      Await.result(_db.run(action), Duration.Inf)
    }
    def runAsync: Future[R] = {
      _db.run(action)
    }
    def handleRun: Option[R] = {
      try {
        Some(this.run)
      } catch {
        case ex => {
          println(ex.getMessage)
          None
        }
      }
    }
    def handleRunAsync(callback: (Option[R]) => Unit = (None) => {}) {
      _db.run(action.asTry).map {
        case Failure(ex) => {
          println(println(ex.getMessage))
          callback(None)
        }
        case Success(op) => {
          callback(Some(op))
        }
      }
    }
  }
}