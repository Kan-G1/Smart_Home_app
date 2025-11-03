// Root-level build.gradle.kts

plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.4" apply false

}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
