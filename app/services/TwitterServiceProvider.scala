package services

object TwitterServiceProvider {
  val User = new TwitterUserService()
  val Analyze = new TwitterAnalyzeService()
}