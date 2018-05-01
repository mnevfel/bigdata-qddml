package tables

import slick.driver.MySQLDriver.api._
import org.joda.time.DateTime

/**
 * Twitter TwitterLog Relation Class For Orm
 */
case class TwitterLog(
  override val ID: Long   = 0,
  var UserID:      Long   = 0,
  var Message:     String = "",
  var Date:        Long   = DateTime.now.getMillis()) extends Base

class TwitterLogTable(tag: Tag) extends BaseTable[TwitterLog](tag, "TwitterLog") {
  val UserID: Rep[Long] = column[Long]("UserID")
  val Message: Rep[String] = column[String]("Message")
  val Date: Rep[Long] = column[Long]("Date")

  def * = (ID, UserID, Message, Date) <> (TwitterLog.tupled, TwitterLog.unapply)
}