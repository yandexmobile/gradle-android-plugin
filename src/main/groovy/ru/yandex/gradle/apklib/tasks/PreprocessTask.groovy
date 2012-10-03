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
import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * Created by Vladimir Grachev (rook@)
 * Date: 03.10.12
 * Time: 13:03
 */
class PreprocessTask extends DefaultTask {

    def filterFile = new File("$project.projectDir" + "/properties/preprocess.txt")
    def filterPath = "res/**/preprocess.xml"
    def filterJavaPath = "**/Preprocess.java"

    @TaskAction
    def preprocess(){
        def props = new Properties();

        try {
            filterFile.withInputStream { props.load(it) }
        }
        catch(FileNotFoundException e) {
            logger.lifecycle("No file with preprocess values found")
            return
        }

        def ant = new AntBuilder()

        props['version.name'] = project.version
        props['build.date'] = getDateString("dd.MM.yyyy")
        props['version.app'] = Helper.getStringVersion(project.version)
        props['version.number'] = Helper.getIntVersion(project.version)
        props['version.name'] = Helper.getStringVersion(project.version)
        props['version.code'] = Helper.getIntVersion(project.version)
        props['build.date.year'] = getDateString("yyyy")
        props['build.date.month'] = getDateString("MM")
        props['build.date.dayMonth'] = getDateString("dd")

        if (project.properties.containsKey('teamcity')) {
            props['beta.features'] = 'false'

            def teamCity = project.properties['teamcity']

            teamCity.each {
                if (it.key.startsWith("preprocess.")) {
                    logger.info("Setting preprocess property: $it.key = $it.value")
                    def key = it.key.replace("preprocess.", "")
                    props[key] = it.value
                }
            }

            if (teamCity.containsKey('build.number')) {
                logger.info("Setting build.number = " + teamCity['build.number'])
                props['build.number'] = teamCity['build.number']
            }
        }
        else {
            props['beta.features'] = 'true'
        }

        try {
            ant.copy(todir: "$project.projectDir/bin", encoding: "UTF-8") {
                fileset(dir : "$project.projectDir") {
                    include(name: filterPath)
                }
                filterset(begintoken: '${', endtoken: '}') {
                    props.each {
                        filter(token: it.key, value: it.value)
                    }
                }
            }
        }
        catch (Throwable e) {

        }

        try {
            ant.copy(todir: "$project.projectDir/gen", encoding: "UTF-8") {
                fileset(dir : "$project.projectDir/preprocess") {
                    include(name: filterJavaPath)
                }
                filterset(begintoken: '${', endtoken: '}') {
                    props.each {
                        filter(token: it.key, value: it.value)
                    }
                }
            }
        }
        catch (Throwable e) {

        }
    }

    def getDateString(String format) {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Calendar cal = Calendar.getInstance();
        return dateFormat.format(cal.getTime());
    }
}

