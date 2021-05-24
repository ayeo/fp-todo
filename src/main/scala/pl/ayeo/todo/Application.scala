package pl.ayeo.todo

import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Decoder, Encoder, Json}

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
