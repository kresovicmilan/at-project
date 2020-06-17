package services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.websocket.Session;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.google.gson.Gson;

import DTO.AgentSpecificDTO;
import DTO.HandshakeDTO;
import DTO.MessageDTO;
import beans.ACLMessageBuilder;
import beans.HostManagerBean;
import beans.StorageBean;
import implementation.RestHostBuilder;
import model.SocketMessage;
import models.ACLMessage;
import models.AID;
import models.Agent;
import models.AgentType;
import models.ForeignMessage;
import models.Host;
import models.UpdatePackage;
import models.User;
import ws.WSEndPoint;

@Stateless
@Remote(HostServiceRemote.class)
@Path("/host")
@LocalBean
public class HostService implements HostServiceRemote {
	
	@EJB
	StorageBean storageBean;
	
	@EJB
	HostManagerBean hostManagerBean;
	
	@EJB
	WSEndPoint ws;
	
	@Override
    public void registerNode(Host newHost) {
		System.out.println("[INFO] [MASTER] First step - Master recieved registration from: " + newHost.getIpAddress());
		
        if (!hostManagerBean.getHosts().containsKey(newHost.getIpAddress())) {
        	hostManagerBean.getHosts().put(newHost.getIpAddress(), newHost);
        	System.out.println("[INFO] [MASTER] First step - FINISHED");
        	
        	System.out.println("[INFO] [MASTER] Second step - Send new host to other hosts");
        	for (Host h: hostManagerBean.getHosts().values()) {
        		if ((!h.getIpAddress().equals(newHost.getIpAddress())) && (!h.getIpAddress().equals(hostManagerBean.getMasterHost().getIpAddress()))) {
        			RestHostBuilder.sendNewHostToHostBuilder(h.getIpAddress(), newHost);
        			System.out.println("[INFO] [MASTER] Second step - Sent to: " + h.getIpAddress());
        		}
        	}
        	
        	System.out.println("[INFO] [MASTER] Second step - FINISHED");
        }
    }


    @Override
    public void sendNewHostToHost(Host newHost) {
        if (!hostManagerBean.getHosts().containsKey(newHost.getIpAddress())) {
        	hostManagerBean.getHosts().put(newHost.getIpAddress(), newHost);
        	System.out.println("[INFO] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Received new host: " + newHost.getIpAddress());
        }
    }

    @Override
    public Collection<Host> sendHostsToNewHost(Host newHost) {
    	System.out.println("[INFO] [MASTER] Third step - Received request from host: " + newHost.getIpAddress());
    	List<Host> otherHosts = new ArrayList<Host>();
        for(Host h: hostManagerBean.getHosts().values()) {
        	if ((!h.getIpAddress().equals(newHost.getIpAddress())) && (!h.getIpAddress().equals(hostManagerBean.getMasterHost().getIpAddress()))) {
        		otherHosts.add(h);
        	}
        }
        
        System.out.println("[INFO] [MASTER] Third step - Sending list of other host with size: " + otherHosts.size());
        System.out.println("[INFO] [MASTER] Third step - FINISHED");
        return otherHosts;
    }

