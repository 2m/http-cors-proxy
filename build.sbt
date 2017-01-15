organization := "lt.dvim"
name := "http-cors-proxy"
scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "fr.hmil"                %%% "roshttp"             % "2.0.1-2m",
  "com.lihaoyi"            %%% "upickle"             % "0.4.4",
  "org.scalatest"          %%% "scalatest"           % "3.0.0"   % Test,
  "com.github.ldaniels528" %%% "scalajs-nodejs-http" % "0.2.3.2" % Test // not available for scala 2.12
)

enablePlugins(ScalaJSPlugin)
scalaJSModuleKind := ModuleKind.CommonJSModule

TaskKey[Unit]("gcDeploy") := {
  val gcTarget = target.value / "gcloud"
  val function = gcTarget / "function.js"
  val functionName = "corsProxy"
  sbt.IO.copyFile((fastOptJS in Compile).value.data, function)
  s"gcloud alpha functions deploy $functionName --local-path ${gcTarget.getAbsolutePath} --stage-bucket ${name.value} --trigger-http"!
}

TaskKey[Unit]("gcDeployLocal") := {
  val gcTarget = target.value / "gcloud"
  val function = gcTarget / "function.js"
  val functionName = "corsProxy"
  sbt.IO.copyFile((fastOptJS in Compile).value.data, function)
  s"functions deploy $functionName ${gcTarget.getAbsolutePath} --trigger-http"!
}
