package tech.ant8e.sbt.i18n

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.internal.io.Source

object SbtI18nPlugin extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = JvmPlugin

  object autoImport {
    val i18nBundlePackageSetting =
      settingKey[String]("Package name for the i18n bundle.")
    val generateI18NBundleTask =
      taskKey[Seq[File]]("The i18n bundle generation task.")

  }

  import autoImport._
  private val i18nSource =
    settingKey[File]("Default directory containing i18n configuration sources.")

  override lazy val projectSettings =
    inConfig(Compile)(
      watchSourceSettings ++
        Seq(
          i18nSource := sourceDirectory.value / "i18n",
          generateI18NBundleTask := generateFromTemplates(streams.value,
                                                          i18nSource.value,
                                                          sourceManaged.value / "sbt-i18n",
                                                          i18nBundlePackageSetting.value),
          mappings in packageSrc ++= managedSources.value pair (Path
            .relativeTo(sourceManaged.value) | Path.flat),
          sourceGenerators += generateI18NBundleTask.taskValue
        )) ++ Seq(i18nBundlePackageSetting := "org.example.i18n")

  def generateFromTemplates(streams: TaskStreams,
                            srcDir: sbt.File,
                            outDir: File,
                            packageName: String): Seq[File] = {
    val inputs = srcDir.allPaths
    streams.log.debug(s"Found ${inputs.get.size} template files in $srcDir.")
    streams.log.info(s"Generating i18n bundle in package $packageName.")
    val bundleFile = outDir / "Bundle.scala"
    IO.write(bundleFile,
             s"""package $packageName
         |
         |object Bundle {}""".stripMargin)

    Seq(bundleFile)
  }

  override lazy val buildSettings = Seq()

  override lazy val globalSettings = Seq()

  def allPaths(f: File) = f.allPaths

  def watchSourceSettings = Def.settings {
    Seq(
      watchSources in Defaults.ConfigGlobal +=
        new Source(i18nSource.value, AllPassFilter, NothingFilter)
    )
  }
}
