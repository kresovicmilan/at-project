package services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.websocket.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;

import beans.HostManagerBean;
import beans.StorageBean;
import implementation.RestHostBuilder;
import model.SocketMessage;
import models.AID;
import agents.FinderAgent;
import agents.MasterAgent;
import agents.PredictorAgent;
import models.Agent;
import models.AgentType;
import models.Host;
import ws.WSEndPoint;

@Stateless
@Remote(AgentServiceRemote.class)
@Path("/agents")
@LocalBean
public class AgentService implements AgentServiceRemote {
	@EJB
	HostManagerBean hostManagerBean;
	
	@EJB
	StorageBean storageBean;
	
	@EJB
	WSEndPoint ws;
	
	@Override
	public Collection<AgentType> getAgentTypes() {
		System.out.println("[INFO] Retrieving agent types");
		Collection<AgentType> allAgentTypes = new ArrayList<>();
		for(List<AgentType> a: this.hostManagerBean.getAgentTypes().values()) {
			allAgentTypes.addAll(a);
		}
		return allAgentTypes;
	}
	
	@Override
	public Collection<Agent> getRunningAgents() {
		System.out.println("[INFO] Retrieving running agents");
		Collection<Agent> allRunningAgents = new ArrayList<>();
		for(List<Agent> a: this.hostManagerBean.getRunningAgents().values()) {
			allRunningAgents.addAll(a);
		}
		return allRunningAgents;
	}
	
