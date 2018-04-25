package tables

import slick.driver.MySQLDriver.api._
import org.joda.time.DateTime

/**
 * Twitter TwitterRequest Relation Class For Orm
 */
case class TwitterRequest(
  override val ID: Long = 0,
  var UserID:      Long = 0,
  var RequestType: Short  = 0,
  var RequestDate: Long = DateTime.now().getMillis) extends Base

class TwitterRequestTable(tag: Tag) extends BaseTable[TwitterRequest](tag, "TwitterRequest") {
  val UserID: Rep[Long] = column[Long]("UserID")
  val RequestType: Rep[Short] = column[Short]("RequestType")
  val RequestDate: Rep[Long] = column[Long]("RequestDate")

  def * = (ID, UserID, RequestType, RequestDate) <> (TwitterRequest.tupled, TwitterRequest.unapply)
}