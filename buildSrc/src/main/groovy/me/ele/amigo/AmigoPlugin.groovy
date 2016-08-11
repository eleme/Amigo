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

        project.dependencies {
            compile 'me.ele:amigo-lib:0.0.4'
        }

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

                    def proguardTaskName = "transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}"
                    def proguardTask = project.tasks.findByName(proguardTaskName)
                    def task = proguardTask ? proguardTask : output.processResources;
                    task.doLast {
                        if (proguardTask) {
                            variant.mappingFile.eachLine { line ->
                                if (!line.startsWith(" ")) {
                                    String[] keyValue = line.split("->");
                                    String key = keyValue[0].trim()
                                    String value = keyValue[1].subSequence(0, keyValue[1].length() - 1).trim()
                                    if (key.equals(applicationName)) {
                                        applicationName = value
                                    }
                                }
                            }
                        }

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

                        if (proguardTask) {
                            def packageName = variant.generateBuildConfig.buildConfigPackageName
                            def classAddress = "${variant.javaCompiler.destinationDir}/${packageName.replace('.', '/')}/acd.class"
                            //add acd class into main.jar
                            File[] files = new File[1]
                            files[0] = new File(classAddress)
                            String proguardDir = "${project.buildDir}/intermediates/transforms/proguard/${variant.flavorName}"
                            String jarPath = Util.findFileInDir("main.jar", proguardDir)
                            Util.addFilesToExistingZip(new File(jarPath), files, Util.classEntryName(variant))
                        }
                    }


                }
            }
        }
    }
}
