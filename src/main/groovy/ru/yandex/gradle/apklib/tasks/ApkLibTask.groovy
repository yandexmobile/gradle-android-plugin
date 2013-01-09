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

    String mPostfix = null
    String versionPostfix = null

    final static String KEY_ANDROID_LIBRARY = "ant.android.library"
    
    final static String DEFAULT_VALUE_PROGUARD = "proguard-project.txt"
    
    final static String ANDROID_MK = "Android.mk"

    def defineVariables() {
        if (baseName == null) baseName = project.archivesBaseName
        if (version == null) version = project.version
        
        versionPostfix = version + mPostfix
    }

    @TaskAction
    def create() {

        logger.info("APKLIB 'apklib' task." + project.properties[KEY_ANDROID_LIBRARY] ) 
        
        if (project.properties[KEY_ANDROID_LIBRARY] != 'true') {
            logger.info("We are not library. Skipping 'apklib' task.")
            return
        }


        def libs = copyCompileDependenciesToDir("$project.projectDir/libs")

        def postfixes = getPostfixes("$project.projectDir/libs")

        try{

            postfixes.each { postfix ->

                mPostfix = ""
                if (!postfix.equals("")){
                    mPostfix = "-"+postfix
                    if (postfix.equals("all")){
                        postfix = ""
                    }
                }
                extension_new = extension + mPostfix

                defineVariables()

                logger.info("Creating application library: $project.buildDir/$baseName-$versionPostfix.$extension_new")

                def ant = new AntBuilder()
                ant.copy(todir: "$project.buildDir/$extension_new") {
                    fileset(dir : "$project.projectDir") {
                        include(name: "assets/**")
                        include(name: "bin/**")
                        include(name: "res/**")
                        include(name: "src/**")
                        include(name: "libs/*.*")
                        include(name: "libs/"+postfix+"/**")
                        include(name: "project.properties")
                        include(name: "proguard-project.txt")
                        include(name: "AndroidManifest.xml")
                    }
                }
                
                if (project.properties.containsKey('ndk.build') && "true".equals(project['ndk.build'])) {
                    logger.info("Copy jni file")
                    copyJniToDir("$project.buildDir/$extension_new")
                }
                    

                logger.info("Creating application library  proguard-android.txt: $project.projectDir")
                
                InputStream stream = getClass().getClassLoader().getResourceAsStream("empty_build.xml")
                new File("$project.buildDir/$extension_new/build.xml").withWriter {it << stream.getText("UTF-8")}

                ant.zip(destfile: destWithSrcFile(), basedir: "$project.buildDir/$extension_new")
                ant.zip(destfile: destNoLibsFile(), basedir: "$project.buildDir/$extension_new") {
                    exclude(name: "src/**")
                    exclude(name: "libs/**")
                }
                ant.zip(destfile: destFile(), basedir: "$project.buildDir/$extension_new", excludes: "src/**")
                ant.zip(destfile: destResFile(), basedir: "$project.buildDir/$extension_new") {
                    exclude(name: "src/**")
                    exclude(name: "libs/**")
                    exclude(name: "bin/classes.jar")
                }

                if (new File("$project.projectDir/bin/proguard/obfuscated.jar").exists()) {

                    ant.copy(todir: "$project.buildDir/obfuscated-$extension_new") {
                        fileset(dir : "$project.projectDir") {
                            include(name: "assets/**")
                            include(name: "bin/res/**")
                            include(name: "res/**")
                            include(name: "project.properties")
                            include(name: "AndroidManifest.xml")
                            include(name: "proguard-android.txt")
                        }
                    }

                    ant.zip(destfile: "$project.buildDir/obfuscated-$extension_new/bin/classes.jar") {
                        zipfileset(src: "$project.projectDir/bin/proguard/obfuscated.jar") {
                            exclude(name: '**/R.class')
                            exclude(name: '**/R$*.class')
                        }
                    }

                    ant.copy(todir: "$project.buildDir/obfuscated-$extension_new") {
                        fileset(dir : "$project.buildDir/$extension_new") {
                            include(name: "build.xml")
                        }
                    }

                    ant.zip(destfile: destObfuscatedFile(), basedir: "$project.buildDir/obfuscated-$extension_new")

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
        }finally {
            deleteCompileDependenciesToDir(libs, "$project.projectDir/libs")
        }
    }

    public File destObfuscatedFile() {
        defineVariables()
        return new File("$project.buildDir/$baseName-$versionPostfix-obfuscated.$extension")
    }

    public File destWithSrcFile() {
        defineVariables()
        return new File("$project.buildDir/$baseName-$versionPostfix-wsrc.$extension")
    }

    public File destNoLibsFile() {
        defineVariables()
        return new File("$project.buildDir/$baseName-$versionPostfix-nolibs.$extension")
    }

    public File destFile() {
        defineVariables()
        return new File("$project.buildDir/$baseName-$versionPostfix.$extension")
    }

    public File destResFile() {
        defineVariables()
        return new File("$project.buildDir/$baseName-$versionPostfix-res.$extension")
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
            
            logger.info("Setting proguard.config: "+"$project.buildDir/deps/$file.name/"+ DEFAULT_VALUE_PROGUARD)
            if (  new File("$project.buildDir/deps/$file.name/"+ DEFAULT_VALUE_PROGUARD).exists()  &&  project.ant.properties.containsKey("proguard.config")){
                project.ant.properties["proguard.config"] = project.ant.properties["proguard.config"]+":"+"$project.buildDir/deps/$file.name/"+ DEFAULT_VALUE_PROGUARD
                logger.info("ADD new proguard.config"+project.ant.properties["proguard.config"] )
            }
            

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

    def getPostfixes(String libsDir) {
        def postfixes = new ArrayList<String>();
        postfixes.add("");
        postfixes.add("all");
        def file = new File(libsDir);
        file.list().each { name ->
            if ((new File(libsDir+File.separator+name)).isDirectory()){
                postfixes.addAll(name)
            }
        }
        return postfixes
    }
    
    
    def copyJniToDir(String apklibDir){
        
        def fileIn = new File ("$project.projectDir/jni/"+ANDROID_MK)
        logger.info("Copy copyJniToDir " + fileIn.exists() + "$project.projectDir/jni/"+ANDROID_MK)
        if (fileIn.exists()){

            def ant = new AntBuilder()
            ant.copy(todir: apklibDir) {
                fileset(dir : "$project.projectDir") {
                    include(name: "jni/**")
                    exclude(name: "jni/"+ANDROID_MK)
                }
            }
            
            def fileOut  = new File (apklibDir+"/jni/"+ANDROID_MK)
            
            def path  = null
//            def isInclude = false;
            
            fileIn.eachLine{ line ->
                if (line.find(/LOCAL_PATH :=/) != null){
                    path = line.replaceAll(/LOCAL_PATH := \$\(call my-dir\)/,'').trim()
                    if (path.startsWith("/..")){
                        path = path.replaceFirst(/\/../,'')
                    }
                    fileOut.leftShift('LOCAL_PATH := $(call my-dir)/\n')
                }else{
                    fileOut.leftShift(line+"\n")

//                    if (path != null && (line.find(/ALL_INCLUDES :=/) != null || isInclude)){
//                        isInclude  = true;
//                        def pathInclude = line.replaceAll('ALL_INCLUDES :=','')
//                        pathInclude = pathInclude.replaceAll(/\$\(LOCAL_PATH\)/,'').trim();
//                        if (pathInclude.size() > 0){
//                            pathInclude = pathInclude.replaceAll("\\\\","").trim();
//                            if (line.find(/build\/deps/) != null){
//                                logger.info("ERROR No copy header files path = $project.projectDir" + (path+pathInclude)+" pathInclude contains  /build/deps pathInclude = "+pathInclude)
//                            }else if (pathInclude.startsWith("/..")){
//                                logger.info("ERROR No copy header files path = $project.projectDir" + (path+pathInclude)+" pathInclude start with /.. pathInclude = "+pathInclude)
//                            }else if (!(new File("$project.projectDir" + (path+pathInclude))).exists() ){
//                                logger.info("ERROR No copy header files path = $project.projectDir" + (path+pathInclude)+" does not exist" )
//                            }else{
//                                logger.info("copy header file path = $project.projectDir" + (path+pathInclude))
//                                if (pathInclude.size() > 0){
//                                    ant.copy(todir: apklibDir+"/jni"+pathInclude) {
//                                        fileset(dir : "$project.projectDir" + (path+pathInclude)) {
//                                            include(name: "**/*.h")
//                                            include(name: "**/*.hpp")
//                                            include(name: "**/*.am")
//                                        }
//                                    }
//                                }else{
//                                    ant.copy(todir: apklibDir+"/jni") {
//                                        fileset(dir : path) {
//                                            include(name: "*.h")
//                                            include(name: "*.hpp")
//                                            include(name: "*.am")
//                                        }
//                                    }
//                                }
//                            }
//                        }else{
//                            isInclude = false;
//                        }
//                    }
                }
                
            }
            logger.info("Copy copyJniToDir " + apklibDir+"/jni/"+ "  path  = "+path)
            if (path != null  && (new File("$project.projectDir" + path)).exists() ){
                ant.copy(todir: apklibDir+"/jni/") {
                    fileset(dir :  "$project.projectDir" + path) {
                        exclude(name: "**/build/**")
                        include(name: "**/*.h")
                        include(name: "**/*.hpp")
                    }
                }
            } else {
               logger.info("ERROR No copy header files path = $project.projectDir" + path)
            }
        }
    } 
    
}
