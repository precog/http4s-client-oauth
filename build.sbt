import scala.collection.Seq

homepage in ThisBuild := Some(url("https://github.com/slamdata/http4s-client-oauth"))

scmInfo in ThisBuild := Some(ScmInfo(
  url("https://github.com/slamdata/http4s-client-oauth"),
  "scm:git@github.com:slamdata/http4s-client-oauth.git"))

// Include to also publish a project's tests
lazy val publishTestsSettings = Seq(
  Test / packageBin / publishArtifact := true)

lazy val root = project
  .in(file("."))
  .settings(noPublishSettings)
  .aggregate(core)

lazy val core = project
  .in(file("core"))
  .settings(addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"))
  .settings(
    name := "http4s-client-oauth",
    performMavenCentralSync := false,
    publishAsOSSProject := true,
    libraryDependencies ++= Seq(
      //"com.slamdata" %% "tectonic" % tectonicVersion.value,
      "org.typelevel" %% "cats-mtl-core" % "0.7.0",
      "com.slamdata" %% "slamdata-predef" % "0.1.1",
      "com.github.julien-truffaut" %% "monocle-core" % "1.6.0",
      "io.argonaut" %% "argonaut-monocle" % "6.2.3",
      "io.argonaut" %% "argonaut-cats" % "6.2.3",
      "org.http4s" %% "http4s-core" % "0.21.0-RC2",
      "org.http4s" %% "http4s-async-http-client" % "0.21.0-RC2",
      "org.http4s" %% "http4s-argonaut" % "0.21.0-RC2",
      "org.http4s" %% "http4s-dsl" % "0.21.0-RC2" % Test,
      "com.codecommit" %% "shims" % "2.0.0",
      "com.typesafe.akka" %% "akka-http" % "10.1.9",
      "com.typesafe.akka" %% "akka-stream" % "2.5.25",
      "org.scodec" %% "scodec-core" % "1.10.3",
      "org.specs2" %% "specs2-core" % "4.8.3" % Test,
      "org.specs2" %% "specs2-scalacheck" % "4.8.3" % Test,
      "org.specs2" %% "specs2-scalaz" % "4.8.3" % Test))
      //"com.slamdata" %% "quasar-foundation" % quasarVersion % Test classifier "tests"))
  .enablePlugins(AutomateHeaderPlugin)
