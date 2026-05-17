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
    implementation(libs.spring.web)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.bitcoinj.core)
    compileOnly(libs.pf4j)
}

npmBundleSource {
    sourceDir.set(rootProject.file("pouwer-bitcoin-plugin/browser/traefik-bitcoin"))
}

npmBundleResources {
    targetPath.set("static/traefik-bitcoin")
}

tasks.processResources {
    filesMatching("plugin.properties") {
        expand(mapOf("version" to project.version.toString()))
    }
}
