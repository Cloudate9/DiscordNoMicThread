plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
}

group = "com.cloudate9.discordnomicthread"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.kord:kord-core:0.8.0-M17")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.slf4j:slf4j-simple:2.0.6")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "${rootProject.group}.DiscordBotKt"
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}