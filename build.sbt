import Dependencies._

name := "body-parser"
scalaVersion := scalaVersionNumber
organization := artifactGroupName
version      := artifactVersionNumber

resolvers += localRepo.get

libraryDependencies ++= Seq(
  protobufJava,
  protobufJavaUtil,
  play,
  playJson,
  queryRuntime % Test,
  slf4jApi % Test,
  slf4jSimple % Test,
  scalaTest % Test
)

publishTo := sonatypePublishTo.value
