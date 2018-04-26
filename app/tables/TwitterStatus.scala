package tables

import slick.driver.MySQLDriver.api._
import org.joda.time.DateTime

/**
 * Twitter TwitterStatus Relation Class For Orm
 */
case class TwitterStatus(
  override val ID:   Long   = 0,
  var UserID:        Long   = 0,
  var Identity:      Long   = 0,
  var Text:          String = "",
  var LastCallDate:  Long   = DateTime.now.getMillis()) extends Base

class TwitterStatusTable(tag: Tag) extends BaseTable[TwitterStatus](tag, "TwitterStatus") {
  val UserID: Rep[Long] = column[Long]("UserID")
  val Identity: Rep[Long] = column[Long]("Identity")
  val Text: Rep[String] = column[String]("Text")
  val LastCallDate: Rep[Long] = column[Long]("LastCallDate")

  def * = (ID, UserID, Identity, Text, LastCallDate) <> (TwitterStatus.tupled, TwitterStatus.unapply)
}