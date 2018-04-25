package tables

import slick.driver.MySQLDriver.api._

/**
 * Twitter User Class For Orm
 */
case class TwitterUser(
  override val ID: Long           = 0,
  var Identity:    Long           = 0,
  var Name:        String         = "",
  var ScreenName:  String         = "",
  var Token:       String         = "",
  var Secret:      String         = "",
  var Location:    Option[String] = None,
  var Description: Option[String] = None) extends Base

class TwitterUserTable(tag: Tag) extends BaseTable[TwitterUser](tag, "TwitterUser") {
  val Name: Rep[String] = column[String]("Name")
  val Identity: Rep[Long] = column[Long]("Identity")
  val ScreenName: Rep[String] = column[String]("ScreenName")
  val Token: Rep[String] = column[String]("Token")
  val Secret: Rep[String] = column[String]("Secret")
  val Location: Rep[Option[String]] = column[Option[String]]("Location")
  val Description: Rep[Option[String]] = column[Option[String]]("Description")

  def * = (ID, Identity, Name, ScreenName, Token,
    Secret, Location, Description) <> (TwitterUser.tupled, TwitterUser.unapply)
}