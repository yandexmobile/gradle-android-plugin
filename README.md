gradle-android-plugin
============================

Плагин позволяет собрать Android Library Project в application library, а так же собрать зависимый от этой библиотеки проект.

Для сборки и выгрузки плагина в локальный репозиторий запустите ./gradlew build uploadArchives

Взгляните на пример сборки библиотеки и зависимого приложения тут:
https://github.com/yandexmobile/gradle-android-plugin-usage-example

Чтобы использовать плагин нужно:
1. Импортировать его в build.gradle

buildscript {
    repositories {
        ivy{name "local"; url 'file://' + new File(System.getProperty('user.home'), '.yandex/ivy-repo').absolutePath}
        mavenCentral()
    }
    dependencies {
        classpath "ru.yandex.gradle.android:gradle-android-plugin:1.00"
    }
}

apply plugin: 'apklib'

2. При вызове сборки заменить исполняемый файл с ant на gradlew, например

Было: ant clean release
Стало: ./gradlew clean release
