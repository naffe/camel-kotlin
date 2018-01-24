package demo.kotlincamel

import org.apache.camel.dataformat.bindy.annotation.CsvRecord
import org.apache.camel.dataformat.bindy.annotation.DataField

data class NewOrderRequest(val customerId: String, val lines: Collection<NewOrderLineRequest>);
data class NewOrderLineRequest(val lineId: Int, val sku: String, val qty: Double)

data class Order(val orderId: Int, val customerId: String, val lines: Collection<OrderLine>)
data class OrderLine(val lineId: Int, val sku: String, val qty: Double, val price: Double)


@CsvRecord(separator = ",")
class OrderSummaryLine(@field:DataField(pos = 1)
                       val orderId: Int,
                       @field:DataField(pos = 2)
                       val lineCount: Int,
                       @field:DataField(pos = 3)
                       val orderTotal: Double)