import cl.franciscosolis.sonatypecentralupload.SonatypeCentralUploadTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    kotlin("multiplatform") version "2.1.10"
    id("org.jetbrains.dokka") version "2.0.0"
    id("cl.franciscosolis.sonatype-central-upload") version "1.0.3"
}

val kotlinVersion by extra { "2.1.10" }

kotlin {
    jvm {
        tasks.withType<KotlinCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_1_8)
            }
        }
    }
}

allprojects {
    group = "io.github.orangain.ktast"
    version = "0.9.4"

    repositories {
        mavenCentral()
    }
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            pom {
                name.set(project.name)
                description.set("Ktast is a simple library to manipulate Kotlin source code as a set of AST objects.")
                url.set("https://github.com/orangain/ktast")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/orangain/ktast/blob/main/LICENSE")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("orangain")
                        name.set("Kota Kato")
                        email.set("orangain@gmail.com")
                    }
                }
                scm {
                    url.set("https://github.com/orangain/ktast")
                }
            }
        }
    }

    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

tasks.named<SonatypeCentralUploadTask>("sonatypeCentralUpload") {
    dependsOn("jar", "sourcesJar", "javadocJar", "generatePomFileForMavenPublication")

    username = System.getenv("MAVEN_CENTRAL_USERNAME")  // This is your Sonatype generated username
    password = System.getenv("MAVEN_CENTRAL_PASSWORD")  // This is your sonatype generated password

    // This is a list of files to upload. Ideally you would point to your jar file, source and javadoc jar (required by central)
    archives = files(
        tasks.named("jar"),
        tasks.named("sourcesJar"),
        tasks.named("javadocJar"),
    )
    // This is the pom file to upload. This is required by central
    pom = file(
        tasks.named("generatePomFileForMavenPublication").get().outputs.files.single()
    )

    signingKey = System.getenv("PGP_SIGNING_KEY")  // This is your PGP private key. This is required to sign your files
    signingKeyPassphrase =
        System.getenv("PGP_SIGNING_KEY_PASSPHRASE")  // This is your PGP private key passphrase (optional) to decrypt your private key
}
