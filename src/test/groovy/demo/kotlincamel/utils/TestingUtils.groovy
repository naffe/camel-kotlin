package demo.kotlincamel.utils

import groovy.text.SimpleTemplateEngine
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.springframework.jms.core.BrowserCallback
import org.springframework.jms.core.JmsTemplate

import javax.jms.JMSException
import javax.jms.QueueBrowser
import javax.jms.Session
import javax.jms.TextMessage
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * Collection of utils for testing domain.
 */
class TestingUtils {

    static def createTempDirectories(String... directories) {
        directories.each {
            File directory = new File(System.getProperty("java.io.tmpdir"), it)
            directory.mkdirs()
        }
    }


    static def deleteTempDirectories(String... directories) {
        directories.each {
            File directory = new File(System.getProperty("java.io.tmpdir"), it)
            directory.exists()
            FileUtils.deleteQuietly(directory)
        }
    }

    static Collection<File> listFilesOnClasspath(String classpathLocation) {
        FileUtils.listFiles(new File(TestingUtils.class.getClassLoader()
                .getResource(classpathLocation).getFile().replace("%20", " ")), null, false)

    }


    static File fromTempDirectory(String child) {
        new File(System.getProperty("java.io.tmpdir"), child)
    }

    static File fromUserDirectory(String child) {
        new File(System.getProperty("user.dir"), child)
    }

    static Collection<TextMessage> readTextMessagesFrom(JmsTemplate jmsTemplate, String queueName) {
        jmsTemplate.browse(queueName, new BrowserCallback<Collection<TextMessage>>() {
            @Override
            Collection<TextMessage> doInJms(Session session, QueueBrowser browser) throws JMSException {
                return Collections.list(browser.getEnumeration())
            }
        })
    }
}


