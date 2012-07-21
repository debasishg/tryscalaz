import sbt._
import Keys._

object TryScalazProject extends Build
{
  lazy val root = Project("TryScalaz", file(".")) settings(coreSettings : _*)

  lazy val commonSettings: Seq[Setting[_]] = Seq(
    organization := "net.debasishg",
    version := "0.0.1",
    scalaVersion := "2.9.2",
    scalacOptions ++= Seq("-deprecation", "-unchecked")
  )

  lazy val coreSettings = commonSettings ++ Seq(
    name := "TryScalaz",

    libraryDependencies ++= Seq(
      "commons-pool"       % "commons-pool"     % "1.6",
      "junit"              % "junit"            % "4.8.1"            % "test",
      "org.scalatest"      % "scalatest_2.9.1"  % "1.6.1"            % "test",
      "org.scalacheck"     %% "scalacheck"      % "1.9"              % "test",
      "org.scalaz"         %% "scalaz-core"     % "6.0.4"),

    parallelExecution in Test := false,
    publishTo <<= version { (v: String) => 
      val nexus = "https://oss.sonatype.org/" 
      if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2") 
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { repo => false },
    pomExtra := (
      <url>https://github.com/debasishg/tryscalaz</url>
      <licenses>
        <license>
          <name>Apache 2.0 License</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:debasishg/scala-redis.git</url>
        <connection>scm:git:git@github.com:debasishg/scala-redis.git</connection>
      </scm>
      <developers>
        <developer>
          <id>debasishg</id>
          <name>Debasish Ghosh</name>
          <url>http://debasishg.blogspot.com</url>
        </developer>
      </developers>),
    unmanagedResources in Compile <+= baseDirectory map { _ / "LICENSE" }
  )

}

