package org.apache.activemq.artemis;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import javax.jms.*;
import java.util.UUID;

public class Publish {
    public static void main(String[] args) throws Exception {
        for(int count = 0; count<1000; count ++) {
            publishMessageAs(
                    "queue1",
                    "ippul",
                    "Pa$$w0rd",
                    "Test JMS Message " + UUID.randomUUID().toString()
            );
            Thread.sleep(1000l);
        }
    }
    public static void publishMessageAs(final String destinationName, final String username, final String password, final String messageBody) throws JMSException {
        final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://ex-aao-hdls-svc:61616", username, password);
        final Connection connection = connectionFactory.createConnection();
        connection.start();
        final Session session  = connection.createSession();
        final Destination destination = session.createQueue(destinationName);
        final MessageProducer producer = session.createProducer(destination);
        final TextMessage message = session.createTextMessage(messageBody);
        System.out.println("Sending: " + messageBody);
        producer.send(message);
        session.close();
        connection.close();
    }
}
