import com.google.protobuf.gradle.id

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.noarg)
    alias(libs.plugins.shadow)
    alias(libs.plugins.protobuf)
    id("ge.becrin.pouwer.maven-shadow-publish")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.32.1"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.76.0"
        }
    }
    generateProtoTasks {
        all().configureEach {
            plugins {
                id("grpc")
            }
        }
    }
}

tasks.processResources {
    filesMatching("contract.properties") {
        expand(mapOf("version" to project.version.toString()))
    }
}

dependencies {
    api(project(":pouwer-core:server:model"))
    api(platform(libs.spring.boot.bom))
    api(libs.jackson.databind)
    api(libs.spring.boot.starter.security)
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.actuator)
    api(libs.commons.lang3)
    api(libs.jackson.module.kotlin)
    api(libs.kotlin.reflect)

    api(libs.spring.boot.starter.validation)

    api(libs.swagger.annotations)
    api(libs.kotlin.logging.jvm)
    api(libs.kotlin.stdlib.jdk8)
    api(libs.protobuf.java)
    api(libs.protobuf.java.util)
    api(libs.grpc.stub)
    api(libs.grpc.protobuf)
    compileOnly(libs.javax.annotation.api)
}
