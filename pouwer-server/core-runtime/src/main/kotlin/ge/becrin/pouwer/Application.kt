package ge.becrin.pouwer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients(basePackages = ["ge.becrin.pouwer"])
@ConfigurationPropertiesScan(basePackages = ["ge.becrin.pouwer"])
open class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
