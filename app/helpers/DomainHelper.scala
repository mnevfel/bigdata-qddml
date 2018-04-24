package helpers

import play.Play

/**
 * Domain Helper Methods Here
 * */
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