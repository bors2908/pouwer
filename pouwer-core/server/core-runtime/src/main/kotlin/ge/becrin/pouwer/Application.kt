package ge.becrin.pouwer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients(basePackages = ["ge.becrin.pouwer"])
@ConfigurationPropertiesScan(basePackages = ["ge.becrin.pouwer"])
open class Application
{
    @Bean
    fun logVersion(@Value($$"${spring.application.version}") version: String): ApplicationRunner {
        return ApplicationRunner {
            log.info { "core-runtime version $version" }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

private val log = KotlinLogging.logger {}
