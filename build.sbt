import NativePackagerHelper._

name := "StarChat"

version := "0.1"

organization := "com.getjenny"

scalaVersion := "2.12.1"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  Resolver.bintrayRepo("hseeberger", "maven"))

libraryDependencies ++= {
  val AkkaVersion       = "2.4.14"
  val AkkaHttpVersion   = "10.0.0"
  val ESClientVersion   = "2.4.0"
  Seq(
    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
	"com.typesafe.akka" %% "akka-http-core" % AkkaHttpVersion,
	"com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
	"com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
	"com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion,
    "ch.qos.logback"    %  "logback-classic" % "1.1.2",
    "org.elasticsearch" % "elasticsearch" % ESClientVersion,
    "log4j" % "log4j" % "1.2.17" // dependency of es libs
   )
}

enablePlugins(JavaServerAppPackaging)

// Assembly settings
mainClass in Compile := Some("com.getjenny.starchat.Main")

mappings in Universal ++= {
  // copy configuration files to config directory
  directory("scripts") ++
  contentOf("src/main/resources").toMap.mapValues("config/" + _).toSeq
}

scriptClasspath := Seq("../config/") ++ scriptClasspath.value

licenses := Seq(("GPLv3", url("https://opensource.org/licenses/MIT")))
