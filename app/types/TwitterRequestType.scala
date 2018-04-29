package types

object TwitterRequestType {
  val GetFollowers: Short = 0;
  val GetFriends: Short = 1;
  val GetStatuses: Short = 2;
  val AnalyzeStatuses: Short = 3;
  
  val PostFollow: Short = 4;
  val PostUnFollow: Short = 5;
}