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

import org.gradle.api.Project

/**
 * Created by Vladimir Grachev (vgrachev@)
 * Date: 14.09.12
 * Time: 20:45
 */
class SdkHelper {

    private static final String SOURCE_PROPERTIES_FILE = 'source.properties'
    private static final String PKG_REVISION_PROPERTY = 'Pkg.Revision'

    public static final int TOOLS_REVISION_UNKNOWN = Integer.MAX_VALUE;
    public static final int TOOLS_REVISION_UNDEFINED = 0;

    private project
    private int toolsRevision = TOOLS_REVISION_UNDEFINED

    SdkHelper(project) {
        this.project = project
    }

    int getToolsRevision() {
        if (toolsRevision == TOOLS_REVISION_UNDEFINED) {
            setupToolsRevision()
        }

        return toolsRevision
    }

    void setupToolsRevision() {
        def ant = project.ant
        def toolsDir = new File(ant['sdk.dir'], 'tools')
        assert toolsDir.exists()
        def sourcePropertiesFile = new File(toolsDir, SOURCE_PROPERTIES_FILE)
        assert sourcePropertiesFile.exists()
        ant.property(file: sourcePropertiesFile)
        String revision = ant[PKG_REVISION_PROPERTY];

        def result = revision.find(/\d+/)
        if (result == null) {
            toolsRevision = TOOLS_REVISION_UNKNOWN
        }
        else {
            try {
                toolsRevision = Integer.parseInt(result)
            }
            catch (Throwable e) {
                toolsRevision = TOOLS_REVISION_UNKNOWN
            }
        }

        if (toolsRevision == TOOLS_REVISION_UNKNOWN) {
            project.logger.info("Android SDK Tools Revision UNKNOWN")
        }
        else {
            project.logger.info("Android SDK Tools Revision " + toolsRevision)
        }
    }

    static def isNdkBuild(Project project) {
        if (project.properties.containsKey('skip.ndk.build') && "true".equals(project['skip.ndk.build'])) {
            return false
        }

        if (project.properties.containsKey('ndk.build') && "true".equals(project['ndk.build'])) {
            return true
        }

        return false
    }
}
