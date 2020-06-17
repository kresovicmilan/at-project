package agents;

import java.util.HashMap;
import java.util.Map;

import beans.ACLMessageBuilder;
import enumerations.Performative;
import models.ACLMessage;
import models.Agent;

public class MasterAgent extends Agent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//conversationId + username
	private Map<String, String> requestedPredictions = new HashMap<>();

	public MasterAgent() {
		super();
	}
	
	@Override
	public void handleMessage(ACLMessage message) {
		System.out.println("[INFO] [MASTER AGENT] [" + this.getAid().getName() + "] Handling of the message");
		if (message.getPerformative() == Performative.SHOWPREDICTION) {
			System.out.println("[INFO] [MASTER AGENT] [" + this.getAid().getName() + "] Received data " + message.getContent());
			if(this.requestedPredictions.get(message.getConversationID()) != null) {
				System.out.println("[INFO] [MASTER AGENT] [" + this.getAid().getName() + "] User found just save the data");
				//TODO Update sockets
				//TODO Update User
			}
		}
	}
	
	public void addRequestedPredictions(String conversationId, String username) {
		this.requestedPredictions.put(conversationId, username);
	}
}
