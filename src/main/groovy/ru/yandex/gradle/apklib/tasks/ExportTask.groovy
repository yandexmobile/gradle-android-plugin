package ru.yandex.gradle.apklib.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.apache.tools.ant.taskdefs.Property

/**
 * Created by Vladimir Grachev (rook@)
 * Date: 04.10.12
 * Time: 17:00
 */
class ExportTask extends DefaultTask{

    private static final String KEY_TARGET = "target"
    private static final String KEY_VERSION_CODE = "version.code"
    private static final String KEY_VERSION_NAME = "version.name"

    private static final String DEFAULT_VALUE_TARGET = "android-15"


    public final static String EXPORT_PATH = ".gradle_export"

    private def properties = new Properties();

    @TaskAction
    def export() {
        properties.addEntry(new Property(KEY_TARGET, DEFAULT_VALUE_TARGET))
        properties.addEntry(new Property(KEY_VERSION_CODE, project.properties[KEY_VERSION_CODE]))
        properties.addEntry(new Property(KEY_VERSION_NAME, project.properties[KEY_VERSION_NAME]))

        unpackApkLibs(EXPORT_PATH)
        unpackLibs(EXPORT_PATH + "/libs")
        createManifest(EXPORT_PATH)
        createProjectProperties(EXPORT_PATH)
        createBuildXml(EXPORT_PATH)
        registerExportedLibrary(EXPORT_PATH)
    }

    def unpackApkLibs(String exportPath) {
        int count = 1

//        def props = project.ant.properties;

//        while (props.containsKey("android.library.reference." + count)) {
//            logger.info("Found apklib in properties: android.library.reference." + count + " = " + props["android.library.reference." + count])
//            count++
//        }

        def ant = new AntBuilder()

        project.configurations['apklib'].files.each { file ->
            logger.info("Found apklib dependency: " + file.name + " " + file.path)

            new File("$project.projectDir/$exportPath/$file.name").deleteDir()

            logger.info("Unpacking $file.name to $project.projectDir/$exportPath/$file.name")
            ant.unzip(src: file.path,
                    dest: "$project.projectDir/$exportPath/$file.name",
                    overwrite: "true")

            logger.info("Setting ant.property: android.library.reference.$count = $exportPath/$file.name")

//            project.ant.properties["android.library.reference.$count"] = "$exportPath/$file.name"
            properties["android.library.reference.$count"] = "$exportPath/$file.name"

            count++
        }
    }

    def unpackLibs(String exportPath) {
        project.configurations['compile'].files.each { file ->
            logger.info("Found compile dependency: " + file.name + " " + file.path)

            if (! file.name.startsWith('android')) {
                logger.info("Copying $file.name to $project.projectDir/$exportPath/$file.name")
                ant.copy(todir: "$project.projectDir/$exportPath", file: "$file.path", overwrite: "true")
            }
        }
    }

    def createManifest(String exportPath) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("AndroidManifestExport.xml")
        new File("$project.buildDir/$exportPath/AndroidManifest.xml").withWriter {it << stream.getText("UTF-8")}
    }

    def createProjectProperties(String exportPath) {
        new File("$project.projectDir/$exportPath/project.properties").withWriter { file ->
            properties.each { property ->
                file << property.key
                file << "="
                file << property.value
                file << "\n"
            }
        }
    }

    def createBuildXml(String exportPath) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("empty_build.xml")
        new File("$project.buildDir/$exportPath/build.xml").withWriter {it << stream.getText("UTF-8")}
    }

    def registerExportedLibrary() {

    }


}
