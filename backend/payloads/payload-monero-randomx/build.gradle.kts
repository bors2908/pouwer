import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    id("io.spring.dependency-management")
}

group = "ru.itmo"

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.3")
    }
}

dependencies {
    implementation(project(":core-api"))
    implementation("org.springframework:spring-context")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.01")
    implementation("org.slf4j:slf4j-api")
    implementation("ge.becrin:kt-stratum:0.1.0")
    implementation("org.json:json:20251224")
    compileOnly("org.pf4j:pf4j:3.13.0")
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
