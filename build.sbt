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
      csvJava,
      csvGeneric,
      scalajHttp,
      json4sNative,
      jason4sExt,
      typesafeConfig,
      supportInternationalisation
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
        |com.gu.MyJson4sFormats*;
      """.stripMargin
  )
