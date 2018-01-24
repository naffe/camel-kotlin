@file:Suppress("UNCHECKED_CAST")

package demo.kotlincamel

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.camel.Exchange
import org.apache.camel.Handler
import org.apache.camel.LoggingLevel
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.http4.HttpMethods.GET
import org.apache.camel.model.dataformat.BindyType
import org.apache.camel.processor.aggregate.AggregationStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 *  Kotlin Route Builder
 */

@Component
class OrderAllocationRoute @Autowired constructor(var kotlinAwareObjectMapper: ObjectMapper) : RouteBuilder() {

    override fun configure() {

        from("direct:create-order-route")
                .bean("setupOrder")
                .split(simple("\${body.lines}"), OrderLineAggregationStrategy()).parallelProcessing()
                .setHeader("currentOrderLine", simple("\${body}"))
                .to("direct:line.stock.price.check")
                .end()
                .setHeader("orderLines", simple("\${body}"))
                .to("direct:next-free-order-number")
                .bean("buildOrder")
                .multicast()
                .to("direct:write-audit-summary-to-file", "direct:publish-onto-jms-queue")

        from("direct:next-free-order-number")
                .description("REST WS Call to obtain the next, free order number")
                .setHeader(Exchange.HTTP_METHOD, constant(GET))
                .setHeader("sku", simple("\${body}"))
                .setBody(constant(null))
                .to("{{next.free.order.number.ws.endpoint}}")
                .convertBodyTo(String::class.java)
                .setHeader("orderNumber", simple("\${body}"))

        from("direct:line.stock.price.check")
                .description("REST WS Call to check current stock pricing")
                .setHeader(Exchange.HTTP_METHOD, constant(GET))
                .setHeader("sku", simple("\${body.sku}"))
                .setBody(constant(null))
                .toD("{{stock.price.check.ws.endpoint}}/\${header.sku}")

        from("direct:write-audit-summary-to-file")
                .description("Write a summary line to todays csv summary file")
                .bean("convertOrderToOrderSummary")
                .marshal().bindy(BindyType.Csv, OrderSummaryLine::class.java)
                .to("file://{{order.output.folder}}?fileExist=Append&fileName=orders-on-\${date:now:yyyyMMdd}.csv")

        from("direct:publish-onto-jms-queue")
                .bean("orderToJsonMessage")
                .to("jms:{{jms.order.queue.name}}")
    }


}

@Component
open class ConvertOrderToOrderSummary {

    fun convertToOrderSummary(exchange: Exchange): OrderSummaryLine {
        val order = exchange.getIn().getBody(Order::class.java)
        return OrderSummaryLine(order.orderId, order.lines.size, order.lines.sumByDouble(OrderLine::price))
    }
}

@Component
class SetupOrder {
    fun setupOrder(exchange: Exchange) {
        exchange.setProperty("newOrderRequest", exchange.`in`.body)
    }
}

@Component
class BuildOrder {

    fun buildOrder(exchange: Exchange) {
        val orderRequest = exchange.getProperty("newOrderRequest", NewOrderRequest::class.java)
        val orderId = exchange.`in`.getHeader("orderNumber", Int::class.java)
        val lines = exchange.`in`.getHeader("orderLines") as Collection<OrderLine>
        exchange.`in`.body = Order(orderId, orderRequest.customerId,
                lines)
    }
}

@Component
class OrderToJsonMessage @Autowired constructor(var kotlinAwareObjectMapper: ObjectMapper) {

    fun convertOrderToJson(exchange: Exchange) {
        exchange.getIn().setBody(kotlinAwareObjectMapper.writeValueAsString(exchange.getIn().getBody()))
    }
}

class OrderLineAggregationStrategy : AggregationStrategy {

    override fun aggregate(oldExchange: Exchange?, newExchange: Exchange): Exchange? {
        val orderLineRequest = newExchange.`in`.getHeader("currentOrderLine", NewOrderLineRequest::class.java)
        val price = newExchange.`in`.getBody(String::class.java)
        println()
        if (oldExchange == null) {
            val prices = ArrayList<OrderLine>()
            prices.add(OrderLine(orderLineRequest.lineId, orderLineRequest.sku, orderLineRequest.qty, price.toDouble()))
            newExchange.`in`.body = prices
            return newExchange
        } else {
            val skuPrices = oldExchange.`in`.body as ArrayList<OrderLine>
            skuPrices.add(OrderLine(orderLineRequest.lineId, orderLineRequest.sku, orderLineRequest.qty, price.toDouble()))
        }
        return oldExchange
    }
}