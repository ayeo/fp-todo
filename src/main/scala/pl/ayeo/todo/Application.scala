package pl.ayeo.todo

import cats.{Applicative, Monad}
import cats.data.EitherT
import cats.effect.IO
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Decoder, Encoder, Json}
import pl.ayeo.todo.Server.service
import cats.implicits._

object Algebra {
  type GUID = String
  type Storage = Map[GUID, Todo]

  type Details = String
  case class Headline private(headline: String)
  object Headline {
    def apply(str: String): Either[String, Headline] =
      Either.cond(str.nonEmpty, new Headline(str), "Headline must not be empty")

    implicit val decodeTodo: Decoder[Headline] = Decoder.decodeString.emap(Headline(_))
    implicit val encodeFoo: Encoder[Headline] = new Encoder[Headline] {
      final def apply(a: Headline): Json = Json.fromString(a.headline)
    }
  }

  case class Todo(guid: GUID, done: Boolean, title: String, description: Option[Details]) {
    def completed(): Either[String, Todo] =
      if (done) Left("Task is already done")
      else Right(Todo(guid, true, title, description))
  }
  object Todo {
    implicit val fooEncoder: Encoder[Todo] = deriveEncoder
  }

  trait TodosService[F[_]] {
    def add(todo: Todo): F[Either[Serializable, Todo]]
    def update(todo: Todo): F[Either[String, Todo]]
    def remove(guid: GUID): F[Option[Todo]]
    def getAll(): F[List[Todo]]
    def get(guid: GUID): F[Option[Todo]]
  }

  class HigherService[F[_]: Applicative: Monad](implicit ts: TodosService[F])  {
    def get(guid: String ): F[Either[String, Todo]] = ts.get(guid).map(o => o match {
        case None => Left("Task not found")
        case Some(todo) => Right(todo)
      })

    def completeTask(guid: String): F[Either[String, Todo]] = {
      val result = for {
        todo      <- EitherT(get(guid))
        completed <- EitherT(todo.completed().pure[F])
        saved     <- EitherT(ts.update(completed))
      } yield saved

      result.value
    }
  }
}

object Interpreter {

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
