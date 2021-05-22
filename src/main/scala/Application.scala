import Algebra.{Todo, TodosService}
import PostExample.{Details, Headline}
import cats.effect.Sync
import cats._
import cats.data.EitherT
import cats.effect._
import cats.implicits._

import java.util.UUID
import cats.effect.Console.implicits._
import io.circe.Encoder
import io.circe.generic.JsonCodec
import io.circe.generic.semiauto.deriveEncoder


object Algebra {
  type GUID = String
  type Storage = Map[GUID, Todo]

  case class Todo(guid: GUID, title: String, description: Option[Details])
  object Todo {
    implicit val fooEncoder: Encoder[Todo] = deriveEncoder
  }

  trait TodosService[F[_]] {
    def add(todo: Todo): F[Either[Serializable, Todo]]
    def remove(guid: GUID): F[Option[Todo]]
    def getAll(): F[List[Todo]]
    def get(guid: GUID): F[Option[Todo]]
  }
}

object Interpreter {
  import Algebra._
  import cats.effect.concurrent.Ref

//  class TestTodosService[F[_] : Monad](val cache: Ref[F, Map[GUID, Todo]]) extends TodosService[F] {
//    override def add(todo: Todo): F[Option[Todo]] =
//      cache.update(x => {x + (todo.guid -> todo)}) *>
//        Option(todo).pure[F]
//
//    override def remove(guid: GUID): F[Option[Todo]] = for {
//      todo  <- get(guid)
//      _     <- cache.update(x => x - guid)
//    } yield todo
//
//    override def getAll(): F[List[Todo]] = cache.get.map(_.values.toList)
//
//    override def get(guid: GUID): F[Option[Todo]] = cache.get.map(_.get(guid))
//  }

}
