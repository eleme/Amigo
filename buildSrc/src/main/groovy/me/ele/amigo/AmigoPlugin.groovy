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
import java.util.regex.Pattern

class AmigoPlugin implements Plugin<Project> {

    static final String GROUP = 'me.ele'
    static final String NAME = 'amigo'

    String content = ""
    String version = ""

    @Override
    void apply(Project project) {

        AmigoExtension ext = project.extensions.create('amigo', AmigoExtension)

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

        project.afterEvaluate {

            if (ext.disable) {
                println 'amigo is disabled'
                return
            }

            project.android.applicationVariants.all { ApkVariant variant ->

                // check instant run which conflicts with us
                println 'check instant run for variant : ' + variant.name
                Task instantRunTask = project.tasks.findByName(
                        "transformClassesWithInstantRunVerifierFor${variant.name.capitalize()}")
                if (instantRunTask) {
                    throw RuntimeException("Sorry, instant run conflicts with Amigo, so please " +
                            "disable Instant Run")
                }

                Task prepareDependencyTask = project.tasks.findByName(
                        "prepare${variant.name.capitalize()}Dependencies")
                prepareDependencyTask.doFirst {
                    clearAmigoDependency(project)
                }

                variant.outputs.each { BaseVariantOutput output ->

                    //process${variantData.variantConfiguration.fullName.capitalize()}Manifest
                    String processManifestTaskName = output.processManifest.name;
                    String taskName = "";
                    String pattern = '^process(.+)Manifest$';
                    if (Pattern.matches(pattern, processManifestTaskName)) {
                        taskName = processManifestTaskName.replace("process", "generate")
                                .replace("Manifest", "AmigoApplicationInfo");
                    } else {
                        taskName = "generate${variant.name.capitalize()}AmigoApplicationInfo"
                    }

                    def generateCodeTask = project.tasks.create(taskName, GenerateCodeTask);
                    generateCodeTask.variantDirName = variant.dirName
                    generateCodeTask.dependsOn  output.processManifest

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

                        QName nameAttr = new QName("http://schemas.android.com/apk/res/android",
                                'name', 'android');
                        def applicationName = appNode.attribute(nameAttr)
                        if (applicationName == null || applicationName.isEmpty()) {
                            applicationName = "android.app.Application"
                        }
                        appNode.attributes().put(nameAttr, "me.ele.amigo.Amigo")

                        Node hackAppNode = new Node(appNode, "activity")
                        hackAppNode.attributes().put("android:name", applicationName)
                        manifestFile.bytes = XmlUtil.serialize(node).getBytes("UTF-8")

                        generateCodeTask.appName = applicationName
                    }

                    variant.registerJavaGeneratingTask(generateCodeTask, generateCodeTask
                            .outputDir())

                    def task_1 = !variant.obfuscation ? variant.javaCompiler : variant.obfuscation

                    // TODO
                    // new jack & jill toolchain still have some problems in splitting
                    // the main dex
                    if (hasMultiDex(project, variant)) {
                        task_1.doLast {
                            collectMainDexInfo(project, variant)
                            updateMainDexKeepList(project, variant)
                        }
                    }

                }
            }
        }
    }

    static void clearAmigoDependency(Project project) {
        File jarPath = new File("${project.buildDir}/intermediates/exploded-aar/me.ele/amigo-lib")
        if (jarPath.exists()) {
            jarPath.delete()
        }
    }

    void collectMainDexInfo(Project project, ApkVariant variant) {
        if (!variant.obfuscation) {
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
                if (key.startsWith("me.ele.amigo")
                        || key.endsWith('R$layout')
                        || key.endsWith('R$style')) {
                    content += "\n"
                    content += "${value.replace(".", "/")}.class"
                }
            }
        }

        content += "\n"
        content += "me/ele/amigo/acd.class"
    }

    void updateMainDexKeepList(Project project, ApkVariant variant) {
        if (!hasMultiDex(project, variant)) {
            return
        }
        def task = getMultiDexTask(project, variant)
        File mainDexList = new File(
                "${project.buildDir}/intermediates/multi-dex/${variant.dirName}/maindexlist.txt")
        task.doLast {
            mainDexList << content
        }
    }

    static Task getMultiDexTask(Project project, ApkVariant variant) {
        String multiDexTaskName = "transformClassesWithMultidexlistFor${variant.name.capitalize()}"
        return project.tasks.findByName(multiDexTaskName);
    }

    static boolean hasMultiDex(Project project, ApkVariant variant) {
        return getMultiDexTask(project, variant) != null
    }

}
