package me.ele.amigo

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.BaseVariantOutput
import groovy.xml.Namespace
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class AmigoPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.withId('com.android.application') {
            project.android.applicationVariants.all { ApkVariant variant ->
                variant.outputs.each { BaseVariantOutput output ->

                    def applicationName = null

                    output.processManifest.doLast {
                        File manifestFile = output.processManifest.manifestOutputFile
                        def manifest = new XmlParser().parse(manifestFile)
                        def androidTag = new Namespace("http://schemas.android.com/apk/res/android", 'android')
                        applicationName = manifest.application[0].attribute(androidTag.name)
                        manifestFile.text = manifestFile.text.replace(applicationName, "me.ele.amigo.Amigo")
                    }

                    output.processResources.doLast {
                        def generateCodeTask = project.tasks.create(
                                name: "generate${variant.name.capitalize()}ApplicationInfo",
                                type: GenerateCodeTask) {
                            variantDirName variant.dirName
                            application applicationName
                            packageName variant.generateBuildConfig.buildConfigPackageName
                        }
                        generateCodeTask.execute()

                        String taskName = "mapper${variant.name.capitalize()}InnerJavac"
                        def javac = variant.javaCompiler
                        project.task(type: JavaCompile, overwrite: true, taskName) { JavaCompile jc ->
                            jc.source generateCodeTask.outputDir()
                            jc.destinationDir javac.destinationDir
                            jc.classpath = project.files(new File(((JavaCompile) javac).options.bootClasspath))
                            jc.sourceCompatibility javac.sourceCompatibility
                            jc.targetCompatibility javac.targetCompatibility
                        }
                        project.tasks.getByName(taskName).execute()
                    }
                }
            }
        }
    }
}
