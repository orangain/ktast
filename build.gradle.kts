plugins {
    id("com.palantir.git-version") version "3.0.0"
}

val gitVersion: groovy.lang.Closure<String> by extra
val kotlinVersion by extra { "1.7.0" }

allprojects {
    group = "com.github.orangain.ktast"
    version = gitVersion()

    repositories {
        mavenCentral()
    }
}
