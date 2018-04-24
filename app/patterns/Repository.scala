package patterns

import tables._
import slick.driver.MySQLDriver.api._

import slick.lifted.CanBeQueryCondition
import scala.concurrent.ExecutionContext

/**
 * Database Table Query Pattern
 * */
class Repository[E <: Base, T <: BaseTable[E]](query: TableQuery[T]) {
  private val _query: TableQuery[T] = query

  def table = {
    _query
  }

  def Filter[Q <: Rep[_]](expr: T => Q)(implicit wt: CanBeQueryCondition[Q]) = {
    _query.filter(expr)
  }

  def Count = {
    _query.length.result
  }

  def Count[Q <: Rep[_]](expr: T => Q)(implicit wt: CanBeQueryCondition[Q]) = {
    _query.filter(expr).length.result
  }

  def Any[Q <: Rep[_]](expr: T => Q)(implicit wt: CanBeQueryCondition[Q], ec: ExecutionContext) = {
    _query.filter(expr).result.headOption.map(x => x.isDefined)
  }

  def Insert(data: E) = {
    (_query returning _query.map(_.ID)) += data
  }

  def Update(data: E) = {
    _query.filter(_.ID === data.ID).update(data)
  }

  def Delete(id: Long) = {
    _query.filter(_.ID === id).delete
  }

  def Delete[Q <: Rep[_]](expr: T => Q)(implicit wt: CanBeQueryCondition[Q]) = {
    _query.filter(expr).delete
  }

  def DeleteAll = {
    _query.delete
  }
}