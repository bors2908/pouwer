plugins {
    java
}

group = "ru.itmo"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

subprojects {
    repositories {
        mavenCentral()
        mavenLocal()
    }

    tasks.withType<Copy>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}
