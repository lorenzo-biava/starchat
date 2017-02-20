import NativePackagerHelper._

name := "StarChat"

version := "master"

organization := "com.getjenny"

scalaVersion := "2.12.1"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  Resolver.bintrayRepo("hseeberger", "maven"))

libraryDependencies ++= {
  val AkkaVersion       = "2.4.17"
  val AkkaHttpVersion   = "10.0.3"
  val ESClientVersion   = "5.2.0"
  Seq(
    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion,
    "ch.qos.logback"    %  "logback-classic" % "1.1.3",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.elasticsearch" % "elasticsearch" % ESClientVersion,
    "org.elasticsearch.client" % "transport" % ESClientVersion,
    "org.elasticsearch.client" % "rest" % ESClientVersion,
    "org.apache.logging.log4j" % "log4j-api" % "2.7",
    "org.apache.logging.log4j" % "log4j-core" % "2.7",
    "org.parboiled" %% "parboiled" % "2.1.4"
   )
}

scalacOptions += "-deprecation"

enablePlugins(JavaServerAppPackaging)

// Assembly settings
mainClass in Compile := Some("com.getjenny.starchat.Main")

// do not buffer test output
logBuffered in Test := false

mappings in Universal ++= {
  // copy configuration files to config directory
  directory("scripts") ++
  contentOf("src/main/resources").toMap.mapValues("config/" + _).toSeq
}

scriptClasspath := Seq("../config/") ++ scriptClasspath.value

licenses := Seq(("GPLv2", url("https://www.gnu.org/licenses/old-licenses/gpl-2.0.md")))

