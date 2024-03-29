import play.sbt.routes.RoutesKeys

name := "BPMN_Webapp"

version := "1.0"

lazy val `bpmn_webapp` = (project in file(".")).enablePlugins(PlayScala)

pipelineStages := Seq(gzip)

scalaVersion := "2.11.7"

herokuAppName in Compile := "bpmn-modeller"

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  filters,
  ws,
  specs2 % Test,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.13",
  "org.scaldi" %% "scaldi-play" % "0.5.15",
  "com.mohiva" %% "play-silhouette" % "4.0.0-RC1",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "4.0.0-RC1",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "4.0.0-RC1",
  "com.mohiva" %% "play-silhouette-persistence" % "4.0.0-RC1",
  "com.mohiva" %% "play-silhouette-testkit" % "4.0.0-RC1" % "test",
  "com.adrianhurt" %% "play-bootstrap" % "1.0-P25-B3",
  "org.webjars.bower" % "bpmn-js" % "0.15.1"
)

scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen" // Warn when numerics are widened.
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

RoutesKeys.routesImport += "play.modules.reactivemongo.PathBindables._"

fork in run := true
