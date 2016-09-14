package me.ele.amigo

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.BaseVariantOutput
import groovy.io.FileType
import groovy.xml.QName
import groovy.xml.XmlUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency

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
                    def generateCodeTask;
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
                        if (applicationName == null || applicationName.isEmpty()) {
                            applicationName = "android.app.Application"
                        }
                        appNode.attributes().put(nameAttr, "me.ele.amigo.Amigo")

                        Node hackAppNode = new Node(appNode, "activity")
                        hackAppNode.attributes().put("android:name", applicationName)
                        manifestFile.text = XmlUtil.serialize(node)

                        generateCodeTask = project.tasks.create(
                                name: "generate${variant.name.capitalize()}ApplicationInfo",
                                type: GenerateCodeTask) {
                            variantDirName variant.dirName
                            appName applicationName
                        }
                        generateCodeTask.execute()
                        println "generateCodeTask execute"
                    }

                    variant.javaCompile.doFirst {
                        variant.javaCompile.source generateCodeTask.outputDir()
                    }

                    if (!hasProguard(project, variant)) {
                        variant.javaCompile.doLast {
                            if (hasMultiDex(project, variant)) {
                                collectMultiDexInfo(project, variant)
                                generateKeepFiles(project, variant)
                            }
                        }
                    } else {
                        getProguardTask(project, variant).doLast {
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
