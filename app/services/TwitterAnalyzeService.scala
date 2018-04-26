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
}