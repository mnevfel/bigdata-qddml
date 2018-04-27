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
      .replace("ı", "i")
      .replace("ö", "o").toList.zipWithIndex.foreach {
        case (char, index) => {
          val ascii = char.toInt
          if (ascii >= 97 && ascii <= 122) {
            nwText += char
          } else if (index != 0 && index != text.length - 1
            && (ascii == 45 /*-*/ || ascii == 95 /*_*/ )) {
            nwText += char
          }
        }
      }
    return nwText
  }

  def IsValid(text: String): Boolean = {
    return (text.length() >= 3
      && !text.contains("http"))
  }

  def HasSpecial(text: String): Boolean = {
    return text.toLowerCase().exists(x => x.toInt < 97 || x.toInt > 122)
  }

  def GetRank(text: String): Long = {
    var rank: Long = text.length() * 1

    if (!text.isEmpty()) {
      //If after head index between before last char index contains special char
      if (this.HasSpecial(text)) { rank += 1 }
      if (text.head == '#') { rank += 2 }
      else if (text.head == '@') { rank += 3 }
    }

    return rank
  }
}