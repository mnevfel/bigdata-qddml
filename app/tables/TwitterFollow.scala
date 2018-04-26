package tables

import slick.driver.MySQLDriver.api._
import org.joda.time.DateTime

/**
 * Twitter TwitterFollow Relation Class For Orm
 */
case class TwitterFollow(
  override val ID: Long  = 0,
  var UserID:      Long  = 0,
  var Identity:    Long  = 0) extends Base

class TwitterFollowTable(tag: Tag) extends BaseTable[TwitterFollow](tag, "TwitterFollow") {
  val UserID: Rep[Long] = column[Long]("UserID")
  val Identity: Rep[Long] = column[Long]("Identity")

  def * = (ID, UserID, Identity) <> (TwitterFollow.tupled, TwitterFollow.unapply)
}