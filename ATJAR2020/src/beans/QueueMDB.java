package beans;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import implementation.RestHostBuilder;
import models.ACLMessage;
import models.AID;
import models.Agent;
import services.HostService;
import ws.WSEndPoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.ActivationConfigProperty;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/queue/mojQueue")
})
public class QueueMDB implements MessageListener{
	
	@EJB
	HostManagerBean hostManagerBean;
	
	@EJB WSEndPoint ws;
	
	@Override
	public void onMessage(Message msg) {
		//TextMessage tmsg = (TextMessage) msg;
		System.out.println("[INFO] [HANDLE ACL MSG] Received ACL message in QueueMDB");
		try {
			ACLMessage receivedMessage = (ACLMessage) ((ObjectMessage) msg).getObject();
			AID[] receivers = receivedMessage.getReceivers();
			Set<String> ipAddresses = new HashSet<String>();
			
			System.out.println("[INFO] [HANDLE ACL MSG] Forwarding messages to agents on this host");
			for (AID a: receivers) {
				if(a.getHost().getIpAddress().equals(this.hostManagerBean.getCurrentSlaveHost().getIpAddress())) {
					Agent at = HostService.findAgentWithAID(this.hostManagerBean.getRunningAgents().get(this.hostManagerBean.getCurrentSlaveHost().getIpAddress()), a);
					at.handleMessage(receivedMessage);
				} else {
					ipAddresses.add(a.getHost().getIpAddress());
				}
			}
			
			System.out.println("[INFO] [HANDLE ACL MSG] Forwarding messages to agents on other hosts");
			for(String receivingHostIp: ipAddresses) {
				System.out.println("[INFO] [HANDLE ACL MSG] Forwarding message to {" + receivingHostIp + "}");
				RestHostBuilder.sendACLMessageBuilder(this.hostManagerBean.getCurrentSlaveHost(), receivingHostIp, receivedMessage);
			}
			
			//TODO Update socket
			//TODO kad se sve poruke posalju, potrebno je azurirati spisak ACL poruka
			
			System.out.println("[INFO] [HANDLE ACL MSG] FINISHED");
			//ws.echoTextMessage(tmsg.getText());
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
