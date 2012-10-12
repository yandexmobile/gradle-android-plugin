package ru.yandex.gradle.apklib.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by Vladimir Grachev (rook@)
 * Date: 05.10.12
 * Time: 1:03
 */
class HidePropertiesTask extends DefaultTask {

    def prefix = ".hidden"

    @TaskAction
    def hidePropertiesFiles() {
        hideFile("ant.properties")
        hideFile("project.properties")
        hideFile("local.properties")
        InputStream stream = getClass().getClassLoader().getResourceAsStream("project.properties.stub")
        new File("$project.projectDir/project.properties").withWriter {it << stream.getText("UTF-8")}
    }

    def hideFile(String filename) {
        try {
            ant.move(file: "$project.projectDir/$filename", toFile: "$project.projectDir/$prefix/$filename");
            logger.info("File $filename was hidden by gradle-android-plugin.")
        }
        catch (Throwable e) {
            logger.info("File $filename was not found & was not hidden.")
        }
    }
}