	@Override
	public Response startAgent(@PathParam("type") String type, @PathParam("name") String name)	{
		System.out.println("[INFO] [START AGENT] Name: " + name);
		System.out.println("[INFO] [START AGENT] Type: " + type);
		for(List<Agent> aL: this.hostManagerBean.getRunningAgents().values()) {
			for(Agent a: aL) {
				if (a.getAid().getName().equals(name)) {
					updateSockets("agentmessage", new Gson().toJson("[ERROR] Agent name is not unique: " + name));
					System.out.println("[ERROR] Agent name is not unique");
					return Response.status(400).entity("[ERROR] Agent name is not unique").build();
				}
			}
		}
		
		AgentType newAgentType = null;
		System.out.println("[INFO] Finding if agent type exists");
		for (AgentType at: this.hostManagerBean.getAgentTypes().get(this.hostManagerBean.getCurrentSlaveHost().getIpAddress())) {
			if(at.getName().equals(type)) {
				newAgentType = at;
			}
		}
		
		if (newAgentType != null) {
			AID aid = new AID(name, this.hostManagerBean.getCurrentSlaveHost(), newAgentType);
			try {
				Object agentObject = Class.forName((newAgentType.equals("User")? "model." : "agents.") + newAgentType.getName()).newInstance();
				if(agentObject instanceof Agent) {
					((Agent) agentObject).setAid(aid);
					this.hostManagerBean.getRunningAgents().get(this.hostManagerBean.getCurrentSlaveHost().getIpAddress()).add((Agent) agentObject);
					System.out.println("[INFO] [START AGENT] Agent is made: " + name);
					
					System.out.println("[INFO] [START AGENT] Sending updated list to other hosts");
					for(Host h: this.hostManagerBean.getHosts().values()) {
						if (!h.getIpAddress().equals(this.hostManagerBean.getCurrentSlaveHost().getIpAddress())) {
							RestHostBuilder.sendingRunningAgentsToNodeBuilder(this.hostManagerBean.getCurrentSlaveHost(), h, this.hostManagerBean.getRunningAgents().get(this.hostManagerBean.getCurrentSlaveHost().getIpAddress()));
							System.out.println("[INFO] [START AGENT] Updated list sent to {" + h.getIpAddress() + "}");
						}
					}
					System.out.println("[INFO] [START AGENT] Updated list sent to other hosts");
					
					updateRunningAgents();
					updateSockets("agentmessage", new Gson().toJson("[INFO] [AGENT STARTED] Started agent on this host with name: " + name));
					System.out.println("[INFO] [START AGENT] Finished");
					return Response.status(200).entity(new Gson().toJson((Agent) agentObject)).build();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			updateSockets("agentmessage", new Gson().toJson("[INFO] [ERROR] Agent can't be made: " + name));
			System.out.println("[ERROR] Agent can't be made");
			return Response.status(400).entity("[ERROR] Agent can't be made").build();
		} else {
			updateSockets("agentmessage", new Gson().toJson("[ERROR] Could not find agent type: " + type));
			System.out.println("[ERROR] Could not find agent type");
			return Response.status(400).entity("[ERROR] Could not find agent type").build();
		}
	}
	
	@Override
	public Response stopAgent(@PathParam("aid") String name)	{
		Boolean isRemoved = false;
		Boolean isOtherHost = false;
		System.out.println("[INFO] [STOP AGENT] Name: " + name);
		
		for(List<Agent> aList: this.hostManagerBean.getRunningAgents().values()) {
			for(Agent a: aList) {
				if(a.getAid().getName().equals(name)) {
					if (a.getAid().getHost().getIpAddress().equals(this.hostManagerBean.getCurrentSlaveHost().getIpAddress())) {
						this.hostManagerBean.getRunningAgents().get(this.hostManagerBean.getCurrentSlaveHost().getIpAddress()).remove(a);
						System.out.println("[INFO] [STOP AGENT] Agent has been found and is removed from list of running agents");
						a.remove();
						isRemoved = true;
						break;
					} else {
						updateSockets("agentmessage", new Gson().toJson("[INFO] [STOP AGENT] Stopping agent on host {" + a.getAid().getHost().getIpAddress() + "}"));
						System.out.println("[INFO] [STOP AGENT] Stopping agent on host {" + a.getAid().getHost().getIpAddress() + "}");
						RestHostBuilder.stopAgentBuilder(a.getAid().getHost().getIpAddress(), name);
						isOtherHost = true;
					}
				}
			}
		}
		/*for(Agent a: this.hostManagerBean.getRunningAgents().get(this.hostManagerBean.getCurrentSlaveHost().getIpAddress())) {
			if (a.getAid().getName().equals(name)) {
				this.hostManagerBean.getRunningAgents().get(this.hostManagerBean.getCurrentSlaveHost().getIpAddress()).remove(a);
				System.out.println("[INFO] [STOP AGENT] Agent has been found and is removed from list of running agents");
				a.remove();
				isRemoved = true;
				break;
			}
		}*/
		
		if (!isRemoved && !isOtherHost) {
			updateSockets("agentmessage", new Gson().toJson("[ERROR] Agent with this name does not exist: " + name));
			System.out.println("[ERROR] Agent with this name does not exist");
			return Response.status(400).entity("[ERROR] Agent with this name does not exist").build();
		}
		
		if (isRemoved) {
			System.out.println("[INFO] [STOP AGENT] Sending updated list to other hosts");
			for(Host h: this.hostManagerBean.getHosts().values()) {
				if (!h.getIpAddress().equals(this.hostManagerBean.getCurrentSlaveHost().getIpAddress())) {
					RestHostBuilder.sendingRunningAgentsToNodeBuilder(this.hostManagerBean.getCurrentSlaveHost(), h, this.hostManagerBean.getRunningAgents().get(this.hostManagerBean.getCurrentSlaveHost().getIpAddress()));
					System.out.println("[INFO] [STOP AGENT] Updated list sent to {" + h.getIpAddress() + "}");
				}
			}
			System.out.println("[INFO] [STOP AGENT] Updated list sent to other hosts");
		}
		
		updateRunningAgents();
		updateSockets("agentmessage", new Gson().toJson("[INFO] [STOP AGENT] Agent has been removed: " + name));
		System.out.println("[INFO] [STOP AGENT] Finished");
		return Response.status(200).entity("[INFO] [STOP AGENT] Finished").build();
	}
	
	@Override
	public Response startAgentOtherHost(@PathParam("type") String type, @PathParam("name") String name, String hostIp) {
		System.out.println("[INFO] [START AGENT] Starting agent on host {" + hostIp + "}");
		System.out.println("[INFO] [START AGENT] Name: " + name);
		System.out.println("[INFO] [START AGENT] Type: " + type);
		updateSockets("agentmessage", new Gson().toJson("[INFO] [START AGENT] Starting agent on host {" + hostIp + "}"));
		return RestHostBuilder.startAgentBuilder(hostIp, type, name);
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
	
}
