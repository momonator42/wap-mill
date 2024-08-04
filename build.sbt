name := """WAP-mill"""
organization := "-"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "3.3.1"

libraryDependencies ++= Seq(
      guice,
      "org.scalactic" %% "scalactic" % "3.2.16",
      "org.scalatest" %% "scalatest" % "3.2.15" % "test",
      "com.typesafe.play" %% "play-json" % "2.10.0-RC7",
      "com.google.inject" % "guice" % "5.1.0",
      ("net.codingwell" %% "scala-guice" % "6.0.0").cross(CrossVersion.for3Use2_13),
      "ch.qos.logback" % "logback-classic" % "1.4.11",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "-.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "-.binders._"
