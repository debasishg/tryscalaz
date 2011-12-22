import sbt._
 
class Plugins extends Plugin {
  val akkaRepo = "Akka Repository" at "http://akka.io/repository"

  val akkaPlugin = "se.scalablesolutions.akka" % "akka-sbt-plugin" % "1.0-RC5"
}

// vim: set ts=4 sw=4 et:
