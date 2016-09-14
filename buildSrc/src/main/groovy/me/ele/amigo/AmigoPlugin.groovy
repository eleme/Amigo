package me.ele.amigo

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.BaseVariantOutput
import groovy.io.FileType
import groovy.xml.Namespace
import groovy.xml.QName
import groovy.xml.XmlUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.compile.JavaCompile

import java.util.jar.JarEntry
import java.util.jar.JarFile

class AmigoPlugin implements Plugin<Project> {

    static final String GROUP = 'me.ele'
    static final String NAME = 'amigo'

    String content = ""
    String version = ""

    @Override
    void apply(Project project) {

        Configuration configuration = project.rootProject.buildscript.configurations.getByName(
            'classpath')
        configuration.allDependencies.all { Dependency dependency ->
            if (dependency.group == GROUP && dependency.name == NAME) {
                version = dependency.version
            }
        }
        println 'amigo plugin version: ' + version

        project.dependencies {
            if (Util.containsProject(project, 'amigo-lib')) {
                compile project.project(':amigo-lib')
            } else {
                compile "me.ele:amigo-lib:${version}"
            }
        }

        project.plugins.withId('com.android.application') {
            project.android.applicationVariants.all { ApkVariant variant ->

                // check instant run which conflicts with us
                println 'check instant run'
                Task instantRunTask = project.tasks.findByName("transformClassesWithInstantRunVerifierFor${variant.name.capitalize()}")
                if (instantRunTask) {
                    throw RuntimeException("Sorry, instant run conflicts with Amigo, so please disable Instant Run")
                }

                Task prepareDependencyTask = project.tasks.findByName("prepare${variant.name.capitalize()}Dependencies")
                prepareDependencyTask.doFirst {
                    clearAmigoDependency(project)
                }

                variant.outputs.each { BaseVariantOutput output ->

                    def applicationName = null
                    File manifestFile = output.processManifest.manifestOutputFile
                    if (manifestFile.exists()) {
                        manifestFile.delete()
                    }

                    output.processManifest.doLast {
                        manifestFile = output.processManifest.manifestOutputFile

                        //fake original application as an activity, so it will be in main dex
                        Node node = (new XmlParser()).parse(manifestFile)
                        Node appNode = null
                        for (Node n : node.children()) {
                            if (n.name().equals("application")) {
                                appNode = n;
                                break
                            }
                        }

                        QName nameAttr = new QName("http://schemas.android.com/apk/res/android", 'name', 'android');
                        applicationName = appNode.attribute(nameAttr)
                        if(applicationName == null || applicationName.isEmpty()) {
                            applicationName = "android.app.Application"
                        }
                        appNode.attributes().put(nameAttr, "me.ele.amigo.Amigo")

                        Node hackAppNode = new Node(appNode, "activity")
                        hackAppNode.attributes().put("android:name", applicationName)
                        manifestFile.text = XmlUtil.serialize(node)
                    }

                    if (hasProguard(project, variant)) {
                        getProguardTask(project, variant).doLast {
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

                            GenerateCodeTask generateCodeTask = project.tasks.create(
                                    name: "generate${variant.name.capitalize()}ApplicationInfo",
                                    type: GenerateCodeTask) {
                                variantDirName variant.dirName
                                appName applicationName
                            }
                            generateCodeTask.execute()

                            String taskName = "compile${variant.name.capitalize()}CodeForApp"
                            def javac = variant.javaCompiler
                            project.task(type: JavaCompile, overwrite: true, taskName) { JavaCompile jc ->
                                jc.source generateCodeTask.outputDir()
                                jc.destinationDir javac.destinationDir
                                jc.classpath = project.files(new File(((JavaCompile) javac).options.bootClasspath))
                                jc.sourceCompatibility javac.sourceCompatibility
                                jc.targetCompatibility javac.targetCompatibility
                            }
                            project.tasks.getByName(taskName).execute()

                            String entryName = "me/ele/amigo/acd.class"
                            def classAddress = "${variant.javaCompiler.destinationDir}/${entryName}"
                            //add acd class into main.jar
                            File[] files = new File[1]
                            files[0] = new File(classAddress)
                            String proguardDir = "${project.buildDir}/intermediates/transforms/proguard/${variant.flavorName}"
                            File mainJarFile = new File(Util.findFileInDir("main.jar", proguardDir))
                            Util.deleteZipEntry(mainJarFile, [entryName])
                            Util.addFilesToExistingZip(mainJarFile, files, entryName)

                            if (hasMultiDex(project, variant)) {
                                collectMultiDexInfo(project, variant)
                                generateKeepFiles(project, variant)
                            }
                        }
                    } else {
                        variant.javaCompile.doLast {
                            GenerateCodeTask generateCodeTask = project.tasks.create(
                                    name: "generate${variant.name.capitalize()}ApplicationInfo",
                                    type: GenerateCodeTask) {
                                variantDirName variant.dirName
                                appName applicationName
                            }
                            generateCodeTask.execute()
                            println "generateCodeTask execute"

                            String taskName = "compile${variant.name.capitalize()}CodeForApp"
                            def javac = variant.javaCompiler
                            project.task(type: JavaCompile, overwrite: true, taskName) { JavaCompile jc ->
                                jc.source generateCodeTask.outputDir()
                                jc.destinationDir javac.destinationDir
                                jc.classpath = project.files(new File(((JavaCompile) javac).options.bootClasspath))
                                jc.sourceCompatibility javac.sourceCompatibility
                                jc.targetCompatibility javac.targetCompatibility
                            }
                            project.tasks.getByName(taskName).execute()

                            if (hasMultiDex(project, variant)) {
                                collectMultiDexInfo(project, variant)
                                generateKeepFiles(project, variant)
                            }
                        }
                    }
                }
            }
        }
    }

