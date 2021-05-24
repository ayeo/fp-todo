package pl.ayeo.todo

import pl.ayeo.todo.Algebra.HigherService
import cats.effect.{Blocker, ExitCode, IO, IOApp}
import org.http4s.HttpRoutes
import org.http4s.server.blaze.BlazeServerBuilder
import Algebra.{Details, Headline, Todo}
import doobie.Transactor
import doobie.util.ExecutionContexts
import io.chrisdavenport.fuuid.FUUID
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.circe._
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global

object Server extends IOApp {
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)
  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // driver classname
    "jdbc:postgresql:ayeo?useUnicode=true&characterEncoding=utf8", // connect URL (driver-specific)
    "postgres", // user
    "", // password
    Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
  )

  implicit val service = new DoobieRepository[IO](xa)
  val hs = new HigherService[IO]

  case class TodoJson(headline: Headline, details: Option[Details])

  val jsonApp = HttpRoutes.of[IO] {
    case req@POST -> Root => for {
      todo <- req.as[TodoJson]
      guid <- FUUID.randomFUUID[IO]
      resp <- service.add(Todo(guid.toString().slice(0, 5), false, todo.headline.headline, todo.details))
      x <- resp match {
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
    case PUT -> Root / guid / "complete" => {
      hs.completeTask(guid).flatMap {
        case Left(error: String) => BadRequest(error)
        case Right(_) => Ok("Task marked as complete")
      }
    }
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
    val server = BlazeServerBuilder[IO](global).bindHttp(8667).withHttpApp(jsonApp).resource
    server.use(_ => IO.never).as(ExitCode.Success)
  }
}
