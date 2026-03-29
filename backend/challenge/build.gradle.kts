import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.allopen") apply true
    kotlin("plugin.noarg") apply true
}

group = "ru.itmo"
version = "0.0.1-SNAPSHOT"

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.1")
    }
}

dependencies {
    implementation(project(":common"))
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.bitcoinj:bitcoinj-core:0.16.3")
    implementation("ge.becrin.kt.stratum:kt-stratum:1.0.0-SNAPSHOT")
    implementation("org.json:json:20251224")

    kapt("org.mapstruct:mapstruct-processor:1.6.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(project(":common-test"))
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
