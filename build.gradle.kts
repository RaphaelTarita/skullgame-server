import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion = "2.0.0"
val ktorVersion = "2.3.11"
val logbackVersion = "1.5.6"
val kotlinxDatetimeVersion = "0.6.0"
val skullgameCommonVersion = "1.0.3-SNAPSHOT"

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("io.ktor.plugin") version "2.3.11"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

group = "com.rtarita"
version = "1.0.0-SNAPSHOT"

application {
    mainClass.set("com.rtarita.skull.server.ApplicationKt")
}

repositories {
    mavenCentral()
    mavenLocal() // needed for 'com.rtarita.skull:skullgame-common' dependency
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-forwarded-header-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")

    // in order to use this dependency, the skullgame-common repository needs to be checked out first and installed into the maven local repository.
    // to do so, invoke 'gradlew(.bat) publishToMavenLocal' in the skullgame-common project
    implementation("com.rtarita.skull:skullgame-common:$skullgameCommonVersion")
}

tasks.withType<KotlinCompile>().all {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

detekt {
    basePath = rootDir.toString()
}
