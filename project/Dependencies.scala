import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"
  lazy val csvJava = "com.nrinaudo" %% "kantan.csv-java8" % "0.5.0"
  lazy val csvGeneric = "com.nrinaudo" %% "kantan.csv-generic" % "0.5.0"
  lazy val scalajHttp = "org.scalaj" %% "scalaj-http" % "2.4.1"
  lazy val json4sNative = "org.json4s" %% "json4s-native" % "3.6.2"
  lazy val jason4sExt = "org.json4s" %% "json4s-ext" % "3.6.2"
  lazy val typesafeConfig = "com.typesafe" % "config" % "1.3.2"
}
