val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.3.0"
    id("io.ktor.plugin") version "3.4.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
}

group = "work.slhaf"
version = "0.0.1"

application {
    mainClass = "work.slhaf.ApplicationKt"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform("dev.langchain4j:langchain4j-bom:1.12.1"))

    implementation("dev.langchain4j:langchain4j")
    implementation("dev.langchain4j:langchain4j-mcp")
    implementation("dev.langchain4j:langchain4j-open-ai")

    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-netty")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-sse")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("org.jsoup:jsoup:1.18.3")
}
