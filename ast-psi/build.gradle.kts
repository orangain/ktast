import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    kotlin("jvm") version "1.3.21"
}

val kotlinVersion: String by rootProject.extra

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compile(project(":ast:ast-jvm"))
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
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