package models;

public interface AgentInterface {
	void init(AID aid);
	void stop();
	void handleMessage(ACLMessage message);
	void setAid(AID aid);
	AID getAid();
}
