package tech.ant8e.sbt.i18n

import com.typesafe.config.ConfigFactory
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.internal.io.Source

import scala.util.{Failure, Try}

object SbtI18nPlugin extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = JvmPlugin

  object autoImport {
    val i18nBundlePackageName  =
      settingKey[String]("Package name for the i18n bundle.")
    val generateI18NBundleTask =
      taskKey[Seq[File]]("The i18n bundle generation task.")
    val i18nBreakOnMissingKeys =
      settingKey[Boolean]("Option to break generation task on missing keys in configuration.")

  }

  import autoImport._
  private val i18nSource =
    settingKey[File]("Directory containing i18n configuration sources.")

  override lazy val projectSettings =
    inConfig(Compile)(
      watchSourceSettings ++
        Seq(
          i18nSource             := sourceDirectory.value / "i18n",
          generateI18NBundleTask := generateFromSource(
            streams.value,
            i18nSource.value,
            sourceManaged.value / "sbt-i18n",
            i18nBundlePackageName.value,
            i18nBreakOnMissingKeys.value
          ),
          packageSrc / mappings ++= managedSources.value pair (Path
            .relativeTo(sourceManaged.value) | Path.flat),
          sourceGenerators += generateI18NBundleTask.taskValue
        )
    ) ++ Seq(
      i18nBundlePackageName  := "org.example.i18n",
      i18nBreakOnMissingKeys := false,
      libraryDependencies ++= Seq(
        "com.ibm.icu" % "icu4j" % "75.1"
      )
    )

  def generateFromSource(
      streams: TaskStreams,
      srcDir: sbt.File,
      outDir: File,
      packageName: String,
      breakOnMissingKeys: Boolean
  ): Seq[File] = {

    def parseSourceFile(f: File) =
      Try(ConfigFactory.parseFile(f).resolve()).recoverWith { case e =>
        streams.log.err(s"Unable to parse  $f: ${e.getMessage}")
        Failure(new RuntimeException(s"i18n parsing failed for $f", e))
      }.toOption

    val inputs     = srcDir.allPaths.get
    if (inputs.nonEmpty) streams.log.info(s"Generating i18n bundle in package $packageName.")
    streams.log.debug(s"Found ${inputs.size} template files in $srcDir.")
    val bundleFile = outDir / "Bundle.scala"

    val configs    = (inputs ** "*").pair(parseSourceFile, errorIfNone = false)
    val fullConfig =
      configs
        .foldLeft(ConfigFactory.empty()) { case (acc, (_, config)) =>
          acc.withFallback(config)
        }

    if (!fullConfig.isEmpty) {
      IO.write(bundleFile, BundleEmitter(fullConfig, packageName, breakOnMissingKeys).emit())
      Seq(bundleFile)
    } else
      Seq.empty
  }

  private def watchSourceSettings =
    Def.settings(
      Seq(
        watchSources in Defaults.ConfigGlobal += new Source(
          i18nSource.value,
          AllPassFilter,
          NothingFilter
        )
      )
    )
}
