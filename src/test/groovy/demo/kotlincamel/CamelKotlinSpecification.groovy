package demo.kotlincamel

import demo.kotlincamel.utils.TestingUtils
import org.apache.activemq.command.ActiveMQBytesMessage
import org.apache.activemq.command.ActiveMQMessage
import org.apache.commons.io.FileUtils
import org.junit.Assume
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.jms.core.BrowserCallback
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.context.TestPropertySource
import spark.utils.StringUtils
import spock.lang.Specification

import javax.jms.JMSException
import javax.jms.Message
import javax.jms.ObjectMessage
import javax.jms.QueueBrowser
import javax.jms.Session
import javax.jms.TextMessage
import javax.xml.soap.Text
import java.nio.charset.Charset

import static com.xlson.groovycsv.CsvParser.parseCsv

import static microsimulator.Helpers.onClassPath
import static microsimulator.MicroSimulator.simulator
import static org.hamcrest.core.IsEqual.equalTo

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:demo/kotlincamel/order-test.properties")
class CamelKotlinSpecification extends Specification {


    @Autowired
    private JmsTemplate jmsTemplate

    @Autowired
    protected TestRestTemplate restTemplate;

    def "Silly Test"() {
        setup:
        def simulator = simulator(9870)
                .loadSimulations(onClassPath("demo/kotlincamel/simulations"))
        Assume.assumeThat(simulator.simulationCount(), equalTo(6))
        TestingUtils.createTempDirectories("orders")
        def inputFiles = TestingUtils.listFilesOnClasspath("demo/kotlincamel/orders")
        def jmsServer = JmsTestServer.builder()
                .port(7898)
                .launchJmsServer()
        when:
        List<ResponseEntity> responses = new ArrayList<>()
        for (inputFile in inputFiles) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-type", "application/json");
            responses.add(this.restTemplate.exchange("/order", HttpMethod.POST,
                    new HttpEntity(FileUtils.readFileToString(inputFile, Charset.defaultCharset()), headers), String.class))
        }
        then: "Check the HTTP Response"
        responses.size() == 2
        responses.each { ResponseEntity re -> re.statusCodeValue == 201 }
        and: "make sure the CSV summary is well formed"
        def reportFiles = FileUtils.listFiles(TestingUtils.fromTempDirectory("orders"), null, false)
        reportFiles.size() == 1
        def csv = parseCsv(readFirstLine: true,
                columnNames: ['orderId', 'lineCount', 'orderTotal'], FileUtils.readFileToString(reportFiles[0], Charset.defaultCharset()))
        def firstLine = csv.next()
        def secondLine = csv.next()
        firstLine.orderId == "999991"
        firstLine.lineCount == "4"
        firstLine.orderTotal == "1000"
        secondLine.orderId == "111111"
        secondLine.lineCount == "2"
        secondLine.orderTotal == "300"
        and: "JMS Queue has the 2 orders"
        Collection<TextMessage> messages = TestingUtils.readTextMessagesFrom(jmsTemplate, "orders")
        StringUtils.isNotEmpty(messages[0].getText())
        StringUtils.isNotEmpty(messages[1].getText())
        cleanup:
        simulator.shutdown()
        TestingUtils.deleteTempDirectories("orders")
        jmsServer.stop()
    }
}
