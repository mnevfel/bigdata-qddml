package tables

import slick.driver.MySQLDriver.api._
import org.joda.time.DateTime

/**
 * Twitter UserStatus Relation Class For Orm
 */
case class TwitterStatus(
  override val ID:   Long   = 0,
  var UserID:        Long   = 0,
  var Identity:      Long   = 0,
  var Text:          String = "",
  var FormattedText: String = "") extends Base

class TwitterStatusTable(tag: Tag) extends BaseTable[TwitterStatus](tag, "TwitterStatus") {
  val UserID: Rep[Long] = column[Long]("FollowedID")
  val Identity: Rep[Long] = column[Long]("FollowerID")
  val Text: Rep[String] = column[String]("FollowDate")
  val FormattedText: Rep[String] = column[String]("Permanent")

  def * = (ID, UserID, Identity, Text, FormattedText) <> (TwitterStatus.tupled, TwitterStatus.unapply)
}