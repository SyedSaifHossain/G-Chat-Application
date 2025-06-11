// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false


}


buildscript {
    dependencies {

        classpath("com.android.tools.build:gradle:8.10.0") // Keep existing Android Gradle Plugin
        classpath("com.android.tools.build:gradle:8.10.1") // Keep existing Android Gradle Plugin
        classpath("com.google.gms:google-services:4.3.10") // Firebase Google Services Plugin
        classpath ("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.4")
    }
}