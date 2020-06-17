package implementation;

import java.util.Collection;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import DTO.HandshakeDTO;
import models.ACLMessage;
import models.Agent;
import models.AgentType;
import models.ForeignMessage;
import models.Host;
import models.UpdatePackage;
import javax.ws.rs.core.Response;

public class RestHostBuilder {
	
	public static void registerNodeBuilder(Host currentSlaveHost, Host masterHost) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + masterHost.getIpAddress() + "/ATWAR2020/rest/host");
		RestAPI rest = target.proxy(RestAPI.class);
		rest.registerNode(currentSlaveHost);
	}
	
	public static void sendNewHostToHostBuilder(String receivingHostIp, Host newHost) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + receivingHostIp + "/ATWAR2020/rest/host");
		RestAPI rest = target.proxy(RestAPI.class);
		rest.sendNewHostToHost(newHost);
	}
	
	public static Collection<Host> sendHostsToNewHostBuilder(Host currentSlaveHost, Host masterHost) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + masterHost.getIpAddress() + "/ATWAR2020/rest/host");
		RestAPI rest = target.proxy(RestAPI.class);
		return rest.sendHostsToNewHost(currentSlaveHost);
	}
	
	public static UpdatePackage sendAllLoggedInUsersToNodeBuilder(Host sender, Host receiver, UpdatePackage updatePackage, int handshake) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + receiver.getIpAddress() + "/ATWAR2020/rest/host");
		RestAPI rest = target.proxy(RestAPI.class);

		HandshakeDTO handshakeDTO = new HandshakeDTO();
    	handshakeDTO.setSender(sender);
    	handshakeDTO.setUpdatePackage(updatePackage);
    	handshakeDTO.setHandshake(handshake);
    	
		return rest.sendAllLoggedInUsersToNode(handshakeDTO);
	}
	
	public static void deleteHostBuilder(Host receiver, Host deletedHost) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + receiver.getIpAddress() + "/ATWAR2020/rest/host");
		RestAPI rest = target.proxy(RestAPI.class);
		rest.deleteHost(deletedHost.getAlias());
	}
	
	public static int sendMessageBuilder(ForeignMessage foreignMessage) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + foreignMessage.getIpReceivingHost() + "/ATWAR2020/rest/host");
		RestAPI rest = target.proxy(RestAPI.class);
		int succ = rest.sendMessage(foreignMessage);
		return succ;
	}
	
	public static int checkIfAliveBuilder(Host checkedHost) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + checkedHost.getIpAddress() + "/ATWAR2020/rest/host");
		RestAPI rest = target.proxy(RestAPI.class);
		int succ = rest.checkIfAlive();
		return succ;
	}
	
	public static void deleteFromSpecificHostBuilder(Host receiver, Host deletedHost) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + receiver.getIpAddress() + "/ATWAR2020/rest/host");
		RestAPI rest = target.proxy(RestAPI.class);
		rest.deleteFromSpecificHost(deletedHost.getAlias());
	}
	
	public static void sendAgentTypesToSpecificHostBuilder(Host currentSlaveHost, Host receiver, Collection<AgentType> agentTypes) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + receiver.getIpAddress() + "/ATWAR2020/rest/host");
		RestAPI rest = target.proxy(RestAPI.class);
		rest.sendAgentTypesToSpecificHost(currentSlaveHost, agentTypes);
	}
	
	public static Collection<AgentType> getAgentTypesFromMasterBuilder(Host masterHost) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + masterHost.getIpAddress() + "/ATWAR2020/rest/host");
		RestAPI rest = target.proxy(RestAPI.class);
		return rest.getAgentTypesFromMaster();
	}
	
	public static Collection<Agent> sendingRunningAgentsToNodeBuilder(Host sendingHost, Host receivingHost, Collection<Agent> receivingRunningAgents) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + receivingHost.getIpAddress() + "/ATWAR2020/rest/host");
		RestAPI rest = target.proxy(RestAPI.class);
		return rest.sendingRunningAgentsToNode(sendingHost, receivingRunningAgents);
	}
	
	public static void sendACLMessageBuilder(Host sendingHost, String receivingHostIp, ACLMessage newACLMessage) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + receivingHostIp + "/ATWAR2020/rest/host");
		RestAPI rest = target.proxy(RestAPI.class);
		rest.sendACLMessage(sendingHost, newACLMessage);
	}
	
	public static void stopAgentBuilder(String receivingHostIp, String agentName) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + receivingHostIp + "/ATWAR2020/rest");
		RestAPI rest = target.proxy(RestAPI.class);
		rest.stopAgent(agentName);
	}
	
	public static Response startAgentBuilder(String receivingHostIp, String agentType, String agentName) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + receivingHostIp + "/ATWAR2020/rest");
		RestAPI rest = target.proxy(RestAPI.class);
		return rest.startAgent(agentType, agentName);
	}
	
	
	/*public static void getAgentTypesBuilder(Host currentSlaveHost, Host masterHost) {
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target("http://" + masterHost.getIpAddress() + "/WAR2020/rest/host");
		RestAPI rest = target.proxy(RestAPI.class);
		rest.getAgentTypes();
	}*/
}