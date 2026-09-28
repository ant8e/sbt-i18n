libraryDependencies += "org.scala-sbt"  %% "scripted-plugin" % sbtVersion.value
addSbtPlugin("org.scalameta"  % "sbt-scalafmt" % "2.6.2")
addSbtPlugin("com.github.sbt" % "sbt-git"      % "2.2.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.2")
addSbtPlugin("com.github.sbt" % "sbt-pgp"      % "2.3.1")
