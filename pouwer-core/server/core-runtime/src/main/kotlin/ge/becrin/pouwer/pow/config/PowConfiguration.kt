package ge.becrin.pouwer.pow.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ge.becrin.pouwer.pow.service.TaskStore
import ge.becrin.pouwer.pow.service.store.InMemoryTaskStore

@Configuration
open class PowConfiguration {

    @Bean
    open fun taskStore(): TaskStore = InMemoryTaskStore()
}
