package tables

import scala.reflect._
import slick.driver.MySQLDriver.api._
import slick.driver.JdbcProfile

abstract class Base {
  val ID: Long = 0
}

abstract class BaseTable[E <: Base: ClassTag](tag: Tag, table: String)
  extends Table[E](tag, table) {
  def ID: Rep[Long] = column[Long]("ID", O.PrimaryKey, O.AutoInc)
}