package tables

import slick.driver.MySQLDriver.api._

case class TwitterUser(
  override val ID:    Long           = 0,
  var Name:           String         = "",
  var ScreenName:     String         = "",
  var Identity:       String         = "",
  var Token:          String         = "",
  var FriendsCount:   Long           = 0,
  var FollowersCount: Long           = 0,
  var StatusesCount:  Long           = 0,
  var Secret:         Option[String] = None,
  var Location:       Option[String] = None,
  var Description:    Option[String] = None) extends Base

class TwitterUserTable(tag: Tag) extends BaseTable[TwitterUser](tag, "TwitterUser") {
  val Name: Rep[String] = column[String]("Name")
  val ScreenName: Rep[String] = column[String]("ScreenName")
  val Identity: Rep[String] = column[String]("Identity")
  val Token: Rep[String] = column[String]("Token")
  val FriendsCount: Rep[Long] = column[Long]("FriendsCount")
  val FollowersCount: Rep[Long] = column[Long]("FollowersCount")
  val StatusesCount: Rep[Long] = column[Long]("StatusesCount")
  val Secret: Rep[Option[String]] = column[Option[String]]("Secret")
  val Location: Rep[Option[String]] = column[Option[String]]("Location")
  val Description: Rep[Option[String]] = column[Option[String]]("Description")

  def * = (ID, Name, ScreenName, Identity, Token, FriendsCount,
    FollowersCount, StatusesCount, Secret, Location, Description) <> (TwitterUser.tupled, TwitterUser.unapply)
}