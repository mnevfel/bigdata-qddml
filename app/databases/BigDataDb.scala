package databases

import slick.driver.MySQLDriver.api._

import patterns._
import tables._

object BigDataDb extends UnitOfWork("bigdata_qddml") {
  val TwitterUser = new Repository[TwitterUser, TwitterUserTable](TableQuery[TwitterUserTable])
  val TwitterFollow = new Repository[TwitterFollow, TwitterFollowTable](TableQuery[TwitterFollowTable])
  val TwitterKeyword = new Repository[TwitterKeyword, TwitterKeywordTable](TableQuery[TwitterKeywordTable])
  val TwitterFriend = new Repository[TwitterFriend, TwitterFriendTable](TableQuery[TwitterFriendTable])
  val TwitterStatus = new Repository[TwitterStatus, TwitterStatusTable](TableQuery[TwitterStatusTable])
  val TwitterRequest = new Repository[TwitterRequest, TwitterRequestTable](TableQuery[TwitterRequestTable])
}