package agents;

import javax.ejb.EJB;

import beans.ACLMessageBuilder;
import beans.HostManagerBean;
import enumerations.Performative;
import models.ACLMessage;
import models.AID;
import models.Agent;

public class FinderAgent extends Agent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FinderAgent() {
		super();
	}
	
	@Override
	public void handleMessage(ACLMessage message) {
		System.out.println("[INFO] [FINDER AGENT] [" + this.getAid().getName() + "] Handling of the message");
		if (message.getPerformative() == Performative.STARTCOLLECTINGDATA) {
			System.out.println("[INFO] [FINDER AGENT] [" + this.getAid().getName() + "] Start collecting data");
			String json = "COLLECTED DATA";
			System.out.println("[INFO] [FINDER AGENT] [" + this.getAid().getName() + "] Sending data to predictor agent on master");
			ACLMessage newACLMessage = new ACLMessage(message, message.getReplyTo());
			newACLMessage.setContent(json);
			newACLMessage.setPerformative(Performative.STARTPREDICTING);
			ACLMessageBuilder.sendACLMessage(newACLMessage);
			System.out.println("[INFO] [FINDER AGENT] [" + this.getAid().getName() + "] Message sent");
		}
	}
}
