package tables

import slick.driver.MySQLDriver.api._
import org.joda.time.DateTime

/**
 * Twitter TwitterFollow Relation Class For Orm
 */
case class TwitterFollow(
  override val ID: Long  = 0,
  var UserID:      Long  = 0,
  var Identity:    Long  = 0,
  var FollowType:  Short = 0,
  var FollowDate:  Long  = DateTime.now.getMillis()) extends Base

class TwitterFollowTable(tag: Tag) extends BaseTable[TwitterFollow](tag, "TwitterFollow") {
  val UserID: Rep[Long] = column[Long]("UserID")
  val Identity: Rep[Long] = column[Long]("Identity")
  val FollowType: Rep[Short] = column[Short]("FollowType")
  val FollowDate: Rep[Long] = column[Long]("FollowDate")

  def * = (ID, UserID, Identity, FollowType, FollowDate) <> (TwitterFollow.tupled, TwitterFollow.unapply)
}