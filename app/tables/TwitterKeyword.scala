package tables

import slick.driver.MySQLDriver.api._
import org.joda.time.DateTime

/**
 * Twitter TwitterKeyword Relation Class For Orm
 */
case class TwitterKeyword(
  override val ID: Long   = 0,
  var UserID:      Long   = 0,
  var Rank:        Long   = 0,
  var Keyword:     String = "") extends Base

class TwitterKeywordTable(tag: Tag) extends BaseTable[TwitterKeyword](tag, "TwitterKeyword") {
  val UserID: Rep[Long] = column[Long]("UserID")
  val Rank: Rep[Long] = column[Long]("Rank")
  val Keyword: Rep[String] = column[String]("Keyword")

  def * = (ID, UserID, Rank, Keyword) <> (TwitterKeyword.tupled, TwitterKeyword.unapply)
}