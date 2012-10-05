package ru.yandex.gradle.apklib.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by Vladimir Grachev (rook@)
 * Date: 04.10.12
 * Time: 17:00
 */
class ExportTask extends DefaultTask{

    private static final String KEY_TARGET = "target"
    private static final String KEY_VERSION_CODE = "version.code"
    private static final String KEY_VERSION_NAME = "version.name"
    private static final String KEY_ANDROID_LIBRARY = "android.library"

    private static final String DEFAULT_VALUE_TARGET = "android-15"


    public final static String EXPORT_PATH = ".gradle_export"

    @TaskAction
    def export() {
        def props = new Properties();

        props[KEY_TARGET] = DEFAULT_VALUE_TARGET
        props[KEY_VERSION_CODE] = project.properties[KEY_VERSION_CODE]
        props[KEY_VERSION_NAME] = project.properties[KEY_VERSION_NAME]
        props[KEY_ANDROID_LIBRARY] = "true"

        unpackApkLibs(EXPORT_PATH, props)
        unpackLibs(EXPORT_PATH + "/libs")
        createManifest(EXPORT_PATH)
        createProjectProperties(EXPORT_PATH, props)
        createBuildXml(EXPORT_PATH)
        createSrcDir(EXPORT_PATH)
        registerExportedLibrary(EXPORT_PATH)
    }

    def unpackApkLibs(String exportPath, Properties props) {
        int count = 1

        def ant = new AntBuilder()

        project.configurations['apklib'].files.each { file ->
            logger.info("Found apklib dependency: " + file.name + " " + file.path)

            new File("$project.projectDir/$exportPath/$file.name").deleteDir()

            logger.info("Unpacking $file.name to $project.projectDir/$exportPath/$file.name")
            ant.unzip(src: file.path,
                    dest: "$project.projectDir/$exportPath/$file.name",
                    overwrite: "true")

            logger.info("Setting ant.property: android.library.reference.$count = $exportPath/$file.name")

            props["android.library.reference.$count"] = "$file.name"

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
        InputStream stream = getClass().getClassLoader().getResourceAsStream("AndroidManifest_Export.xml")
        new File("$project.projectDir/$exportPath/AndroidManifest.xml").withWriter {it << stream.getText("UTF-8")}
    }

    def createProjectProperties(String exportPath, Properties props) {

        project.hideProperties.prefix = ".export"
        project.hideProperties.hidePropertiesFiles()

        new File("$project.projectDir/$exportPath/project.properties").withWriter { file ->
            props.each { property ->
                file << property.key
                file << "="
                file << property.value
                file << "\n"
            }
        }
    }

    def createBuildXml(String exportPath) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("build.xml")
        new File("$project.projectDir/$exportPath/build.xml").withWriter {it << stream.getText("UTF-8")}

        stream = getClass().getClassLoader().getResourceAsStream("build.xml")
        new File("$project.projectDir/build.xml").withWriter {it << stream.getText("UTF-8")}
    }

    def createSrcDir(String exportPath) {
        new File("$project.projectDir/$exportPath/src").mkdir()
    }

    def registerExportedLibrary(String exportPath) {
        new File("$project.projectDir/ant.properties").withWriter {file ->
            project.ant.properties.each { property ->
                    file << property.key
                    file << "="
                    file << property.value
                    file << "\n"
            }

            def num = 1

            while (project.ant.properties.containsKey("android.library.reference.$num") &&
                    "$exportPath/" != project.ant.properties["$exportPath/"]) {
                num++
            }
            file << "android.library.reference.$num=$exportPath/\n"

        }
    }


}
