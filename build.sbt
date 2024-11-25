name                             := "sbt-i18n"
ThisBuild / organization         := "tech.ant8e"
ThisBuild / organizationName     := "ant8e"
ThisBuild / organizationHomepage := Some(url("https://github.com/ant8e"))

ThisBuild / scmInfo    := Some(
  ScmInfo(
    url("https://github.com/ant8e/sbt-i18n"),
    "scm:git@github.com:ant8e/sbt-i18n.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "ant8e",
    name = "Antoine Comte",
    email = "antoine@comte.cc",
    url = url("https://github.com/ant8e/sbt-i18n")
  )
)

ThisBuild / description := "An sbt plugin to transform your i18n bundles into Scala code."
ThisBuild / startYear   := Some(2018)

ThisBuild / licenses := List(
  "Apache 2" -> new URI("http://www.apache.org/licenses/LICENSE-2.0.txt").toURL
)

ThisBuild / homepage := Some(url("https://github.com/ant8e/sbt-i18n"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo            := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle    := true

scalaVersion := "2.12.18"

sbtPlugin := true

libraryDependencies += "com.typesafe"     % "config"    % "1.4.3"
libraryDependencies += "com.ibm.icu"      % "icu4j"     % "75.1"
libraryDependencies += "com.google.guava" % "guava"     % "33.3.1-jre"
libraryDependencies += "org.scalactic"   %% "scalactic" % "3.2.17" % "test"
libraryDependencies += "org.scalatest"   %% "scalatest" % "3.2.17" % "test"

console / initialCommands := """import tech.ant8e.sbt.i18n._"""

enablePlugins(SbtPlugin)
// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)

ThisBuild / scalafmtOnCompile := true

enablePlugins(GitVersioning)
git.useGitDescribe := true
