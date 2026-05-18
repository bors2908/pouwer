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
include("pouwer-core:server:plugin-base")
include("pouwer-core:server:core-runtime")
include("pouwer-core:server:common-test")

include("pouwer-sha256-plugin:server")
include("pouwer-bitcoin-plugin:server")
include("pouwer-monero-plugin:server")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri(providers.gradleProperty("repo.url.maven.public").get())
            isAllowInsecureProtocol = true
        }
        mavenCentral()
    }
}
