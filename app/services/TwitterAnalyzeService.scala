package services

import tables._
import types._
import helpers._
import databases.BigDataDb._
import slick.driver.MySQLDriver.api._
import play.api.libs.oauth._
import play.api.libs.json._

import databases.BigDataDb

import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext
import org.joda.time.DateTime

class TwitterAnalyzeService {
  def Keywords(userId: Long, text: String, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    val split = text.split(" ")
    split.foreach(partial => {
      if (!partial.isEmpty()) {
        val keyword = TextHelper.Normalize(partial)
        val rank = TextHelper.GetRank(partial)
        if (TextHelper.IsValid(keyword)) {
          val keywordRel = BigDataDb.TwitterKeyword.Filter(x => x.UserID === userId
            && x.Keyword === keyword).result.headOption.run

          if (keywordRel.isDefined) {
            var upRel = keywordRel.get
            upRel.Rank = upRel.Rank + rank
            BigDataDb.TwitterKeyword.Update(upRel).run
          } else {
            BigDataDb.TwitterKeyword.Insert(new TwitterKeyword {
              UserID = userId;
              Keyword = keyword;
              Rank = rank;
            }).run
          }
        }
      }
    })
  }

  def AnalyzeStatuses(userId: Long, ws: WSClient, ec: ExecutionContext) {
    implicit val exec: ExecutionContext = ec
    BigDataDb.TwitterUser.Filter(x => x.ID === userId)
      .result.headOption.runAsync.map(user => {
        if (user.isDefined) {
          BigDataDb.TwitterKeyword.Filter(x => x.UserID === userId)
            .sortBy(x => x.Rank).take(5).result.runAsync.map(keywords => {
              keywords.foreach(keyword => {
                ws.url(TwitterHelper.TwApi + "search/tweets.json?q=" + keyword.Keyword + "&count=100")
                  .sign(OAuthCalculator(TwitterHelper.TwitterKey, RequestToken(
                    user.get.Token,
                    user.get.Secret)))
                  .get
                  .map(result => {
                    val obj = result.json.as[JsObject]
                    val statuses = (obj \ "statuses").as[JsArray]
                    statuses.value.foreach(status => {
                      val identity = (status \ "id").as[Long]
                      val text = (status \ "text").as[String]

                      TwitterServiceProvider.User.InsertStatus(userId, identity, text, ec)
                    })
                  })
              })
            })
          TwitterServiceProvider.User.UpdateRequestType(userId, TwitterRequestType.GetStatuses)
        }
      })
  }
}