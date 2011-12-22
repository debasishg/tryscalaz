import sbt._
import sbt._
import Keys._

object TryScalazBuild extends Build {
  lazy val buildSettings = Seq(
    organization := "net.debasishg",
    version := "0.0.1",
    scalaVersion := "2.9.1"
  )

  lazy val akka = Project(
    id = "tryscalaz",
    base = file("."),

    settings = Defaults.defaultSettings ++ Seq(
      resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
      resolvers += "Typesafe snapshots" at "http://repo.typesafe.com/typesafe/akka-snapshots",

      libraryDependencies ++= Seq(
        "com.typesafe.akka" % "akka-actor"          % "2.0-SNAPSHOT",
        "org.scalaz"        % "scalaz-core_2.9.0-1" % "6.0.1",
        "junit"             % "junit"               % "4.5"           % "test",
        "org.scalatest"     % "scalatest_2.9.0"     % "1.6.1"         % "test",
        "com.typesafe.akka" % "akka-testkit"        % "2.0-SNAPSHOT"  % "test")
    )
  )
}
