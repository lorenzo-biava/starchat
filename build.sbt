import NativePackagerHelper._
import com.typesafe.sbt.packager.docker._

name := "StarChat"

organization := "com.getjenny"

scalaVersion := "2.12.2"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  Resolver.bintrayRepo("hseeberger", "maven"))

libraryDependencies ++= {
  val AkkaVersion       = "2.5.4"
  val AkkaHttpVersion   = "10.0.9"
  val ESClientVersion   = "5.5.0"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % AkkaVersion,
    "com.typesafe.akka" %% "akka-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-contrib" % AkkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion,
    "ch.qos.logback"    %  "logback-classic" % "1.1.3",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.elasticsearch" % "elasticsearch" % ESClientVersion,
    "org.elasticsearch.client" % "transport" % ESClientVersion,
    "org.elasticsearch.client" % "rest" % ESClientVersion,
    "org.scalanlp" %% "breeze" % "0.13",
    "org.scalanlp" %% "breeze-natives" % "0.13",
    "org.apache.logging.log4j" % "log4j-api" % "2.8.2",
    "org.apache.logging.log4j" % "log4j-core" % "2.8.2",
    "org.apache.tika" % "tika-core" % "1.14",
    "org.apache.tika" % "tika-parsers" % "1.14",
    "org.apache.tika" % "tika-app" % "1.14",
    "com.github.scopt" %% "scopt" % "3.6.0",
    "com.roundeights" %% "hasher" % "1.2.0",
    "org.parboiled" %% "parboiled" % "2.1.4"
   )
}

scalacOptions += "-deprecation"
scalacOptions += "-feature"

enablePlugins(GitVersioning)
enablePlugins(GitBranchPrompt)
enablePlugins(JavaServerAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(DockerPlugin)
enablePlugins(DockerComposePlugin)

git.useGitDescribe := true

//http://www.scala-sbt.org/sbt-native-packager/formats/docker.html
dockerCommands := Seq(
  Cmd("FROM", "java:8"),
  Cmd("RUN", "apt", "update"),
  Cmd("RUN", "apt", "install", "-y", "netcat"),
  Cmd("LABEL", "maintainer=\"Angelo Leto <angelo@getjenny.com>\""),
  Cmd("LABEL", "description=\"Docker container for Starchat\""),
  Cmd("WORKDIR", "/"),
  Cmd("ADD", "/opt/docker", "/starchat"),
  Cmd("VOLUME", "/starchat/config"),
  Cmd("VOLUME", "/starchat/log")
)

packageName in Docker := packageName.value
version in Docker := version.value
dockerRepository := Some("elegansio")

//dockerImageCreationTask := (publishLocal in Docker).value
composeNoBuild := true
composeFile := "docker-starchat/docker-compose.test.yml" 

// Assembly settings
mainClass in Compile := Some("com.getjenny.starchat.Main")

fork in Test := true
javaOptions in Test ++= Seq("-Dconfig.file=./src/test/resources/application.conf")

// do not buffer test output
logBuffered in Test := false

mappings in Universal ++= {
  // copy configuration files to config directory
  directory("scripts") ++
  contentOf("src/main/resources").toMap.mapValues("config/" + _).toSeq
}

scriptClasspath := Seq("../config/") ++ scriptClasspath.value

licenses := Seq(("GPLv2", url("https://www.gnu.org/licenses/old-licenses/gpl-2.0.md")))

