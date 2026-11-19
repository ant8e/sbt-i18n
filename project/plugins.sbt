libraryDependencies += "org.scala-sbt"  %% "scripted-plugin" % sbtVersion.value
addSbtPlugin("org.scalameta"  % "sbt-scalafmt" % "2.6.2")
addSbtPlugin("com.github.sbt" % "sbt-git"      % "3.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.2")
addSbtPlugin("com.github.sbt" % "sbt-pgp"      % "2.4.0")
