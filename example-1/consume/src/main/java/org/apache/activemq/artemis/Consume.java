package org.apache.activemq.artemis;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import javax.jms.*;

public class Consume {

    public static void main(String[] args) throws Exception {
        for(int count = 0; count<1000; count ++) {
            consumeMessageAs(
                    "queue1",
                    "ippul",
                    "Pa$$w0rd"
            );
            Thread.sleep(1000l);
        }
    }

    public static void consumeMessageAs(final String destinationName, final String username, final String password) throws JMSException {
        final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://ex-aao-hdls-svc:61616", username, password);
        final Connection connection = connectionFactory.createConnection();
        connection.start();
        final Session session  = connection.createSession();
        final Destination destination = session.createQueue(destinationName);
        final MessageConsumer consumer = session.createConsumer(destination);
        final Message message = consumer.receive(10000);
        String messageBody = message.getBody(String.class);
        message.acknowledge();
        session.close();
        connection.close();
        System.out.println("Message body: " + messageBody);
    }
}
