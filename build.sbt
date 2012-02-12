name := "tryscalaz"

organization := "net.debasishg"

version := "0.0.1"

crossScalaVersions := Seq("2.9.1", "2.9.0", "2.8.1", "2.8.0")

libraryDependencies <++= scalaVersion { scalaVersion =>
  val scalatestVersion: String => String = Map(("2.8.0" -> "1.3.1.RC2"), ("2.8.1" -> "1.5.1")) getOrElse (_, "1.6.1")
  // The dependencies with proper scope
  Seq(
    "junit"          % "junit"         % "4.8.1"  % "test",
    "org.scalatest" %% "scalatest"     % scalatestVersion(scalaVersion) % "test",
    "org.scalaz" %% "scalaz-core" % "6.0.4"
  )
}

scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-Xcheckinit")

publishTo := Some("Scala-Tools Nexus Repository for Releases" at "http://nexus.scala-tools.org/content/repositories/releases")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
