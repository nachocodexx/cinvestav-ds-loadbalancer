import com.typesafe.sbt.packager.docker
import com.typesafe.sbt.packager.docker._
val Fs2Version = "3.0.0-M9"
lazy val Scalactic = "org.scalactic" %% "scalactic" % "3.2.5"
lazy val ScalaTest = "org.scalatest" %% "scalatest" % "3.2.5" % Test
lazy val PureConfig = "com.github.pureconfig" %% "pureconfig" % "0.14.1"
lazy val Fs2 = Seq(
  "co.fs2" %% "fs2-core",
  "co.fs2" %% "fs2-io"
).map(_%Fs2Version)
val circeVersion = "0.12.3"

lazy val Circe =  Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

lazy val Ip4s = "com.comcast" %% "ip4s-core" % "1.4.1"
lazy val app = (project in file(".")).settings(
  name := "cinvestav-ds-loadbalancer",
  version := "0.1",
  scalaVersion := "2.13.5",
  libraryDependencies++=Seq(Ip4s,ScalaTest,Scalactic,PureConfig)++Fs2++Circe,
)
  .enablePlugins(JavaAppPackaging)
