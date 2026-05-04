import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.springframework.boot")
    id("org.springdoc.openapi-gradle-plugin")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.allopen") apply true
    kotlin("plugin.noarg") apply true
}

group = "ru.itmo"

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.1")
    }
}

dependencies {
    implementation(project(":common"))

    implementation("org.mockito.kotlin:mockito-kotlin:6.2.3")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

    runtimeOnly("com.h2database:h2")
}

openApi {
    outputDir.set(project.layout.projectDirectory)
    apiDocsUrl.set("http://localhost:8089/v3/api-docs.yaml")
    outputFileName.set("${project.name}.yml")
    waitTimeInSeconds.set(15)
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
