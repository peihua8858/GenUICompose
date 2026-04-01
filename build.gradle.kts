// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
    configurations.configureEach {
        resolutionStrategy.force(
//            "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.3.2",
//            "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.3.2"
        )
    }
}
