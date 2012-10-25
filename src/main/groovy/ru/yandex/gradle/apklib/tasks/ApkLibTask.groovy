/*
 * Copyright 2012 Yandex.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.yandex.gradle.apklib.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by Vladimir Grachev (vgrachev@)
 * Date: 31.05.12
 * Time: 20:53
 */
class ApkLibTask extends DefaultTask {

    String baseName = null
    String appendix = null
    String version = null
    String classifier = null

    String extension = "apklib"

    final static String KEY_ANDROID_LIBRARY = "ant.android.library"

    def defineVariables() {
        if (baseName == null) baseName = project.archivesBaseName
        if (version == null) version = project.version
    }

    @TaskAction
    def create() {

        if (project.properties[KEY_ANDROID_LIBRARY] != 'true') {
            logger.info("We are not library. Skipping 'apklib' task.")
            return
        }

        defineVariables()

        logger.info("Creating application library: $project.buildDir/$baseName-$version.$extension")

        def libs = copyCompileDependenciesToDir("$project.projectDir/libs")

        try {
            def ant = new AntBuilder()
            ant.copy(todir: "$project.buildDir/$extension") {
                fileset(dir : "$project.projectDir") {
                    include(name: "assets/**")
                    include(name: "bin/**")
                    include(name: "res/**")
                    include(name: "src/**")
                    include(name: "libs/**")
                    include(name: "project.properties")
                    include(name: "AndroidManifest.xml")
                }
            }
        }
        finally {
            deleteCompileDependenciesToDir(libs, "$project.projectDir/libs")
        }

        InputStream stream = getClass().getClassLoader().getResourceAsStream("empty_build.xml")
        new File("$project.buildDir/$extension/build.xml").withWriter {it << stream.getText("UTF-8")}

        ant.zip(destfile: destWithSrcFile(), basedir: "$project.buildDir/$extension")
        ant.zip(destfile: destNoLibsFile(), basedir: "$project.buildDir/$extension") {
            exclude(name: "src/**")
            exclude(name: "libs/**")
        }
        ant.zip(destfile: destFile(), basedir: "$project.buildDir/$extension", excludes: "src/**")
        ant.zip(destfile: destResFile(), basedir: "$project.buildDir/$extension") {
            exclude(name: "src/**")
            exclude(name: "libs/**")
            exclude(name: "bin/classes.jar")
        }

        if (new File("$project.projectDir/bin/proguard/obfuscated.jar").exists()) {
            def ant = new AntBuilder()
            ant.copy(todir: "$project.buildDir/obfuscated-$extension") {
                fileset(dir : "$project.projectDir") {
                    include(name: "assets/**")
                    include(name: "bin/res/**")
                    include(name: "res/**")
                    include(name: "project.properties")
                    include(name: "AndroidManifest.xml")
                }
            }

            ant.zip(destfile: "$project.buildDir/obfuscated-$extension/bin/classes.jar") {
                zipfileset(src: "$project.projectDir/bin/proguard/obfuscated.jar") {
                    exclude(name: '**/R.class')
                    exclude(name: '**/R$*.class')
                }
            }

            ant.copy(todir: "$project.buildDir/obfuscated-$extension") {
                fileset(dir : "$project.buildDir/$extension") {
                    include(name: "build.xml")
                }
            }

            ant.zip(destfile: destObfuscatedFile(), basedir: "$project.buildDir/obfuscated-$extension")

            project.artifacts{
                archives file: destObfuscatedFile(), type: 'apklib'
            }
        }

        project.artifacts{
            archives file: destFile(), type: 'apklib'
            archives file: destWithSrcFile(), type: 'apklib'
            archives file: destNoLibsFile(), type: 'apklib'
            archives file: destResFile(), type: 'apklib'
        }
    }

    public File destObfuscatedFile() {
        defineVariables()
        return new File("$project.buildDir/$baseName-$version-obfuscated.$extension")
    }

    public File destWithSrcFile() {
        defineVariables()
        return new File("$project.buildDir/$baseName-$version-wsrc.$extension")
    }

    public File destNoLibsFile() {
        defineVariables()
        return new File("$project.buildDir/$baseName-$version-nolibs.$extension")
    }

    public File destFile() {
        defineVariables()
        return new File("$project.buildDir/$baseName-$version.$extension")
    }

    public File destResFile() {
        defineVariables()
        return new File("$project.buildDir/$baseName-$version-res.$extension")
    }

    def unpackDependencies() {

        int i = 1
        int count = 1
        try {
            def props = new Properties();
            new File("$project.projectDir/ant.properties").withInputStream { props.load(it) }

            while (props.stringPropertyNames().contains("android.library.reference." + i)) {
                if ("$ExportTask.EXPORT_PATH/" != props["android.library.reference." + i]) {
                    logger.info("Found apklib in ant.properties: android.library.reference." + count + " = " + props["android.library.reference." + i])
                    count++
                }
                i++
            }
        }
        catch (FileNotFoundException e) {
            logger.warn("No $project.projectDir/ant.properties file found.")
        }

        def ant = new AntBuilder()

        project.configurations['apklib'].files.each { file ->
            logger.info("Found apklib dependency: " + file.name + " " + file.path)

            new File("$project.buildDir/deps/$file.name").deleteDir()

            logger.info("Unpacking $file.name to $project.buildDir/deps/$file.name")
            ant.unzip(src: file.path,
                    dest: "$project.buildDir/deps/$file.name",
                    overwrite: "true")

            logger.info("Setting ant.property: android.library.reference.$count = build/deps/$file.name")

            project.ant.properties["android.library.reference.$count"] = "build/deps/$file.name"

            count++
        }
    }

    def copyCompileDependenciesToDir(String libsDir) {

        def libs = new ArrayList<File>();

        project.configurations['compile'].files.each { file ->
            project.logger.info("Found compile dependency: " + file.name + " " + file.path)

            def lib = new File("$libsDir/$file.name");
            libs.add(lib)
            lib.delete()

            if (! file.name.startsWith('android')) {
                project.logger.info("Copying $file.name to $libsDir/$file.name")
                project.ant.copy(todir: "$libsDir", file: "$file.path", overwrite: "true")
            }
        }

        return libs
    }

    def deleteCompileDependenciesToDir(ArrayList<File> libs, String dir) {
        libs.each { lib ->
            lib.delete()
        }
    }
}
