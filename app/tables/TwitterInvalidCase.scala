package tables

import slick.driver.MySQLDriver.api._
import org.joda.time.DateTime

/**
 * Twitter TwitterInvalidCase Class For Orm
 */
case class TwitterInvalidCase(
  override val ID: Long   = 0,
  var Keyword:     String = "") extends Base

class TwitterInvalidCaseTable(tag: Tag) extends BaseTable[TwitterInvalidCase](tag, "TwitterInvalidCase") {
  val Keyword: Rep[String] = column[String]("Keyword")

  def * = (ID, Keyword) <> (TwitterInvalidCase.tupled, TwitterInvalidCase.unapply)
}