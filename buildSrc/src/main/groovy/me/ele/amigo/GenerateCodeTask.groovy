package me.ele.amigo

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class GenerateCodeTask extends DefaultTask {

    @Input
    String appName

    @Input
    String variantDirName

    @OutputDirectory
    File outputDir() {
        project.file("${project.buildDir}/generated/source/amigo/${variantDirName}")
    }

    @OutputFile
    File outputFile() {
        project.file("${outputDir().absolutePath}/me/ele/amigo/acd.java")
    }

    @TaskAction
    def taskAction() {
        def source = new JavaFileTemplate(['appName': appName]).getContent()
        def outputFile = outputFile()
        if (!outputFile.isFile()) {
            outputFile.delete()
            outputFile.parentFile.mkdirs()
        }
        outputFile.text = source
    }

}