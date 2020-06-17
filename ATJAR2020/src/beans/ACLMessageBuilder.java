package beans;

import java.io.Serializable;
import java.util.UUID;

import javax.ejb.EJB;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import models.ACLMessage;

public class ACLMessageBuilder {
	
	public static void sendACLMessage(ACLMessage newACLMessage) {
		try {
			Context context = new InitialContext();
			ConnectionFactory cf = (ConnectionFactory) context.lookup("java:jboss/exported/jms/RemoteConnectionFactory");
			final Queue queue = (Queue) context.lookup("java:jboss/exported/jms/queue/mojQueue");
			context.close();
			Connection connection = cf.createConnection("guest", "guest.guest.1");
			final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			connection.start();
			ObjectMessage tmsg = session.createObjectMessage((Serializable) newACLMessage);
			MessageProducer producer = session.createProducer(queue);
			producer.setTimeToLive(12000);
			producer.send(tmsg);
			producer.close();
			connection.stop();
			connection.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
