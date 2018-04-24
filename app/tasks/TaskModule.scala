package tasks

import play.api.inject.{ SimpleModule, _ }

class TaskModule extends SimpleModule(bind[TwitterTask].toSelf.eagerly())