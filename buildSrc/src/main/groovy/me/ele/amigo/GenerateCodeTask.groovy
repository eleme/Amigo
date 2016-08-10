package me.ele.amigo

import jdk.internal.util.xml.impl.Input
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class GenerateCodeTask extends DefaultTask {

    @Input
    String application

    @Input
    String packageName

    @Input
    String variantDirName

    @OutputDirectory
    File outputDir() {
        project.file("${project.buildDir}/generated/source/amigo/${variantDirName}")
    }

    @OutputFile
    File outputFile() {
        project.file("${outputDir().absolutePath}/${packageName.replace('.', '/')}/acd.java")
    }

    @TaskAction
    def taskAction() {
        def source = new JavaFileTemplate(['packageName': packageName, 'application': application]).getContent()
        def outputFile = outputFile()
        if (!outputFile.isFile()) {
            outputFile.delete()
            outputFile.parentFile.mkdirs()
        }
        outputFile.text = source
    }

}