import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.google.cloud.tools.jib") version "3.4.5"
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.allopen") apply true
    kotlin("plugin.noarg") apply true
    kotlin("plugin.spring")
}

group = "ge.becrin.pouwer"

val bundledPayloadsEnabled = providers
    .gradleProperty("challenge.bundledPayloads")
    .map(String::toBooleanStrictOrNull)
    .orElse(false)

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.1")
    }
}

dependencies {
    implementation(project(":pouwer-core:server:common"))
    implementation(project(":pouwer-core:server:core-api"))
    implementation("org.pf4j:pf4j:3.14.1")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    kapt("org.mapstruct:mapstruct-processor:1.6.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(project(":pouwer-core:server:common-test"))

    if (bundledPayloadsEnabled.get()) {
        runtimeOnly(project(":pouwer-sha256-plugin:server:payload-sha256"))
        runtimeOnly(project(":pouwer-bitcoin-plugin:server:payload-bitcoin-rpc"))
        runtimeOnly(project(":pouwer-randomx-plugin:server:payload-monero-randomx"))
    }
    implementation(kotlin("stdlib"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

noArg {
    annotation("jakarta.persistence.Entity")
}

tasks.test {
    useJUnitPlatform()
}

// Global browser build (optional, if core-runtime needs its own static assets or shared libs)
tasks.register<Exec>("npmBuildBrowser") {
    val browserDir = file("${rootProject.projectDir}")
    workingDir = browserDir
    commandLine = if (System.getProperty("os.name").lowercase().contains("windows")) {
        listOf("cmd", "/c", "npm ci && npm run build")
    } else {
        listOf("sh", "-c", "npm ci && npm run build")
    }
}

jib {
    val projectName = "pouwer-server"
    val dockerRepoName = "localhost:9002"
    val nexusUser = findProperty("nexusUser") as String?
    val nexusPass = findProperty("nexusPass") as String?

    setAllowInsecureRegistries(true)
    from {
        image = "eclipse-temurin:21-jre"
    }
    to {
        image = "$dockerRepoName/$projectName:${project.version}"
        auth {
            username = nexusUser
            password = nexusPass
        }
    }
    container.apply {
        mainClass = "ge.becrin.pouwer.ApplicationKt"
        ports = listOf("8082")
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}
repositories {
    mavenCentral()
}
