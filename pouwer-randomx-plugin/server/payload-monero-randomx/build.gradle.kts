import ge.becrin.pouwer.NpmBundleExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    id("maven-publish")
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

extensions.configure<NpmBundleExtension>("npmBundle") {
    sourceDir.set(rootProject.layout.projectDirectory.dir("pouwer-randomx-plugin/browser/traefik-randomx"))
    targetPath.set("static/traefik-randomx")
}

publishing {
    repositories {
        maven {
            url = uri(providers.gradleProperty("pouwerMavenHostedRepoUrl").get())
            credentials {
                username = providers.gradleProperty("nexusUser").orNull
                password = providers.gradleProperty("nexusPass").orNull
            }
            isAllowInsecureProtocol = true
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = project.name
        }
    }
}
