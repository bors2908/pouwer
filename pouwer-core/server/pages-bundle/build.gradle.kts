plugins {
    `java`
    alias(libs.plugins.jib)
}

val dockerRepoName: String = providers.gradleProperty("repo.url.docker.hosted").get()
val pagesDir: File = rootProject.file("pouwer-core/browser/widget-traefik/pages")

val entrypointScript = layout.buildDirectory.file("entrypoint.sh")

val createEntrypoint = tasks.register("createEntrypoint") {
    outputs.file(entrypointScript)
    doLast {
        entrypointScript.get().asFile.writeText("""
            #!/bin/sh
            if [ -z "${'$'}PAGES_DESTINATION" ]; then
                echo "PAGES_DESTINATION environment variable is not set"
                exit 1
            fi
            mkdir -p "${'$'}PAGES_DESTINATION"
            cp -v /app/pages/* "${'$'}PAGES_DESTINATION/"
            echo "Pages copied to ${'$'}PAGES_DESTINATION"
        """.trimIndent())
    }
}

jib {
    val projectName = "pouwer-pages"
    val nexusUser = providers.gradleProperty("nexusUser").orNull
    val nexusPass = providers.gradleProperty("nexusPass").orNull

    setAllowInsecureRegistries(true)
    from {
        image = "busybox:1.36"
    }
    to {
        image = "$dockerRepoName/$projectName:${project.version}"
        auth {
            username = nexusUser
            password = nexusPass
        }
    }
    extraDirectories {
        paths {
            path {
                setFrom(pagesDir)
                into = "/app/pages"
            }
            path {
                setFrom(layout.buildDirectory.dir("entrypoint-dir"))
                into = "/app"
            }
        }
        permissions.set(mapOf("/app/entrypoint.sh" to "755"))
    }
    container {
        entrypoint = listOf("/bin/sh", "/app/entrypoint.sh")
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}

val prepareEntrypointDir = tasks.register<Copy>("prepareEntrypointDir") {
    dependsOn(createEntrypoint)
    from(entrypointScript)
    into(layout.buildDirectory.dir("entrypoint-dir"))
}

tasks.named("jib") {
    dependsOn(prepareEntrypointDir)
}

tasks.named("jibDockerBuild") {
    dependsOn(prepareEntrypointDir)
}
