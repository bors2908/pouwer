plugins {
    java
    kotlin("jvm") version "2.3.10" apply false
    kotlin("kapt") version "2.3.10" apply false
    kotlin("plugin.allopen") version "2.3.10" apply false
    kotlin("plugin.noarg") version "2.3.10" apply false
    id("org.springframework.boot") version "4.0.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "ru.itmo"

repositories {
    maven {
        url = uri("http://localhost:9001/repository/maven-public/")
        isAllowInsecureProtocol = true
    }
    mavenCentral()
}

subprojects {
    repositories {
        maven {
            url = uri("http://localhost:9001/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
        mavenCentral()
    }

    tasks.withType<Copy>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}
