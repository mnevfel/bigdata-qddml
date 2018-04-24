package patterns

import slick.driver.MySQLDriver.api._

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import play.api.Play
import scala.concurrent._
import scala.concurrent.duration._

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
        case ex => None
      }
    }
    def handleRunAsync: Option[Future[R]] = {
      try {
        Some(this.runAsync)
      } catch {
        case ex => None
      }
    }
  }
}