    void clearAmigoDependency(Project project) {
        File jarPath = new File("${project.buildDir}/intermediates/exploded-aar/me.ele/amigo-lib")
        if (jarPath.exists()) {
            jarPath.delete()
        }
    }

    void collectMultiDexInfo(Project project, ApkVariant variant) {
        if (!hasProguard(project, variant)) {
            File dir = new File("${project.buildDir}/intermediates/exploded-aar/me.ele/amigo-lib")
            JarFile jarFile
            if (dir.exists()) {
                dir.eachFileRecurse(FileType.FILES) { File file ->
                    if (file.name == 'classes.jar') {
                        jarFile = new JarFile(file)
                        return
                    }
                }
            }
            Enumeration<JarEntry> enumeration = jarFile.entries()
            while (enumeration.hasMoreElements()) {
                JarEntry entry = enumeration.nextElement()
                content += "\n"
                content += entry.name
            }
            content += "\n"
            content += "me/ele/amigo/acd.class"
            return
        }

        variant.mappingFile.eachLine { line ->
            if (!line.startsWith(" ")) {
                String[] keyValue = line.split("->");
                String key = keyValue[0].trim()
                String value = keyValue[1].subSequence(0, keyValue[1].length() - 1).trim()
                if (key.startsWith("me.ele.amigo") || key.endsWith('R$layout') || key.endsWith('R$style')) {
                    content += "\n"
                    content += "${value.replace(".", "/")}.class"
                }
            }
        }

        content += "\n"
        content += "me/ele/amigo/acd.class"
    }

    void generateKeepFiles(Project project, ApkVariant variant) {
        if (!hasMultiDex(project, variant)) {
            return
        }
        def task = project.tasks.getByName("transformClassesWithMultidexlistFor${variant.name.capitalize()}")
        File mainDexList = new File("${project.buildDir}/intermediates/multi-dex/${variant.dirName}/maindexlist.txt")
        task.doLast {
            mainDexList << content
        }
    }

    Task getMultiDexTask(Project project, ApkVariant variant) {
        String multiDexTaskName = "transformClassesWithMultidexlistFor${variant.name.capitalize()}"
        return project.tasks.findByName(multiDexTaskName);
    }

    Task getProguardTask(Project project, ApkVariant variant) {
        String proguardTaskName = "transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}"
        return project.tasks.findByName(proguardTaskName)
    }

    boolean hasProguard(Project project, ApkVariant variant) {
        return getProguardTask(project, variant) != null
    }

    boolean hasMultiDex(Project project, ApkVariant variant) {
        return getMultiDexTask(project, variant) != null
    }

}
