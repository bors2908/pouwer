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
}

group = "ru.itmo"

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
    implementation(project(":common"))
    implementation(project(":core-api"))
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    kapt("org.mapstruct:mapstruct-processor:1.6.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(project(":common-test"))

    if (bundledPayloadsEnabled.get()) {
        runtimeOnly(project(":payloads:payload-sha256"))
        runtimeOnly(project(":payloads:payload-bitcoin-rpc"))
        runtimeOnly(project(":payloads:payload-monero-randomx"))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
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

jib {
    val projectName = "pouw-backend"
    val dockerRepoName = "localhost:9002"
    val nexusUser = findProperty("nexusUser") as String?
    val nexusPass = findProperty("nexusPass") as String?

    setAllowInsecureRegistries(true)
    from {
        image = "eclipse-temurin:25-jre"
    }
    to {
        image = "$dockerRepoName/$projectName:${project.version}"
        auth {
            username = nexusUser
            password = nexusPass
        }
    }
    container.apply {
        mainClass = "ru.itmo.enterprise.ApplicationKt"
        ports = listOf("8082")
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}
