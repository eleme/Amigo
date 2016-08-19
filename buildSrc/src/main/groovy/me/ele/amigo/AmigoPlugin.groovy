package me.ele.amigo

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.BaseVariantOutput
import groovy.xml.Namespace
import groovy.xml.XmlUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.compile.JavaCompile

import java.util.jar.JarEntry
import java.util.jar.JarFile

class AmigoPlugin implements Plugin<Project> {

    String content = ""
    static final VERSION = "0.1.9"

    @Override
    void apply(Project project) {

        project.dependencies {
            compile "me.ele:amigo-lib:${VERSION}"
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

                        //fake original application as an activity, so it will be in main dex
                        Node node = (new XmlParser()).parse(manifestFile)
                        Node appNode = null
                        for (Node n : node.children()) {
                            if (n.name().equals("application")) {
                                appNode = n;
                                break
                            }
                        }
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

                            def classAddress = "${variant.javaCompiler.destinationDir}/me/ele/amigo/acd.class"
                            //add acd class into main.jar
                            File[] files = new File[1]
                            files[0] = new File(classAddress)
                            String proguardDir = "${project.buildDir}/intermediates/transforms/proguard/${variant.flavorName}"
                            String jarPath = Util.findFileInDir("main.jar", proguardDir)
                            Util.addFilesToExistingZip(new File(jarPath), files, "me/ele/amigo/acd.class")

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

    void collectMultiDexInfo(Project project, ApkVariant variant) {
        if (!hasProguard(project, variant)) {
            String jarPath = "${project.buildDir}/intermediates/exploded-aar/me.ele/amigo-lib/${VERSION}/jars/classes.jar"
            JarFile jarFile = new JarFile(jarPath)
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
                if (key.startsWith("me.ele.amigo")) {
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
