package tables

import slick.driver.MySQLDriver.api._
import org.joda.time.DateTime

/**
 * Twitter TwitterTarget Relation Class For Orm
 */
case class TwitterTarget(
  override val ID:  Long         = 0,
  var UserID:       Long         = 0,
  var Identity:     Long         = 0,
  var DeleteDate: Option[Long] = None) extends Base

class TwitterTargetTable(tag: Tag) extends BaseTable[TwitterTarget](tag, "TwitterTarget") {
  val UserID: Rep[Long] = column[Long]("UserID")
  val Identity: Rep[Long] = column[Long]("Identity")
  val DeleteDate: Rep[Option[Long]] = column[Option[Long]]("DeleteDate")

  def * = (ID, UserID, Identity, DeleteDate) <> (TwitterTarget.tupled, TwitterTarget.unapply)
}