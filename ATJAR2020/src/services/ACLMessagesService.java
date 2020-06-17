package services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jms.ConnectionFactory;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.websocket.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;

import agents.MasterAgent;
import beans.HostManagerBean;
import beans.StorageBean;
import enumerations.Performative;
import model.SocketMessage;
import models.ACLMessage;
import models.AID;
import models.Agent;
import models.AgentType;
import ws.WSEndPoint;

@Stateless
@Path("/messages")
@LocalBean
public class ACLMessagesService {
	
	@EJB
	HostManagerBean hostManagerBean;
	
	@EJB
	StorageBean storageBean;
	
	@EJB
	WSEndPoint ws;
	
	@Resource(mappedName = "java:/ConnectionFactory")
	private ConnectionFactory connectionFactory;
	
	@Resource(mappedName = "java:jboss/exported/jms/queue/mojQueue")
	private Queue queue;
	
	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<String> getAllACLMessages() {
		List<String> allPerformatives = new ArrayList<String>();
		for(Enum<Performative> e : Performative.values()) {
			allPerformatives.add(e.toString());
		}
		return allPerformatives;
		//return this.hostManagerBean.getACLmessages().values();	
	}
	
	@POST
	@Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
	public Response sendACLMessage(ACLMessage newACLMessage) {
		try {
			this.hostManagerBean.getACLmessages().put(UUID.randomUUID(), newACLMessage);
			if (newACLMessage.getPerformative() == Performative.STARTCOLLECTINGDATA) {
				newACLMessage = additionalSetUpOfACLMessage(newACLMessage);
			}
			QueueConnection connection = (QueueConnection) connectionFactory.createConnection("guest", "guest.guest.1");
			QueueSession session = connection.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
			QueueSender sender = session.createSender(queue);
			
	 		updateSockets("agentmessage", new Gson().toJson("[INFO] [" + newACLMessage.getSender().getName() + "] Sent message with performative: " + newACLMessage.getPerformative().toString()));
			
			ObjectMessage message = session.createObjectMessage(newACLMessage);
			sender.send(message);
			return Response.status(200).entity("Message has been sent").build();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return Response.status(400).entity("Message has not been sent").build();
	}
	
	public ACLMessage additionalSetUpOfACLMessage(ACLMessage aclMessage) {
		Agent masterAgent = HostService.findAgentWithAID(this.hostManagerBean.getRunningAgents().get(this.hostManagerBean.getCurrentSlaveHost().getIpAddress()), aclMessage.getSender());
		if (masterAgent instanceof MasterAgent) {
			String uuid = (UUID.randomUUID()).toString();
			((MasterAgent) masterAgent).addRequestedPredictions(uuid, aclMessage.getSender().getName());
			aclMessage.setConversationID(uuid);
		}
		aclMessage.setNumberOfPastReceivers(aclMessage.getReceivers().length);
		return aclMessage;
	}
	
	public void updateSockets(String socketMessageType, String json) {
    	System.out.println("[INFO] Updating sockets");
    	List<String> usernames = new ArrayList<>(ws.getUserSessions().keySet());
    	for (List<String> listOfForeignLoggedInUsers: hostManagerBean.getForeignLoggedUsers().values()) {
    		usernames.addAll(listOfForeignLoggedInUsers);
    	}
		SocketMessage message = new SocketMessage(socketMessageType, new Date(), json);
		String jsonMessage = new Gson().toJson(message);
		try {
			for (Session s: ws.getSessions()) {
				s.getBasicRemote().sendText(jsonMessage);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

}
