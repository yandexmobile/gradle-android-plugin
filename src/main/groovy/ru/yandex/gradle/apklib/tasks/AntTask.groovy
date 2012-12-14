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
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskAction
import ru.yandex.gradle.apklib.SdkHelper
import org.gradle.api.GradleScriptException

/**
 * Created by Vladimir Grachev (vgrachev@)
 * Date: 04.06.12
 * Time: 12:21
 */
class AntTask extends DefaultTask {

    def target = getName()

    def libs = new ArrayList<File>();

    def exec = new Exec()

    def versions = [ldpi:100000, mdpi:200000, hdpi:300000, xhdpi:400000]

    def BUILD_FILENAME = "gradle_build.xml"
    def SDK_BUILD_FILENAME = "gradle_sdk_build.xml"

    @TaskAction
    def runAnt() {
        def libsDir = "$project.projectDir/libs"
            if (getName() != 'installr' && getName() != 'installd') {
                copyJarsToDir(libsDir)
                project.apklib.unpackDependencies()
                if (project.properties.containsKey('dpi')) {
                    def dpi = project.properties['dpi']
                    def code = versions[dpi]
                    if (code == null) code = 0
                    project.ant.properties['version.code'] = code + Integer.parseInt(project.properties['version.code'])
                    logger.lifecycle("Version Code: " + project.ant.properties['version.code'])
                }
            }
        try {
            def build = getBuildScripts()
            project.hideProperties.hidePropertiesFiles()
            project.ant.ant(antfile: build, dir: project.projectDir, target: target, inheritAll: true)
        }
        finally {
            project.restoreProperties.restorePropertiesFiles()
            deleteJarsFromDir(libsDir)
            removeBuildScripts()
        }
    }

    def copyJarsToDir(String libsDir) {

        project.configurations['compile'].files.each { file ->
            logger.info("Found compile dependency: " + file.name + " " + file.path)

            def lib = new File("$libsDir/$file.name");
            libs.add(lib)
            lib.delete()

            if (! file.name.startsWith('android')) {
                logger.info("Copying $file.name to $libsDir/$file.name")
                ant.copy(todir: "$libsDir", file: "$file.path", overwrite: "true")
            }
        }
    }

    def deleteJarsFromDir(String dir) {
        libs.each { lib ->
            lib.delete()
        }
    }

    def clean() {
        def build = getBuildScripts()

        if (new SdkHelper(project).getToolsRevision() < 20) {
            logger.error("""
        =======================================================================
            Please, update SDK Tools upto 20 version at least to procced.
        =======================================================================
            """);

            throw new GradleScriptException("SDK Tools are outdated.")
        }
        else {
            project.logger.info("Calling 'ant clean'")
            project.ant.ant(antfile: build, dir: project.projectDir, target: "clean", inheritAll: true)
        }

        removeBuildScripts()
    }

    def removeBuildScripts() {
        new File("$project.projectDir/" + BUILD_FILENAME).delete()
        new File("$project.projectDir/" + SDK_BUILD_FILENAME).delete()
    }

    def getBuildScripts() {

        getSdkBuildScript();

        if (SdkHelper.isNdkBuild(project)) {
            return getBuildScript("build_with_ndk.xml")
        }

        return getBuildScript("build.xml")
    }

    def getSdkBuildScript() {

        String version = new SdkHelper(project).getToolsRevisionString();

        return getSdkBuildScript("build_sdk-" + version + ".xml")
    }

    def getBuildScript(String build) {

        logger.info("Get build script: " + build)

        InputStream stream = getClass().getClassLoader().getResourceAsStream(build)

        new File("$project.projectDir/" + BUILD_FILENAME).withWriter {
            it << stream.getText("UTF-8")
        }

        String buildScript = "$project.projectDir/" + BUILD_FILENAME

        if (project.properties.containsKey('ant.build.xml')) {
            buildScript = project.properties['ant.build.xml']
        }

        project.logger.lifecycle("BUILD.XML: " + buildScript)

        return buildScript
    }

    def getSdkBuildScript(String build) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(build)

        new File("$project.projectDir/" + SDK_BUILD_FILENAME).withWriter {
            it << stream.getText("UTF-8")
        }

        String buildScript = "$project.projectDir/" + SDK_BUILD_FILENAME

        if (project.properties.containsKey('sdk.build.xml')) {
            buildScript = project.properties['sdk.build.xml']
        }

        project.logger.lifecycle("SDK_BUILD.XML: " + buildScript)

        return buildScript
    }
}