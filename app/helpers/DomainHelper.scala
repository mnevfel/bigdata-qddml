package helpers

import play.Play

object DomainHelper {
  var Host: String = {
    if (Play.application().isProd()) {
      Play.application().configuration()
        .getString("domain.prod.url")
    } else {
      Play.application().configuration()
        .getString("domain.dev.url")
    }
  }
}