    @Override
    public UpdatePackage sendAllLoggedInUsersToNode(HandshakeDTO handshakeDTO) {
    	Host sender = handshakeDTO.getSender();
    	UpdatePackage updatePackage = handshakeDTO.getUpdatePackage();
    	int handshake = handshakeDTO.getHandshake();

		if (handshake == 1) {
			System.out.println("[INFO] [MASTER] Fourth step - Received request from host: " + sender.getIpAddress());
			UpdatePackage newUpdatePackage = new UpdatePackage();
			Map<String, List<String>> loggedInUsersByHosts = new HashMap<>();
			Map<String, Set<String>> registeredUsersByHosts = new HashMap<>();
			List<String> loggedInUsernamesOnMaster = new ArrayList<>();
			Set<String> registeredUsernamesOnMaster = new HashSet<>();
			
			//Logged in users directly from master
			for (User u: storageBean.getLoggedInUsers().values()) {
				loggedInUsernamesOnMaster.add(u.getUsername());
			}
			loggedInUsersByHosts.put(hostManagerBean.getCurrentSlaveHost().getIpAddress(), loggedInUsernamesOnMaster);
			System.out.println("[INFO] [MASTER] Fourth step - [DIRECTLY MASTER] Size of list of logged in users: " + loggedInUsernamesOnMaster.size());
			
			//Logged in users from other hosts
			for (Map.Entry<String, List<String>> entry : hostManagerBean.getForeignLoggedUsers().entrySet()) {
			    if (!entry.getKey().equals(sender.getIpAddress())) {
			    	loggedInUsersByHosts.put(entry.getKey(), entry.getValue());
			    }
			}
			String jsonLoggedIn = new Gson().toJson(loggedInUsersByHosts);
			System.out.println("[INFO] [MASTER] Fourth step - [ALL] Map of logged in users converted to JSON");
			newUpdatePackage.getLoggedInUsers().add(jsonLoggedIn);
			System.out.println("[INFO] [MASTER] Fourth step - [ALL] Map of logged in users added to package");
			
			//Registered users directly from master
			for (User u: storageBean.getUsers().values()) {
				registeredUsernamesOnMaster.add(u.getUsername());
			}
			registeredUsersByHosts.put(hostManagerBean.getCurrentSlaveHost().getIpAddress(), registeredUsernamesOnMaster);
			System.out.println("[INFO] [MASTER] Fourth step - [DIRECTLY MASTER] Size of set of registered users: " + registeredUsernamesOnMaster.size());
			
			//Registered users from other hosts
			for (Map.Entry<String, Set<String>> entry : hostManagerBean.getForeignRegisteredUsers().entrySet()) {
				if (!entry.getKey().equals(sender.getIpAddress())) {
					registeredUsersByHosts.put(entry.getKey(), entry.getValue());
				}
			}
			String jsonRegistered = new Gson().toJson(registeredUsersByHosts);
			System.out.println("[INFO] [MASTER] Fourth step - [ALL] Map of registered users converted to JSON");
			newUpdatePackage.getRegisteredUsers().add(jsonRegistered);
			System.out.println("[INFO] [MASTER] Fourth step - [ALL] Map of registered users added to package");
			
			System.out.println("[INFO] [MASTER] Fourth step - FINISHED");
			return newUpdatePackage;
			
		} else {
			System.out.println("[INFO] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Got an user update from host: " + sender.getIpAddress());
			hostManagerBean.getForeignLoggedUsers().put(sender.getIpAddress(), updatePackage.getLoggedInUsers());
			hostManagerBean.getForeignRegisteredUsers().put(sender.getIpAddress(), updatePackage.getRegisteredUsers());
			updateUsersInSocket();
			
			return updatePackage;
		}
	}
    
    @Override
    public void deleteHost(@PathParam("alias") String alias) {
    	System.out.println("[DELETE] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Deleting host: " + alias);
    	Host deletedHost = hostManagerBean.getHosts().remove(alias);
		if (deletedHost != null) {
			hostManagerBean.getForeignLoggedUsers().remove(alias);
			hostManagerBean.getForeignRegisteredUsers().remove(alias);
			hostManagerBean.getAgentTypes().remove(alias);
			hostManagerBean.getRunningAgents().remove(alias);
			System.out.println("[DELETE] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Host {" + alias + "} is removed");
			
			updateUsersInSocket();
			updateRunningAgents();
			updateAgentTypes();
    		updateSockets("agentmessage", new Gson().toJson("[DELETE] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Host {" + alias + "} is removed"));
			purgeMessages(alias);
			
			if (hostManagerBean.getCurrentSlaveHost().getIpAddress().equals(hostManagerBean.getMasterHost().getIpAddress())) {
	    		for (Host h: hostManagerBean.getHosts().values()) {
	    			if (!h.getIpAddress().equals(hostManagerBean.getMasterHost().getIpAddress())) {
	    				System.out.println("[DELETE] [MASTER] Deleting {" + alias + "} from {" + h.getAlias() + "}");
	    				RestHostBuilder.deleteHostBuilder(h, deletedHost);
	    			}
	    		}
	    		System.out.println("[DELETE] [MASTER] All other host are purged from {" + alias + "}");
	    	}
		}
    }
    
