// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("com.google.devtools.ksp") version "2.1.20-2.0.0" apply false // Add this line
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
    kotlin("plugin.serialization") version "1.9.23"
}
