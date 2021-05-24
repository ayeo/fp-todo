package pl.ayeo.todo

import cats.data.EitherT
import cats.effect.Bracket
import cats.implicits._
import doobie.implicits._
import doobie.{Transactor, Update0, _}
import pl.ayeo.todo.Algebra.{GUID, Todo, TodosService}

class DoobieRepository[F[_]](val xa: Transactor[F])(implicit b: Bracket[F, Throwable]) extends TodosService[F] {
  def insertSQL(todo: Todo): Update0 =
    sql"INSERT INTO todos(guid, done, title, description) VALUES (${todo.guid}, ${todo.title}, ${todo.description})".update

  def updateSQL(todo: Todo): Update0 =
    sql"UPDATE todos SET (done, title, description) = (${todo.done}, ${todo.title}, ${todo.description}) WHERE guid = ${todo.guid}".update

  def removeSQL(guid: GUID): Update0 =
    sql"DELETE FROM todos WHERE guid = $guid".update

  def selectSingleSQL(guid: GUID): Query0[Todo] =
    sql"SELECT guid, done, title, description FROM todos WHERE guid = $guid".query[Todo]

  def selectAllSQL(): Query0[Todo] =
    sql"SELECT guid, done, title, description FROM todos".query[Todo]

  override def add(todo: Algebra.Todo): F[Either[Serializable, Todo]] = {
    val eitherT = for {
      _       <- EitherT(insertSQL(todo).run.transact(xa).attemptSomeSqlState{ case _ => "Could not save"})
      result  <- EitherT(Either.right[Serializable, Todo](todo).pure[F])
    } yield result

    eitherT.value
  }

  override def update(todo: Algebra.Todo): F[Either[Serializable, Todo]] = {
    val eitherT = for {
      r <- updateSQL(todo).run.transact(xa).attemptSql
      c <-
        Either.right[Serializable, Todo](todo).pure[F]
    } yield c


    eitherT
  }

  override def remove(guid: GUID): F[Option[Algebra.Todo]] = for {
    todo  <- get(guid)
    _     <- removeSQL(guid).run.transact(xa)
  } yield todo

  override def getAll(): F[List[Todo]] = selectAllSQL().to[List].transact(xa)

  override def get(guid: GUID): F[Option[Algebra.Todo]] = selectSingleSQL(guid).option.transact(xa)
}