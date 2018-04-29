package services

object TwitterServiceProvider {
  val User = new TwitterUserService()
  val Api = new TwitterApiService()
}