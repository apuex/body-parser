import sbt._

object Dependencies {
  lazy val scalaVersionNumber    = "2.12.8"
  lazy val artifactVersionNumber = "1.0.0"
  lazy val artifactGroupName     = "com.github.apuex.play"
  lazy val playVersion           = "2.7.0"

  lazy val protobufJava     = "com.google.protobuf"       %   "protobuf-java"                      % "3.6.1"
  lazy val protobufJavaUtil = "com.google.protobuf"       %   "protobuf-java-util"                 % "3.6.1"
  lazy val playJson         = "com.typesafe.play"         %%  "play-json"                          % playVersion
  lazy val play             = "com.typesafe.play"         %%  "play"                               % playVersion

  lazy val slf4jApi         = "org.slf4j"                 %   "slf4j-api"                          % "1.7.25"
  lazy val slf4jSimple      = "org.slf4j"                 %   "slf4j-simple"                       % "1.7.25"
  lazy val scalaTest        = "org.scalatest"             %%  "scalatest"                          % "3.0.4"
  lazy val queryRuntime     = "com.github.apuex.springbootsolution" %%  "runtime"                  % "1.0.8"

  lazy val localRepo = Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
}
