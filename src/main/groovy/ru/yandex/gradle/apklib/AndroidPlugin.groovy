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

package ru.yandex.gradle.apklib

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import ru.yandex.gradle.apklib.tasks.*

/**
 * Created by Vladimir Grachev (vgrachev@)
 * Date: 30.05.12
 * Time: 15:43
 */
public class AndroidPlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin.class)

        project.configurations.add('apklib')

        project.task('prebuild', type: PrebuildTask)
        project.task('setup', type: SetupTask)
        project.task('apklib', type: ApkLibTask)
        project.task('preprocess', type: PreprocessTask)
        project.task('antrun', type: AntTask)

        project.prebuild.dependsOn(project.setup)
        project.preprocess.dependsOn(project.prebuild)
        project.clean.dependsOn(project.setup)
        project.build.dependsOn(project.prebuild)
        project.jar.dependsOn(project.prebuild)
        project.apklib.dependsOn(project.prebuild)

        project.apklib.onlyIf{ project.properties['android.library'] == 'true' }

        project.clean.doFirst() {
            project.antrun.clean()
        }

        project.task("ndk") << {
            if (project.properties.containsKey('skip.ndk.build') &&
                    "true".equals(project['skip.ndk.build'])) {
                project.logger.info("SKIP ndk build.")
            }
            else {
                project.logger.info("Turn on ndk build.")
                project.ext['ndk.build'] = 'true'
            }
        }

        [
            "help",
            "debug",
            "release",
            "instrument",
            "emma",
            "installd",
            "installr",
            "installi",
            "installt",
            "uninstall"
        ].each {
            antTask ->
            project.task("$antTask", type: AntTask)
            project.tasks["$antTask"].dependsOn 'prebuild'
        }

        project.task("android-install", type: AntTask)
        project.tasks["android-install"].target = "install"
        project.tasks["android-install"].dependsOn 'prebuild'

        project.task("android-test", type: AntTask)
        project.tasks["android-test"].target = "test"
        project.tasks["android-test"].dependsOn 'prebuild'

        [
            "android-install",
            "android-test",
            "installd",
            "installr",
            "installi",
            "installt",
            "uninstall"
        ].each {
            antTask ->
            project.tasks["$antTask"].onlyIf{ project.properties['android.library'] != 'true' }
        }

        [
            "android-install",
            "android-test",
            "debug",
            "release",
            "instrument",
            "installd",
            "installr",
            "installi",
            "installt",
            "uninstall"
        ].each {
            antTask ->
            project.tasks["$antTask"].dependsOn 'preprocess'
        }

        [
            "debug",
            "release",
        ].each {
            antTask ->
            project.tasks["$antTask"].doLast{
                project.tasks["jar"].dependsOn "$antTask"
                project.tasks["jar"].from {"bin/classes"}
                if (project.properties.containsKey(ApkLibTask.KEY_ANDROID_LIBRARY) &&
                    "true" == project.properties[ApkLibTask.KEY_ANDROID_LIBRARY]) {
                    project.apklib.create()
                }
            }
        }
    }
}
