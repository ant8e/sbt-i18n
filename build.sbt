name := "sbt-i18n"
organization := "tech.ant8e"
version := "0.1-SNAPSHOT"

sbtPlugin := true

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

bintrayPackageLabels := Seq("sbt", "plugin")
bintrayVcsUrl := Some("git@github.com:ant8e/sbt-i18n.git")

initialCommands in console := """import tech.ant8e.sbt.i18n._"""

// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)

scalafmtOnCompile in ThisBuild := true
