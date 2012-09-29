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
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.TaskAction
import ru.yandex.gradle.apklib.SdkHelper

/**
 * Created by Vladimir Grachev (vgrachev@)
 * Date: 31.05.12
 * Time: 13:51
 */
class SetupTask extends DefaultTask {

    @TaskAction
    def setup() {
        setupSdkDir()
        setupNdkDir()
        setupVersion()
        setupLibrary()

        setAntProps()
    }

    def setupVersion() {
        project.ext['version.code'] = getIntVersion(project.version)
        project.ext['version.name'] = getStringVersion(project.version)
    }

    def getIntVersion(String version) {
        def result = version.replaceAll('\\.', '').find(/\d+/)
        if (result == null) return "123"
        while (result.length() < 3) result += "0"
        return result
    }

    def getStringVersion(String version) {
        def result = version.replace("-SNAPSHOT", "")
        if (result == null) return "1.23"
        return result
    }

    def getReleaseVersion(String version, String buildNumber) {
        return version.replace("-SNAPSHOT", "-$buildNumber");
    }

    def setAntProps() {
        project.ant.properties['ant.project.name'] = project.name
        project.ant.properties['version.code'] = project['version.code']
        project.ant.properties['version.name'] = project['version.name']
        if (project.properties.containsKey('dpi') && project.properties['dpi'] != 'all') {
            project.ant.properties['aapt.resource.filter'] = project.properties['dpi']
        }
    }

    def setupLibrary() {
        def props = new Properties();

        def files = [new File("$project.projectDir/ant.properties"), new File("$project.projectDir/project.properties")]

        String result = 'false'

        files.each { file ->
            try {
                file.withInputStream { props.load(it) }

                logger.debug("Reading file: $file.name")

                if (props.stringPropertyNames().contains("android.library")) {
                    logger.debug("Props contain android.library")
                    if (props["android.library"] == 'true') {
                        result = 'true'
                    }
                }
                else {
                    logger.debug("Props don't contain android.library")
                }
            }
            catch (FileNotFoundException) {
                logger.warn("No $project.projectDir/$file.name file found.")
            }
        }
        project.ext['android.library'] = result
    }

    def setupSdkDir() {
        if (project.properties.containsKey('sdk.dir')) {
            logger.info("Setting up 'sdk.dir' from project properties: " + project.properties['sdk.dir'])
            project.ant.properties['sdk.dir'] = project.properties['sdk.dir']
        }
        else if (System.getenv().containsKey("ANDROID_SDK_HOME")) {
            logger.info("Setting up 'sdk.dir' from environment: " + System.getenv().get("ANDROID_SDK_HOME"))
            project.ant.properties['sdk.dir'] = System.getenv().get("ANDROID_SDK_HOME")
            project.ext['sdk.dir'] = System.getenv().get("ANDROID_SDK_HOME")
        }
        else if (System.getenv().containsKey("ANDROID_HOME")) {
            logger.info("Setting up 'sdk.dir' from environment: " + System.getenv().get("ANDROID_HOME"))
            project.ant.properties['sdk.dir'] = System.getenv().get("ANDROID_HOME")
            project.ext['sdk.dir'] = System.getenv().get("ANDROID_HOME")
        }
        logger.lifecycle("SDK DIR: " + project.properties['sdk.dir'])

        if (!project.properties.containsKey('sdk.dir')) {
            logger.error("""
    =======================================================================
        Please, setup ANDROID_SDK_HOME environment variable to procced.
    =======================================================================
            """);

            throw new GradleScriptException("ANDROID_SDK_HOME is undefined.")
        }
    }

    def setupNdkDir() {
        if (project.properties.containsKey('ndk.dir')) {
            logger.info("Setting up 'ndk.dir' from project properties: " + project.properties['ndk.dir'])
            project.ant.properties['ndk.dir'] = project.properties['ndk.dir']
        }
        else if (System.getenv().containsKey("ANDROID_NDK_HOME")) {
            logger.info("Setting up 'ndk.dir' from environment: " + System.getenv().get("ANDROID_NDK_HOME"))
            project.ant.properties['ndk.dir'] = System.getenv().get("ANDROID_NDK_HOME")
            project.ext['ndk.dir'] = System.getenv().get("ANDROID_NDK_HOME")
        }
        else if (System.getenv().containsKey("NDK_HOME")) {
            logger.info("Setting up 'ndk.dir' from environment: " + System.getenv().get("NDK_HOME"))
            project.ant.properties['ndk.dir'] = System.getenv().get("NDK_HOME")
            project.ext['ndk.dir'] = System.getenv().get("NDK_HOME")
        }
        logger.lifecycle("NDK DIR: " + project.properties['ndk.dir'])

        if (!project.properties.containsKey('ndk.dir') && SdkHelper.isNdkBuild(project)) {
            logger.error("""
    =======================================================================
        Please, setup ANDROID_NDK_HOME environment variable to procced.
    =======================================================================
            """);

            throw new GradleScriptException("ANDROID_NDK_HOME is undefined.")
        }
    }

    def releaseVersion() {
        project.version = getReleaseVersion(project.version, project['build.number'])
        logger.info("Project version: $project.version")
    }
}
