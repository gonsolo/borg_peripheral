ThisBuild / scalaVersion     := "2.13.18"
ThisBuild / version          := "0.1"
ThisBuild / organization     := "org.gonsolo"

val chiselVersion = "7.4.0"

lazy val root = (project in file("."))
  .settings(
    name := "TinyGPU",
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel" % chiselVersion,
      "org.scalatest" %% "scalatest" % "3.2.19" % "test"
    ),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:reflectiveCalls",
    ),
    addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full),
)

