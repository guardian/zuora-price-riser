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
      scalaTest,
      csvJoda,
      csvGeneric,
      scalajHttp,
      json4sNative,
      jason4sExt,
      typesafeConfig,
      supportInternationalisation,
      "com.lihaoyi" %% "pprint" % "0.5.3",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
    ),
    scalacOptions ++= Seq(
      "-Xfatal-warnings",  // New lines for each options
      "-deprecation",
      "-feature",
      "-unchecked"
    ),
    coverageExcludedPackages := """
        |com.gu.Main*;
        |com.gu.Config*;
      """.stripMargin,
    trapExit := false,
    fork in Test := true,
    envVars in Test := Map("ZUORA_STAGE" -> "DEV")
  )
