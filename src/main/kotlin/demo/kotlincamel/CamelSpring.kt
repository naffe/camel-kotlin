
package demo.kotlincamel

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.camel.ProducerTemplate
import org.apache.camel.impl.DefaultExchange
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
open class CamelMain

fun main(args: Array<String>) {
    SpringApplication.run(CamelMain::class.java, *args)
}

@RestController
class OrderController(@Autowired val producerTemplate: ProducerTemplate) {

    @PostMapping("/order", consumes = arrayOf("application/json"))
    fun createOrder(@RequestBody newOrderRequest: NewOrderRequest): String {
        val syncResponseExchange = DefaultExchange(producerTemplate.camelContext)
        syncResponseExchange.getIn().setBody(newOrderRequest)
        producerTemplate.send("direct:create-order-route", syncResponseExchange)
        if (syncResponseExchange.getProperty("CamelExceptionCaught") != null)
            return ""

        println(newOrderRequest)
        return syncResponseExchange.`in`.getBody(String::class.java);
    }

}

@Configuration
open class Configuration {
    @Bean
    open fun objectMapper(): ObjectMapper? {
        return ObjectMapper().registerModule(KotlinModule())
    }
}
