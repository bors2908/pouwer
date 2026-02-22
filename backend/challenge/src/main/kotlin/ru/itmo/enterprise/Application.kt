package ru.itmo.enterprise

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients(basePackages = ["ru.itmo.enterprise"])
@ConfigurationPropertiesScan(basePackages = ["ru.itmo.enterprise"])
open class Application

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}
