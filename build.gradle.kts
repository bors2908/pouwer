import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test

plugins {
}

group = "ge.becrin.pouwer"

allprojects {
    group = rootProject.group
    version = rootProject.version
}

subprojects {
    tasks.withType<Copy>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
