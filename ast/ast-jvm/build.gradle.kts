import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    kotlin("multiplatform") version "1.3.21"
}

val kotlinVersion: String by rootProject.extra

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvm()
}

dependencies {
    expectedBy(project(":ast:ast-common"))
    compile("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    testCompile("junit:junit:4.12")
    testCompile("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testCompile("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}

tasks {
    test {
        testLogging {
            outputs.upToDateWhen { false }
            showStandardStreams = true
            exceptionFormat =  FULL
            events = setOf(PASSED, SKIPPED, FAILED)
        }
    }
}