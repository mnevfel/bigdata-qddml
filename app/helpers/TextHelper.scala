package helpers

import play.Play

/**
 * Text Helper Methods Here
 */
object TextHelper {
  def Normalize(text: String): String = {
    var nwText: String = ""
    text.toLowerCase().trim()
      .replace("ş", "s")
      .replace("ü", "u")
      .replace("ç", "c")
      .replace("ğ", "g")
      .replace("ö", "o").toList.foreach(char => {
        val ascii = char.toInt
        if (ascii >= 97 && ascii <= 122) {
          nwText += char
        }
      })
    return nwText
  }

  def IsValid(text: String): Boolean = {
    return (text.length() >= 3
      && !text.contains("httpstco"))
  }

  def HasSpecial(text: String): Boolean = {
    return text.toLowerCase().exists(x => x.toInt < 97 || x.toInt > 122)
  }

  def GetRank(text: String): Long = {
    var rank: Long = text.length() * 1

    if (!text.isEmpty()) {
      //If after head index between before last char index contains special char
      if (this.HasSpecial(text)) { rank += 1 }
      if (text.head == '@') { rank += 1 }
      else if (text.head == '#') { rank += 2 }
    }

    return rank
  }
}