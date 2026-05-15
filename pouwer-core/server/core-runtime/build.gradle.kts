plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.jib)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.noarg)
}

val dockerRepoName = providers.gradleProperty("pouwerDockerRepoName").get()

dependencies {
    implementation(project(":pouwer-core:server:common"))
    implementation(project(":pouwer-core:server:core-api"))
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.spring.cloud.bom))
    implementation(libs.spring.cloud.starter.openfeign)
    implementation(libs.pf4j)
    implementation(libs.kotlin.stdlib)

    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(project(":pouwer-core:server:common-test"))
}

tasks.register<Exec>("npmBuildBrowser") {
    workingDir = rootProject.projectDir
    commandLine = if (System.getProperty("os.name").lowercase().contains("windows")) {
        listOf("cmd", "/c", "npm ci && npm run build")
    } else {
        listOf("sh", "-c", "npm ci && npm run build")
    }
}

jib {
    val projectName = "pouwer-server"
    val nexusUser = providers.gradleProperty("nexusUser").orNull
    val nexusPass = providers.gradleProperty("nexusPass").orNull

    setAllowInsecureRegistries(true)
    from {
        image = "eclipse-temurin:21-jre"
    }
    to {
        image = "$dockerRepoName/$projectName:${project.version}"
        auth {
            username = nexusUser
            password = nexusPass
        }
    }
    container.apply {
        mainClass = "ge.becrin.pouwer.ApplicationKt"
        ports = listOf("8082")
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}
