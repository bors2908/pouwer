plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    id("maven-publish")
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

npmBundle {
    sourceDir.set(rootProject.file("pouwer-bitcoin-plugin/browser/traefik-bitcoin"))
    targetPath.set("static/traefik-bitcoin")
}

tasks.processResources {
    filesMatching("plugin.properties") {
        expand(mapOf("version" to project.version.toString()))
    }
}

publishing {
    repositories {
        maven {
            url = uri(providers.gradleProperty("repo.url.maven.hosted").get())
            credentials {
                username = providers.gradleProperty("nexusUser").orNull
                password = providers.gradleProperty("nexusPass").orNull
            }
            isAllowInsecureProtocol = true
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.named("shadowJar"))
            artifactId = project.name
        }
    }
}
