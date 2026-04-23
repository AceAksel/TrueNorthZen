// Root build.gradle.kts with plugins configuration
plugins {
    id("com.android.application") version "7.0.0" apply false
    id("kotlin-android") version "1.5.31" apply false
}

// Add repositories
repositories {
    google()
    mavenCentral()
}