    @Override
    public int sendMessage(ForeignMessage foreignMessage) {
    	System.out.println("[INFO] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Recieved message from host {" + foreignMessage.getIpSendingHost() + "}");
    	User receivingUser = storageBean.getUsers().get(foreignMessage.getRecieverUsername());
    	if (receivingUser != null) {
    		receivingUser.getReceivedForeignMessages().add(foreignMessage);
    		System.out.println("[INFO] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Received message added to model");
    		
    		MessageDTO messageDTO = new MessageDTO(foreignMessage);
    		String jsonMessageDTO = new Gson().toJson(messageDTO);
			ws.echoTextMessage(jsonMessageDTO);
			System.out.println("[INFO] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Received message sent to sockets");
			return 1;
    	} else {
    		System.out.println("[ERROR] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] User " + foreignMessage.getRecieverUsername() + " doesn't exist");
    		System.out.println("[ERROR] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Message is not sent");
    		return 0;
    	}
    }
    
    @Override
    public int checkIfAlive() {
    	return 1;
    }
    
    @Override
    public void deleteFromSpecificHost(@PathParam("alias") String alias) {
    	System.out.println("[DELETE] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Deleting host: " + alias);
    	Host deletedHost = hostManagerBean.getHosts().remove(alias);
		if (deletedHost != null) {
			hostManagerBean.getForeignLoggedUsers().remove(alias);
			hostManagerBean.getForeignRegisteredUsers().remove(alias);
			hostManagerBean.getAgentTypes().remove(alias);
			hostManagerBean.getRunningAgents().remove(alias);
			System.out.println("[DELETE] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Host {" + alias + "} is removed");
			
			updateUsersInSocket();
			updateAgentTypes();
			updateRunningAgents();
			updateAgentTypes();
    		updateSockets("agentmessage", new Gson().toJson("[DELETE] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Host {" + alias + "} is removed"));
			purgeMessages(alias);
		}
    }
    
    @Override
    public void sendAgentTypesToSpecificHost(AgentSpecificDTO agentSpecificDTO) {
    	Host sendingHost = new Host();
    	Collection<AgentType> agentTypes = new ArrayList<>();
    	sendingHost = new Gson().fromJson(agentSpecificDTO.getHostJSON(), sendingHost.getClass());
    	agentTypes = new Gson().fromJson(agentSpecificDTO.getContentJSON(), agentTypes.getClass()); 
    	if (hostManagerBean.getCurrentSlaveHost().getIpAddress().equals(hostManagerBean.getMasterHost().getIpAddress())) {
    		System.out.println("[INFO] [MASTER] Fifth step - Received agent types from host: " + sendingHost.getIpAddress());
        	this.hostManagerBean.getAgentTypes().put(sendingHost.getIpAddress(), new ArrayList<>(agentTypes));

        	System.out.println("[INFO] [MASTER] Fifth step - New agent types has been added");
            System.out.println("[INFO] [MASTER] Fifth step - Sending agent types to other hosts");
            updateAgentTypes();
    		updateSockets("agentmessage", new Gson().toJson("[INFO] [MASTER] Received agent types from host: " + sendingHost.getIpAddress() + "}"));
            //TODO Ovo jos bi trebalo da se uradi
            Collection<AgentType> allAgentTypes = createCollectionOfAllAgentTypes();
            for(Host h: hostManagerBean.getHosts().values()) {
            	if(!h.getIpAddress().equals(sendingHost.getIpAddress()) && !h.getIpAddress().equals(hostManagerBean.getMasterHost().getIpAddress())) {
            		RestHostBuilder.sendAgentTypesToSpecificHostBuilder(hostManagerBean.getMasterHost(), h, allAgentTypes);
            	}
            }
            System.out.println("[INFO] [MASTER] Fifth step - FINISHED");
    	} else {
    		System.out.println("[INFO] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Received agent types from host: " + sendingHost.getIpAddress());
    		addAgentTypes(agentTypes);
    		updateAgentTypes();
     		updateSockets("agentmessage", new Gson().toJson("[INFO] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Received agent types from host: " + sendingHost.getIpAddress() + "}"));

     		System.out.println("[INFO] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] New agent types has been added");
            System.out.println("[INFO] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] FINISHED");
    	}
    }
    
