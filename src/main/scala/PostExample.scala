import Algebra.Todo
import cats.effect._
import doobie.Transactor
import doobie.util.ExecutionContexts
import io.chrisdavenport.fuuid.FUUID
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io._
import org.http4s.server.blaze._
import org.http4s.implicits._
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.syntax._

object PostExample extends IOApp {
  import io.circe._


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

  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)
  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",     // driver classname
    "jdbc:postgresql:ayeo?useUnicode=true&characterEncoding=utf8",     // connect URL (driver-specific)
    "postgres",                  // user
    "",                          // password
    Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
  )

  val service = new DoobieRepository[IO](xa)

  case class TodoJson(headline: Headline, details: Option[Details])

  implicit val todoEncoder =  deriveEncoder[Todo]

  import org.http4s.circe._

  val jsonApp = HttpRoutes.of[IO] {
    case req @ POST -> Root  => for {
        todo <- req.as[TodoJson]
        guid <- FUUID.randomFUUID[IO]
        resp <- service.add(Todo(guid.toString().slice(0, 5), todo.headline.headline, todo.details))
        x    <- resp match {
          case Right(todo) => Ok(todo.guid)
          case Left(error) => BadRequest(error.toString)
        }
      } yield x
    case GET -> Root => for {
        todos <- service.getAll()
        response <- Ok(todos.asJson)
      } yield response
    case GET -> Root / guid => for {
      todo <- service.get(guid)
      response <- Ok(todo.asJson)
    } yield response
    case DELETE -> Root / guid => for {
        r <- service.remove(guid)
        res <-
          r match {
          case Some(_) => Ok("Removed")
          case None => BadRequest("Could not remove")
        }
      } yield res
  }.orNotFound


  override def run(args: List[String]): IO[ExitCode] = {
    val server = BlazeServerBuilder[IO](global).bindHttp(8666).withHttpApp(jsonApp).resource
    server.use(_ => IO.never).as(ExitCode.Success)
  }
}
