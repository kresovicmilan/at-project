package models;

import java.io.Serializable;

import javax.ejb.Remove;
import javax.ejb.Singleton;
import javax.ejb.Stateful;

@Stateful
public class Agent implements AgentInterface, Serializable{
	private static final long serialVersionUID = 1L;
	
	private AID aid;
	
	public Agent() {
		super();
	}

	public Agent(AID aid) {
		super();
		this.aid = aid;
	}
	
	public AID getAid() {
		return aid;
	}
	
	public void setAid(AID aid) {
		this.aid = aid;
	}

	@Override
	public void init(AID aid) {
		// TODO Auto-generated method stub
		this.aid = aid;
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		System.out.println("Agent has stopped");
	}

	@Override
	public void handleMessage(ACLMessage message) {
		// TODO Auto-generated method stub
		System.out.println("Agent is handling messages");
	}
	
	@Remove(retainIfException=false)
	public void remove() {
		System.out.println("[INFO] [STOP AGENT] Agent has been removed: " + this.aid.getName());
	}
	
	
}
