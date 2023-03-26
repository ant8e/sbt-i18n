name         := "sbt-i18n"
organization := "tech.ant8e"

licenses += "Apache 2.0 License" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")
startYear                        := Some(2018)
scalaVersion                     := "2.12.15"

sbtPlugin := true

libraryDependencies += "com.typesafe"   % "config"    % "1.4.2"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.15" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9"  % "test"

bintrayPackageLabels := Seq("sbt", "plugin")
bintrayVcsUrl        := Some("git@github.com:ant8e/sbt-i18n.git")

initialCommands in console := """import tech.ant8e.sbt.i18n._"""

enablePlugins(SbtPlugin)
// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)

scalafmtOnCompile in ThisBuild := true

enablePlugins(GitVersioning)
git.useGitDescribe := true
