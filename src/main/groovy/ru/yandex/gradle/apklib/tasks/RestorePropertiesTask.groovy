package ru.yandex.gradle.apklib.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by Vladimir Grachev (rook@)
 * Date: 05.10.12
 * Time: 1:05
 */
class RestorePropertiesTask extends DefaultTask {

    def prefix = ".hidden"

     @TaskAction
     def restorePropertiesFiles() {
         restoreFile("project.properties")
         restoreFile("ant.properties")
         restoreFile("local.properties")
     }

     def restoreFile(String filename) {
         try {
             ant.move(file: "$project.projectDir/$prefix/$filename", toFile: "$project.projectDir/$filename");
             logger.info("Hidden file $filename was restored by gradle-android-plugin.")
         }
         catch (Throwable e) {
             logger.info("Hidden file $filename was not found & was not restored.")
         }
     }
 }
