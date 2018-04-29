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
          val lines = Seq(45, 95)
          if (nwText.length() < 40) {
            val ascii = char.toInt
            if (ascii >= 97 && ascii <= 122) {
              nwText += char
            } else if (index != 0 && index != text.length - 1
              && (lines.contains(ascii) /*-,_*/ )
              && !nwText.contains("-")
              && !nwText.contains("_")) {
              nwText += char
            }
          }
        }
      }
    return nwText
  }

  def IsValid(text: String): Boolean = {
    return (text.length() > 3
      && !text.contains("http"))
  }

  def GetRank(text: String): Long = {
    var rank: Long = 1

    if (!text.isEmpty()) {
      if (text.head == '@') { rank += 1 }
      else if (text.head == '#') { rank += 2 }
    }

    return rank
  }
}