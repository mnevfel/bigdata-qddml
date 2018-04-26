package tables

import slick.driver.MySQLDriver.api._
import org.joda.time.DateTime

/**
 * Twitter TwitterFriend Relation Class For Orm
 */
case class TwitterFriend(
  override val ID:  Long    = 0,
  var UserID:       Long    = 0,
  var Identity:     Long    = 0,
  var FriendDate:   Long    = DateTime.now.getMillis(),
  var Permanent:    Boolean = false,
  var LastCallDate: Long    = DateTime.now.getMillis()) extends Base

class TwitterFriendTable(tag: Tag) extends BaseTable[TwitterFriend](tag, "TwitterFriend") {
  val UserID: Rep[Long] = column[Long]("UserID")
  val Identity: Rep[Long] = column[Long]("Identity")
  val FriendDate: Rep[Long] = column[Long]("FriendDate")
  val Permanent: Rep[Boolean] = column[Boolean]("Permanent")
  val LastCallDate: Rep[Long] = column[Long]("LastCallDate")

  def * = (ID, UserID, Identity, FriendDate, Permanent, LastCallDate) <> (TwitterFriend.tupled, TwitterFriend.unapply)
}