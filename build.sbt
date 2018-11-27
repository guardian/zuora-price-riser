import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.gu",
      scalaVersion := "2.12.5",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "zuora-price-riser",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "com.nrinaudo" %% "kantan.csv-java8" % "0.5.0",
      "com.nrinaudo" %% "kantan.csv-generic" % "0.5.0",
      "org.scalaj" %% "scalaj-http" % "2.4.1",
      "org.json4s" %% "json4s-native" % "3.6.2",
      "org.json4s" %% "json4s-ext" % "3.6.2"
    )
  )
