import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    kotlin("jvm") version "1.6.21"
    `maven-publish`
}

val kotlinVersion: String by rootProject.extra

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":ast"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    testImplementation("junit:junit:4.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String
            from(components["kotlin"])
        }
    }
}
