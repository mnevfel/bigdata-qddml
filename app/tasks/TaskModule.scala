package tasks

import play.api.inject.{ SimpleModule, _ }

class TaskModule extends SimpleModule(
  bind[TwitterUserTask].toSelf.eagerly(),
  bind[TwitterAnalyzeTask].toSelf.eagerly(),
  bind[TwitterTargetTask].toSelf.eagerly()
  )