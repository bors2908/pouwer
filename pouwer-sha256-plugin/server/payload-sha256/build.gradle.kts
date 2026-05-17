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
    compileOnly(libs.pf4j)
}

npmBundleSource {
    sourceDir.set(rootProject.file("pouwer-sha256-plugin/browser/traefik-sha256"))
}

npmBundleResources {
    targetPath.set("static/traefik-sha256")
}

tasks.processResources {
    filesMatching("plugin.properties") {
        expand(mapOf("version" to project.version.toString()))
    }
}
