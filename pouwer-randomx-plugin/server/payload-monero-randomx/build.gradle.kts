plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    id("ge.becrin.pouwer.maven-shadow-publish")
    id("ge.becrin.pouwer.npm-bundle")
}

dependencies {
    implementation(project(":pouwer-core:server:core-api"))
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.context)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.slf4j.api)
    implementation(libs.kt.stratum)
    implementation(libs.json)
    compileOnly(libs.pf4j)
}

npmBundleSource {
    sourceDir.set(rootProject.file("pouwer-randomx-plugin/browser/traefik-randomx"))
}

npmBundleResources {
    targetPath.set("static/traefik-randomx")
}

tasks.processResources {
    filesMatching("plugin.properties") {
        expand(mapOf("version" to project.version.toString()))
    }
}
