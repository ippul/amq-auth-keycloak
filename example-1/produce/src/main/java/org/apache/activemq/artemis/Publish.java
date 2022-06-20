package org.apache.activemq.artemis;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import javax.jms.*;
import java.util.UUID;

public class Publish {
    public static void main(String[] args) throws Exception {
        final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://ex-aao-hdls-svc:61616", "ippul", "Pa$$w0rd");
        final Connection connection = connectionFactory.createConnection();
        connection.start();
        final Session session  = connection.createSession();
        final Destination destination = session.createQueue("queue1");
        final MessageProducer producer = session.createProducer(destination);
        for(int count = 0; count<10_000_000; count ++) {
            final TextMessage message = session.createTextMessage("Test JMS Message " + UUID.randomUUID().toString());
            System.out.println("Sending: " + message);
            producer.send(message);
            Thread.sleep(10l);
        }
        session.close();
        connection.close();
    }
}
