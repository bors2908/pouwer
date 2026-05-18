plugins {
    `java-library`
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.noarg)
    id("ge.becrin.pouwer.maven-shadow-publish")
}

dependencies {
    api(platform(libs.spring.boot.bom))
    api(libs.spring.boot.starter)
    api(libs.jackson.databind)
    api(libs.jackson.module.kotlin)
    api(libs.kotlin.stdlib.jdk8)
}
