package ge.becrin.pouwer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

class MavenShadowPublishConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("maven-publish")

        project.plugins.withId("com.gradleup.shadow") {
            project.extensions.getByType(PublishingExtension::class.java).apply {
                repositories {
                    maven {
                        url = project.uri(project.providers.gradleProperty("repo.url.maven.hosted").get())
                        credentials {
                            username = project.providers.gradleProperty("nexusUser").orNull
                            password = project.providers.gradleProperty("nexusPass").orNull
                        }
                        isAllowInsecureProtocol = true
                    }
                }

                publications {
                    create("mavenJava", MavenPublication::class.java) {
                        artifact(project.tasks.named("shadowJar"))
                        artifactId = project.name
                    }
                }
            }
        }
    }
}
