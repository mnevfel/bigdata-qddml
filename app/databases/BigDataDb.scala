package databases

import slick.driver.MySQLDriver.api._

import patterns._
import tables._

object BigDataDb extends UnitOfWork("bigdata_qddml") {
  val TwitterUser = new Repository[TwitterUser, TwitterUserTable](TableQuery[TwitterUserTable])
}