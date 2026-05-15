import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "pouwer"

include("pouwer-core:server:common")
include("pouwer-core:server:model")
include("pouwer-core:server:core-api")
include("pouwer-core:server:core-runtime")
include("pouwer-core:server:common-test")

include("pouwer-sha256-plugin:server:payload-sha256")
include("pouwer-bitcoin-plugin:server:payload-bitcoin-rpc")
include("pouwer-randomx-plugin:server:payload-monero-randomx")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri(providers.gradleProperty("pouwerMavenPublicRepoUrl").get())
            isAllowInsecureProtocol = true
        }
        mavenCentral()
    }
}
