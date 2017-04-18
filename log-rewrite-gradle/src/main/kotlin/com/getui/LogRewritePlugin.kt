package com.getui

import com.android.build.gradle.BaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import  com.android.build.gradle.LibraryPlugin;
import  com.android.build.gradle.LibraryExtension;
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Created by fox on 12/04/2017.
 */

class LogRewritePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        project.plugins.withType(LibraryPlugin::class.java) {
            val create = project.tasks.create("logRewrite", LoRewriteTask::class.java)
            val task = project.tasks.findByName("preBuild")
            if (task != null) {
                task.dependsOn.add(create)
            }
        }
    }

}

open class LoRewriteTask : DefaultTask() {
    @TaskAction
    fun rewriteLog() {
        val targetPath = File(project.buildDir,"rewrite")
        val androidExtension = project.extensions.getByName("android") as BaseExtension
        val sourceSets = androidExtension.sourceSets
        for (ss in sourceSets) {
            val java = ss.java
            val name = ss.name
            val srcDirs = java.srcDirs
            val file = File(targetPath, name)
            file.mkdirs()


        }

    }
}