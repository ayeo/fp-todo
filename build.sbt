name := "doobie-test"

version := "0.1"

scalaVersion := "2.13.5"

lazy val doobieVersion = "0.12.1"
val Http4sVersion = "0.21.21"
val TsecVersion = "0.2.1"

resolvers += "jitpack".at("https://jitpack.io")
//cresolvers += "Tabmo Myget Public".at("https://www.myget.org/F/tabmo-public/maven/")

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime,
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-specs2" % doobieVersion,
  "dev.profunktor" %% "console4cats" % "0.8.1",
  "mysql" % "mysql-connector-java" % "5.1.44",
  "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  "io.circe" %% "circe-generic" % "0.13.0",
  "io.circe" %% "circe-literal" % "0.13.0",
  "io.circe" %% "circe-parser" % "0.13.0",
  "io.circe" %% "circe-json-schema" % "0.1.0",
  "io.chrisdavenport" %% "fuuid" % "0.5.0",
  "io.github.jmcardon" %% "tsec-common" % TsecVersion,
  "io.github.jmcardon" %% "tsec-password" % TsecVersion,
  "io.github.jmcardon" %% "tsec-mac" % TsecVersion,
  "io.github.jmcardon" %% "tsec-signatures" % TsecVersion,
  "io.github.jmcardon" %% "tsec-jwt-mac" % TsecVersion,
  "io.github.jmcardon" %% "tsec-jwt-sig" % TsecVersion,
  "io.github.jmcardon" %% "tsec-http4s" % TsecVersion,

)
