import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    id("maven-publish")
}

dependencies {
    implementation(project(":pouwer-core:server:core-api"))
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.context)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.logging.jvm)
    compileOnly(libs.pf4j)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.register<Exec>("npmBuildBundle") {
    workingDir = rootProject.layout.projectDirectory.dir("pouwer-sha256-plugin/browser/traefik-sha256").asFile
    commandLine = if (System.getProperty("os.name").lowercase().contains("windows")) {
        listOf("cmd", "/c", "npm run build")
    } else {
        listOf("sh", "-c", "npm run build")
    }
}

tasks.register<Copy>("copyBundleDist") {
    dependsOn("npmBuildBundle")
    from(rootProject.layout.projectDirectory.dir("pouwer-sha256-plugin/browser/traefik-sha256/dist")) {
        into("static/traefik-sha256")
    }
    into(layout.buildDirectory.dir("resources/main"))
}

tasks.named("processResources") {
    finalizedBy("copyBundleDist")
}

tasks.named<Jar>("jar") {
    dependsOn("copyBundleDist")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
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
