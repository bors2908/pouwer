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
    implementation("org.springframework:spring-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.bitcoinj:bitcoinj-core:0.16.3")
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
