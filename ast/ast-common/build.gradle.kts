plugins {
    kotlin("multiplatform") version "1.3.21"
}

val kotlinVersion: String by rootProject.extra

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion")
    testCompile("org.jetbrains.kotlin:kotlin-test-annotations-common:$kotlinVersion")
    testCompile("org.jetbrains.kotlin:kotlin-test-common:$kotlinVersion")
}