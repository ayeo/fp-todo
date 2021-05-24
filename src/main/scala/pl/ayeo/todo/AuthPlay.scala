package pl.ayeo.todo


import cats.data.OptionT
import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.{Eq, Id}
import org.http4s.HttpService
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import pl.ayeo.todo.ExampleAuthHelpers.{Role, User, dummyBackingStore}
import tsec.authentication._
import tsec.authorization.{AuthGroup, SimpleAuthEnum}
import tsec.common.SecureRandomId
import tsec.jws.mac.JWTMac
import tsec.mac.jca.{HMACSHA256, MacSigningKey}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ExampleAuthHelpers {
  def dummyBackingStore[F[_], I, V](getId: V => I)(implicit F: Sync[F]) = new BackingStore[F, I, V] {
    private val storageMap = mutable.HashMap.empty[I, V]

    def put(elem: V): F[V] = {
      val map = storageMap.put(getId(elem), elem)
      if (map.isEmpty)
        F.pure(elem)
      else
        F.raiseError(new IllegalArgumentException)
    }

    def get(id: I): OptionT[F, V] = {
      println(s"get $id")
      OptionT.fromOption[F](storageMap.get(id))
    }

    def update(v: V): F[V] = {
      storageMap.update(getId(v), v)
      F.pure(v)
    }

    def delete(id: I): F[Unit] =
      storageMap.remove(id) match {
        case Some(_) => F.unit
        case None => F.raiseError(new IllegalArgumentException)
      }
  }

  /*
  In our example, we will demonstrate how to use SimpleAuthEnum, as well as
  Role based authorization
   */
  sealed case class Role(roleRepr: String)

  object Role extends SimpleAuthEnum[Role, String] {

    val Administrator: Role = Role("Administrator")
    val Customer: Role = Role("User")
    val Seller: Role = Role("Seller")

    implicit val E: Eq[Role] = Eq.fromUniversalEquals[Role]

    def getRepr(t: Role): String = t.roleRepr

    protected val values: AuthGroup[Role] = AuthGroup(Administrator, Customer, Seller)
  }

  case class User(id: Int, age: Int, name: String, role: Role = Role.Customer)

}

object jwtStatefulExample extends IOApp {

  val jwtStore =
    dummyBackingStore[IO, SecureRandomId, AugmentedJWT[HMACSHA256, Int]](s => SecureRandomId.coerce(s.id))

  //We create a way to store our users. You can attach this to say, your doobie accessor
  val userStore: BackingStore[IO, Int, User] = dummyBackingStore[IO, Int, User](_.id)

  //Our signing key. Instantiate in a safe way using .generateKey[F]
  //val signingKey: MacSigningKey[HMACSHA256] = HMACSHA256.generateKey[Id]

  val signingKey: MacSigningKey[HMACSHA256] = HMACSHA256.buildKey[Id]("glon".getBytes())

  val jwtStatefulAuth =
    JWTAuthenticator.backed.inBearerToken(
      expiryDuration = 10.minutes, //Absolute expiration time
      maxIdle        = None,
      tokenStore     = jwtStore,
      identityStore  = userStore,
      signingKey     = signingKey
    )

  val Auth =
    SecuredRequestHandler(jwtStatefulAuth)

  /*
  Now from here, if want want to create services, we simply use the following
  (Note: Since the type of the service is HttpService[IO], we can mount it like any other endpoint!):
   */
  val service: HttpService[IO] = Auth.liftService(TSecAuthService {
    //Where user is the case class User above
    case request@GET -> Root / "api" asAuthed user =>
      /*
      Note: The request is of type: SecuredRequest, which carries:
      1. The request
      2. The Authenticator (i.e token)
      3. The identity (i.e in this case, User)
       */
      val r: SecuredRequest[IO, User, AugmentedJWT[HMACSHA256, Int]] = request
      Ok()
  })

  val i = for {
    augmentedJWT <- jwtStatefulAuth.create(789)
  } yield JWTMac.toEncodedString[IO, HMACSHA256](augmentedJWT.jwt)

  userStore.put(User(789, 22, "Bob", Role.Customer))
  println(i.unsafeRunSync())

  val httpApp = Router(
    "/users" -> service
  ).orNotFound

  override def run(args: List[String]): IO[ExitCode] = {
    val server = BlazeServerBuilder[IO](global).bindHttp(8665).withHttpApp(httpApp).resource
    server.use(_ => IO.never).as(ExitCode.Success)
  }
}