    @Override
    public Collection<AgentType> getAgentTypesFromMaster() {
    	System.out.println("[INFO] [MASTER] Sixth step - Received request for retrieval of all agent types");
    	Collection<AgentType> allAgentTypes = createCollectionOfAllAgentTypes();
    	System.out.println("[INFO] [MASTER] Sixth step - FINISHED");
    	return allAgentTypes;
    }
    
    @Override
    public Collection<Agent> sendingRunningAgentsToNode(AgentSpecificDTO agentSpecificDTO) {
    	Host sendingHost = new Host();
    	Collection<Agent> receivingRunningAgents = new ArrayList<>();
    	sendingHost = new Gson().fromJson(agentSpecificDTO.getHostJSON(), sendingHost.getClass());
    	receivingRunningAgents = new Gson().fromJson(agentSpecificDTO.getContentJSON(), receivingRunningAgents.getClass()); 
    	if (receivingRunningAgents.size() == 0 && hostManagerBean.getCurrentSlaveHost().getIpAddress().equals(hostManagerBean.getMasterHost().getIpAddress())) {
	    	System.out.println("[INFO] [MASTER] Seventh step - Received request for retrieval of all running agents from {" + sendingHost.getIpAddress() + "}");
	    	Collection<Agent> allRunningAgents = new ArrayList<>();
	    	for(List<Agent> a: hostManagerBean.getRunningAgents().values()) {
	    		allRunningAgents.addAll(a);
	    	}
	    	System.out.println("[INFO] [MASTER] Seventh step - FINISHED");
	    	return allRunningAgents;
    	} else {
	    	System.out.println("[INFO] ["+ this.hostManagerBean.getCurrentSlaveHost().getIpAddress() +"] Received new set of running agents from {" + sendingHost.getIpAddress() + "}");
	    	this.hostManagerBean.getRunningAgents().put(sendingHost.getIpAddress(), new ArrayList<>(receivingRunningAgents));

	    	updateRunningAgents();
     		updateSockets("agentmessage", new Gson().toJson("[INFO] ["+ this.hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Received new set of running agents from {" + sendingHost.getIpAddress() + "}"));
	    	System.out.println("[INFO] ["+ this.hostManagerBean.getCurrentSlaveHost().getIpAddress() +"] List of running agents has been updated from {" + sendingHost.getIpAddress() + "}");
	    	return receivingRunningAgents;
    	}
    }
    
    @Override
    public void sendACLMessage(AgentSpecificDTO agentSpecificDTO) {
    	Host sendingHost = new Host();
    	ACLMessage newACLMessage = new ACLMessage();
    	sendingHost = new Gson().fromJson(agentSpecificDTO.getHostJSON(), sendingHost.getClass());
    	newACLMessage = new Gson().fromJson(agentSpecificDTO.getContentJSON(), newACLMessage.getClass());
    	System.out.println("[INFO] ["+ this.hostManagerBean.getCurrentSlaveHost().getIpAddress() +"] Received new ACL message from {" + sendingHost.getIpAddress() + "}");
    	//this.hostManagerBean.getACLmessages().put(UUID.randomUUID(), newACLMessage);
    	//TODO update list of acl messages on other hosts
 		updateSockets("agentmessage", new Gson().toJson("[INFO] ["+ this.hostManagerBean.getCurrentSlaveHost().getIpAddress() +"] Received new ACL message from {" + sendingHost.getIpAddress() + "}. Performative: " + newACLMessage.getPerformative().toString()));
    	System.out.println("[INFO] ["+ this.hostManagerBean.getCurrentSlaveHost().getIpAddress() +"] Forwarding ACL message to agents on this host");
    	AID[] receivers = newACLMessage.getReceivers();
    	for (AID a: receivers) {
			if(a.getHost().getIpAddress().equals(this.hostManagerBean.getCurrentSlaveHost().getIpAddress())) {
				Agent at = HostService.findAgentWithAID(this.hostManagerBean.getRunningAgents().get(this.hostManagerBean.getCurrentSlaveHost().getIpAddress()), a);
				at.handleMessage(newACLMessage);
			}
		}
    	System.out.println("[INFO] ["+ this.hostManagerBean.getCurrentSlaveHost().getIpAddress() +"] ACL message forwarded to agents on this host - FINISHED");
    }
    
    public void updateUsersInSocket() {
    	System.out.println("[INFO] Updating sockets");
    	List<String> usernames = new ArrayList<>(ws.getUserSessions().keySet());
    	for (List<String> listOfForeignLoggedInUsers: hostManagerBean.getForeignLoggedUsers().values()) {
    		usernames.addAll(listOfForeignLoggedInUsers);
    	}
		SocketMessage message = new SocketMessage("logged", new Date(), new Gson().toJson(usernames));
		String jsonMessage = new Gson().toJson(message);
		try {
			for (Session s: ws.getSessions()) {
				s.getBasicRemote().sendText(jsonMessage);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		usernames = new ArrayList<>(ws.getRegisteredUsers());
		for (Set<String> setOfForeignRegisteredUsers: hostManagerBean.getForeignRegisteredUsers().values()) {
    		usernames.addAll(new ArrayList<String>(setOfForeignRegisteredUsers));
    	}
		message = new SocketMessage("registered", new Date(), new Gson().toJson(usernames));
		jsonMessage = new Gson().toJson(message);
		try {
			for (Session s: ws.getSessions()) {
				s.getBasicRemote().sendText(jsonMessage);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void purgeMessages(String hostIp) {
    	System.out.println("[INFO] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Purging messages of deleted host {" + hostIp + "}");
    	for(User u: storageBean.getUsers().values()) {
    		List<ForeignMessage> receivedToRemove = new ArrayList<>();
    		for (ForeignMessage received: u.getReceivedForeignMessages()) {
    			if (received.getIpSendingHost().equals(hostIp)) {
    				receivedToRemove.add(received);
    			}
    		}
    		if (receivedToRemove.size() != 0) {
    			u.getReceivedForeignMessages().removeAll(receivedToRemove);
    		}
    		
    		List<ForeignMessage> sentToRemove = new ArrayList<>();
    		for (ForeignMessage sent: u.getSentForeignMessages()) {
    			if (sent.getIpReceivingHost().equals(hostIp)) {
    				sentToRemove.add(sent);
    			}
    		}
    		if (sentToRemove.size() != 0) {
    			u.getSentForeignMessages().removeAll(sentToRemove);
    		}
    	}
    	System.out.println("[INFO] [" + hostManagerBean.getCurrentSlaveHost().getIpAddress() + "] Messages purged");
    }
    
    public Collection<AgentType> createCollectionOfAllAgentTypes() {
    	Collection<AgentType> allAgentTypes = new ArrayList<>();
    	for(List<AgentType> atl: hostManagerBean.getAgentTypes().values()) {
    		allAgentTypes.addAll(atl);
    	}
    	return allAgentTypes;
    }
    
    public void addAgentTypes(Collection<AgentType> allAgentTypes) {
    	for(AgentType at: allAgentTypes) {
			if (this.hostManagerBean.getAgentTypes().get(at.getModule()) == null) {
				List<AgentType> atList = new ArrayList<>();
				atList.add(at);
				this.hostManagerBean.getAgentTypes().put(at.getModule(), atList);
			} else {
				this.hostManagerBean.getAgentTypes().get(at.getModule()).add(at);
			}
		}
    }
    
	
	public static Agent findAgentWithAID(List<Agent> aList, AID aid) {
		for(Agent a: aList) {
			if(a.getAid().getName().equals(aid.getName())) {
				return a;
			}
		}
		return null;
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
	
	public void updateRunningAgents() {
		List<Agent> agents = new ArrayList<>();
		for(List<Agent> aList: this.hostManagerBean.getRunningAgents().values()) {
			agents.addAll(aList);
		}
		updateSockets("runningagents", new Gson().toJson(agents));
	}
	
	public void updateAgentTypes() {
		List<AgentType> agentTypes = new ArrayList<>();
		for(List<AgentType> atList: this.hostManagerBean.getAgentTypes().values()) {
			agentTypes.addAll(atList);
		}
		updateSockets("agenttypes", new Gson().toJson(agentTypes));
	}
    
}