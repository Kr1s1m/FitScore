ThisBuild / version := "1.0.0"

lazy val fitscore    = "com.fitscore"
lazy val scala3Version = "3.3.1"

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// Common - contains domain model
///////////////////////////////////////////////////////////////////////////////////////////////////////////

lazy val core = (crossProject(JSPlatform, JVMPlatform) in file("common"))
  .settings(
    name         := "common",
    scalaVersion := scala3Version,
    organization := fitscore
  )
  .jvmSettings(
    // add here if necessary
  )
  .jsSettings(
    // Add JS-specific settings here
  )

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// Frontend
///////////////////////////////////////////////////////////////////////////////////////////////////////////

lazy val tyrianVersion = "0.6.1"
lazy val fs2DomVersion = "0.1.0"
lazy val laikaVersion  = "0.19.0"
lazy val circeVersion  = "0.14.0"

lazy val app = (project in file("app"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name         := "app",
    scalaVersion := scala3Version,
    organization := fitscore,
    libraryDependencies ++= Seq(
      "io.indigoengine" %%% "tyrian-io"     % tyrianVersion,
      "com.armanbilge"  %%% "fs2-dom"       % fs2DomVersion,
      "org.planet42"    %%% "laika-core"    % laikaVersion,
      "io.circe"        %%% "circe-core"    % circeVersion,
      "io.circe"        %%% "circe-parser"  % circeVersion,
      "io.circe"        %%% "circe-generic" % circeVersion
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    semanticdbEnabled := true,
    autoAPIMappings   := true
  )
  .dependsOn(core.js)

lazy val catsEffectVersion          = "3.3.14"
lazy val http4sVersion              = "0.23.15"
lazy val doobieVersion              = "1.0.0-RC1"
lazy val pureConfigVersion          = "0.17.1"
lazy val log4catsVersion            = "2.4.0"
lazy val tsecVersion                = "0.4.0"
lazy val scalaTestVersion           = "3.2.12"
lazy val scalaTestCatsEffectVersion = "1.4.0"
lazy val testContainerVersion       = "1.17.3"
lazy val logbackVersion             = "1.4.0"
lazy val jansiVersion               = "1.18"
lazy val slf4jVersion               = "2.0.0"
lazy val javaMailVersion            = "1.6.2"
lazy val stripeVersion              = "22.12.0"

lazy val server = (project in file("server"))
  .settings(
    name         := "server",
    scalaVersion := scala3Version,
    organization := fitscore,
    libraryDependencies ++= Seq(
      "org.typelevel"         %% "cats-effect"         % catsEffectVersion,
      "org.http4s"            %% "http4s-dsl"          % http4sVersion,
      "org.http4s"            %% "http4s-ember-server" % http4sVersion,
      "org.http4s"            %% "http4s-circe"        % http4sVersion,
      "io.circe"              %% "circe-generic"       % circeVersion,
      "io.circe"              %% "circe-fs2"           % circeVersion,
      "org.tpolecat"          %% "doobie-core"         % doobieVersion,
      "org.tpolecat"          %% "doobie-hikari"       % doobieVersion,
      "org.tpolecat"          %% "doobie-postgres"     % doobieVersion,
      "org.tpolecat"          %% "doobie-scalatest"    % doobieVersion    % Test,
      "com.github.pureconfig" %% "pureconfig-core"     % pureConfigVersion,
      "org.typelevel"         %% "log4cats-slf4j"      % log4catsVersion,
      "org.slf4j"              % "slf4j-simple"        % slf4jVersion,
      "io.github.jmcardon"    %% "tsec-http4s"         % tsecVersion,
      "com.sun.mail"           % "javax.mail"          % javaMailVersion,
      "com.stripe"             % "stripe-java"         % stripeVersion,
      "org.typelevel"         %% "log4cats-noop"       % log4catsVersion  % Test,
      "org.scalatest"         %% "scalatest"           % scalaTestVersion % Test,
      "org.typelevel"     %% "cats-effect-testing-scalatest" % scalaTestCatsEffectVersion % Test,
      "org.testcontainers" % "testcontainers"                % testContainerVersion       % Test,
      "org.testcontainers" % "postgresql"                    % testContainerVersion       % Test,
      "ch.qos.logback"     % "logback-classic"               % logbackVersion             % Test,
      "org.fusesource.jansi" % "jansi"                       % jansiVersion
    ),
    Compile / mainClass := Some(fitscore + ".Application")
  )
  .dependsOn(core.jvm)


//val tapirVersion = "1.10.6"
//
//lazy val rootProject = (project in file(".")).settings(
//  Seq(
//    name := "fitscore",
//    version := "0.1.0-SNAPSHOT",
//    organization := "com.fitscore",
//    scalaVersion := "3.4.2",
//    libraryDependencies ++= Seq(
//      "org.typelevel" %% "cats-core" % "2.10.0",
//      "org.typelevel" %% "cats-effect" % "3.5.4",
//      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
//      "org.http4s" %% "http4s-ember-server" % "0.23.27",
//      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
//      "ch.qos.logback" % "logback-classic" % "1.5.6",
//      "org.fusesource.jansi" % "jansi" % "1.18",
//      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
//      "org.scalatest" %% "scalatest" % "3.2.18" % Test,
//      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test,
//      "com.softwaremill.sttp.client3" %% "circe" % "3.9.6" % Test
//    )
//  )
//)
//
//assembly / assemblyMergeStrategy := {
//  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//  case x =>
//    val oldStrategy = (assembly / assemblyMergeStrategy).value
//    oldStrategy(x)
//}
