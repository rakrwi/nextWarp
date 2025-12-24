plugins {
    kotlin("jvm") version "2.0.21"
    id("com.gradleup.shadow") version "8.3.5"
}

allprojects {
    group = "dev.rakrwi"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":api"))
    implementation(project(":paper"))
    implementation(project(":velocity"))
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("NextWarp")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.build {
    dependsOn(tasks.shadowJar)
}