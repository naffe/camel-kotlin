package demo.kotlincamel

import org.apache.activemq.broker.BrokerService

/**
 * Manages a embedded JMS server for testing.
 */
class JmsTestServer {
    private int port

    private JmsTestServer() {
    }

    static JmsTestServer builder() {
        new JmsTestServer()
    }

    JmsTestServer port(int port) {
        this.port = port
        this
    }

    BrokerService launchJmsServer() {
        BrokerService broker = new BrokerService()
        broker.setPersistent(false)
        broker.setUseJmx(false)
        // configure the broker
        broker.addConnector("tcp://127.0.0.1:$port")
        broker.start();
        broker

    }


}
