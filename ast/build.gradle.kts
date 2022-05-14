plugins {
    kotlin("multiplatform") version "1.6.21"
}

val kotlinVersion: String by rootProject.extra

kotlin {
    jvm()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion")
            }
        }
    }
}
