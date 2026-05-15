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
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.logging.jvm)
    compileOnly(libs.pf4j)
}

npmBundle {
    sourceDir.set(rootProject.file("pouwer-sha256-plugin/browser/traefik-sha256"))
    targetPath.set("static/traefik-sha256")
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
