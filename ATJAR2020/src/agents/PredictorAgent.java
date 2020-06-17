package agents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Stateful;

import beans.ACLMessageBuilder;
import enumerations.Performative;
import models.ACLMessage;
import models.AID;
import models.Agent;

public class PredictorAgent extends Agent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Map<String, Integer> startedPredictions = new HashMap<>();
	
	public PredictorAgent() {
		super();
	}
	
	@Override
	public void handleMessage(ACLMessage message) {
		System.out.println("[INFO] [PREDICTOR AGENT] [" + this.getAid().getName() + "] Handling of the message");
		if (message.getPerformative() == Performative.STARTPREDICTING) {
			System.out.println("[INFO] [PREDICTOR AGENT] [" + this.getAid().getName() + "] Received data " + message.getContent());
			if(startedPredictions.get(message.getConversationID()) == null) {
				startedPredictions.put(message.getConversationID(), 1);
			} else {
				startedPredictions.put(message.getConversationID(), startedPredictions.get(message.getConversationID()) + 1);
			}
			
			if ((int) startedPredictions.get(message.getConversationID()) != message.getNumberOfPastReceivers()) {
				System.out.println("[INFO] [PREDICTOR AGENT] [" + this.getAid().getName() + "] Waiting for other finder agents to return data");
				return;
			} else {
				System.out.println("[INFO] [PREDICTOR AGENT] [" + this.getAid().getName() + "] Start predicting data");
				String predicted = "PREDICTED";
				System.out.println("[INFO] [PREDICTOR AGENT] [" + this.getAid().getName() + "] Sending data to master agent on original host");
				ACLMessage newACLMessage = new ACLMessage(message, message.getSender());
				newACLMessage.setContent(predicted);
				newACLMessage.setPerformative(Performative.SHOWPREDICTION);
				ACLMessageBuilder.sendACLMessage(newACLMessage);
			}
		}
	}

}
