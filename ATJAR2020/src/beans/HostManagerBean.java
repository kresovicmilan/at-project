package beans;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import com.google.gson.Gson;

import implementation.RestHostBuilder;
import models.ACLMessage;
import models.AID;
import models.Agent;
import models.AgentType;
import models.Host;
import models.UpdatePackage;
import models.User;
import services.HostService;

@Singleton
@LocalBean
@Startup
public class HostManagerBean {
	
	private Map<String, Host> hosts = new HashMap<>();
	private String masterInfo = "";
	private String hostInfo = "";
	private Host masterHost = new Host();
	private Host currentSlaveHost = new Host();
	private Map<String, List<String>> foreignLoggedUsers = new HashMap<>();
	private Map<String, Set<String>> foreignRegisteredUsers = new HashMap<>();
	
	private Map<String, List<AgentType>> agentTypes = new HashMap<>();
	private Map<String, List<Agent>> runningAgents = new HashMap<>();
	private Map<UUID, ACLMessage> ACLmessages = new HashMap();
	
	@EJB
	StorageBean storageBean;
	
	@EJB
	HostService hostService;
	
	@PostConstruct
	public void handshakeInit() {
		System.out.println("[INFO] Setting up the server");
		
		findMasterIpAddress();
		setHosts();
		setAgentTypes();
		this.runningAgents.put(currentSlaveHost.getIpAddress(), new ArrayList<>());
		
		System.out.println("[INFO] Master IP: " + this.masterHost.getIpAddress());
		System.out.println("[INFO] Slave host IP: " + this.currentSlaveHost.getIpAddress());
		
		if (!masterHost.equals(currentSlaveHost)) {
			System.out.println("[INFO] Handshake started");
			
			System.out.println("[INFO] [NEW HOST] First step - Register to master: " + this.currentSlaveHost.getIpAddress());
			System.out.println("[INFO] [NEW HOST] Second step - Master should send new host to other hosts");
			try {
				RestHostBuilder.registerNodeBuilder(this.currentSlaveHost, this.masterHost);
			} catch (Exception e) {
				startAgain("First");
			}
			System.out.println("[INFO] [NEW HOST] First step - FINISHED");
			System.out.println("[INFO] [NEW HOST] Second step - FINISHED");
			
			System.out.println("[INFO] [NEW HOST] Third step - Receiving other host from master");
			try {
				Collection<Host> otherHosts = RestHostBuilder.sendHostsToNewHostBuilder(this.currentSlaveHost, this.masterHost);
				System.out.println("[INFO] [NEW HOST] Third step - Received list of other hosts from master with size: " + otherHosts.size());
				for (Host h: otherHosts) {
					this.hosts.put(h.getIpAddress(), h);
				}
			} catch (Exception e) {
				startAgain("Third");
			}
			System.out.println("[INFO] [NEW HOST] Third step - FINISHED");
			
			System.out.println("[INFO] [NEW HOST] Fourth step - Receiving logged in users from other hosts");
			try {
				UpdatePackage newUpdatePackage = RestHostBuilder.sendAllLoggedInUsersToNodeBuilder(this.currentSlaveHost, this.masterHost, new UpdatePackage(), 1);
				foreignLoggedUsers = new Gson().fromJson(newUpdatePackage.getLoggedInUsers().get(0), foreignLoggedUsers.getClass());
				System.out.println("[INFO] [NEW HOST] Fourth step - Received map of logged users");
				Map<String, List<String>> helpMap = new Gson().fromJson(newUpdatePackage.getRegisteredUsers().iterator().next(), foreignRegisteredUsers.getClass());
				helpConversion(helpMap);
				System.out.println("[INFO] [NEW HOST] Fourth step - Received set of registered users");
			} catch (Exception e) {
				startAgain("Fourth");
			}
			System.out.println("[INFO] [NEW HOST] Fourth step - FINISHED");
			
			System.out.println("[INFO] [NEW HOST] Fifth step - Master is receiving list of agent types");
			try {
				RestHostBuilder.sendAgentTypesToSpecificHostBuilder(this.currentSlaveHost, this.masterHost, this.agentTypes.get(this.currentSlaveHost.getIpAddress()));
			} catch(Exception e) {
				startAgain("Fifth");
			}
			System.out.println("[INFO] [NEW HOST] Fifth step - FINISHED");
			
			System.out.println("[INFO] [NEW HOST] Sixth step - Receive all agent types from master");
			try {
				Collection<AgentType> allAgentTypes = RestHostBuilder.getAgentTypesFromMasterBuilder(this.masterHost);
				System.out.println("[INFO] [NEW HOST] Sixth step - Adding all agent types from master by IP address of their host");
				for(AgentType at: allAgentTypes) {
					if (this.agentTypes.get(at.getModule()) == null) {
						List<AgentType> atList = new ArrayList<>();
						atList.add(at);
						this.agentTypes.put(at.getModule(), atList);
					} else {
						this.agentTypes.get(at.getModule()).add(at);
					}
				}
				System.out.println("[INFO] [NEW HOST] Sixth step - All agent types from master added by IP address of their host");
			} catch(Exception e) {
				startAgain("Sixth");
			}
			System.out.println("[INFO] [NEW HOST] Sixth step - FINISHED");
			
			System.out.println("[INFO] [NEW HOST] Seventh step - Receive all running agents from master");
			try {
				Collection<Agent> allRunningAgents = RestHostBuilder.sendingRunningAgentsToNodeBuilder(this.currentSlaveHost, this.masterHost, new ArrayList<Agent>());
				System.out.println("[INFO] [NEW HOST] Seventh step - Adding all running agents from master by IP address of their host");
				for(Agent a: allRunningAgents) {
					if (this.runningAgents.get(a.getAid().getHost().getIpAddress()) == null) {
						List<Agent> aList = new ArrayList<>();
						aList.add(a);
						this.runningAgents.put(a.getAid().getHost().getIpAddress(), aList);
					} else {
						this.runningAgents.get(a.getAid().getHost().getIpAddress()).add(a);
					}
				}
				System.out.println("[INFO] [NEW HOST] Seventh step - All running agents from master added by IP address of their host");
			} catch(Exception e) {
				startAgain("Seventh");
			}
			System.out.println("[INFO] [NEW HOST] Seventh step - FINISHED");
			
			System.out.println("[INFO] Handshake over - SUCCESS");
			
		}	
		
	}
	
	@PreDestroy
	public void shutDownHost() {
		System.out.println("[SHUTDOWN] Shutting down the host");
		System.out.println("[SHUTDOWN] Deleting host from master");
		RestHostBuilder.deleteHostBuilder(this.masterHost, this.currentSlaveHost);
		System.out.println("[SHUTDOWN] Host deleted from master");
	}
	
	//@Schedule(hour="*", minute = "*", second = "*/60", info = "Every 60 seconds")
	public void heartbeatProtocol() {
		System.out.println("[INFO] [HEARTBEAT] Starting");
		
		for (Host h: hosts.values()) {
			if (!h.getIpAddress().contains(currentSlaveHost.getIpAddress())) {
				System.out.println("[INFO] [HEARTBEAT] Checking is alive {" + h.getIpAddress() + "}");
				int succ = 0;
				try {
					succ = RestHostBuilder.checkIfAliveBuilder(h);
				} catch (Exception e) {
					System.out.println("[INFO] [HEARTBEAT] Host {" + h.getIpAddress() + "} didn't answer");
					System.out.println("[INFO] [HEARTBEAT] Checking is alive {" + h.getIpAddress() + "} - Second time");
					try {
						succ = RestHostBuilder.checkIfAliveBuilder(h);
					} catch (Exception eSecond) {
						System.out.println("[INFO] [HEARTBEAT] Host {" + h.getIpAddress() + "} didn't answer");
						System.out.println("[INFO] [HEARTBEAT] Host {" + h.getIpAddress() + "} is dead");
					}
				}
				
				if (succ != 1) {
					System.out.println("[INFO] [HEARTBEAT] Deleting host {" + h.getIpAddress() + "} from current host");
					deleteHostFromCurrentHost(h.getIpAddress());
					System.out.println("[INFO] [HEARTBEAT] Host deleted {" + h.getIpAddress() + "} from current host");
					System.out.println("[INFO] [HEARTBEAT] Deleting host {" + h.getIpAddress() + "} from other hosts");
					deleteHostFromOtherHosts(h);
					System.out.println("[INFO] [HEARTBEAT] Host deleted {" + h.getIpAddress() + "} from other hosts");
				} else {
					System.out.println("[INFO] [HEARTBEAT] Host {" + h.getIpAddress() + "} is OK");
				}
			}
		}
		System.out.println("[INFO] [HEARTBEAT] Finished");
	}
	
	public void startAgain(String err) {
		try {
			System.out.println("[INFO] " + err + "step retrying");
			switch(err) {
				case "First":
					System.out.println("[INFO] [NEW HOST] First step - Register to master: " + this.currentSlaveHost.getIpAddress());
					System.out.println("[INFO] [NEW HOST] Second step - Master should send new host to other hosts");
					RestHostBuilder.registerNodeBuilder(this.currentSlaveHost, this.masterHost);
					break;
				case "Third":
					Collection<Host> otherHosts = RestHostBuilder.sendHostsToNewHostBuilder(this.currentSlaveHost, this.masterHost);
					System.out.println("[INFO] [NEW HOST] Third step - Received list of other hosts from master with size: " + otherHosts.size());
					for (Host h: otherHosts) {
						this.hosts.put(h.getIpAddress(), h);
					}
					break;
				case "Fourth":
					UpdatePackage newUpdatePackage = RestHostBuilder.sendAllLoggedInUsersToNodeBuilder(this.currentSlaveHost, this.masterHost, new UpdatePackage(), 1);
					System.out.println("[INFO] [NEW HOST] Fourth step - Received list of logged users with size: " + newUpdatePackage.getLoggedInUsers().size());
					System.out.println("[INFO] [NEW HOST] Fourth step - Received set of registered users with size: " + newUpdatePackage.getRegisteredUsers().size());
					foreignLoggedUsers = new Gson().fromJson(newUpdatePackage.getLoggedInUsers().get(0), foreignLoggedUsers.getClass());
					System.out.println("[INFO] [NEW HOST] Fourth step - Received map of logged users");
					Map<String, List<String>> helpMap = new Gson().fromJson(newUpdatePackage.getRegisteredUsers().iterator().next(), foreignRegisteredUsers.getClass());
					helpConversion(helpMap);
					System.out.println("[INFO] [NEW HOST] Fourth step - Received set of registered users");
					break;
				case "Fifth":
					System.out.println("[INFO] [NEW HOST] Fifth step - Master is receiving list of agent types");
					RestHostBuilder.sendAgentTypesToSpecificHostBuilder(this.currentSlaveHost, this.masterHost, this.agentTypes.get(this.currentSlaveHost.getIpAddress()));
					break;
				case "Sixth":
					Collection<AgentType> allAgentTypes = RestHostBuilder.getAgentTypesFromMasterBuilder(this.masterHost);
					System.out.println("[INFO] [NEW HOST] Sixth step - Adding all agent types from master by IP address of their host");
					for(AgentType at: allAgentTypes) {
						if (this.agentTypes.get(at.getModule()) == null) {
							List<AgentType> atList = new ArrayList<>();
							atList.add(at);
							this.agentTypes.put(at.getModule(), atList);
						} else {
							this.agentTypes.get(at.getModule()).add(at);
						}
					}
					System.out.println("[INFO] [NEW HOST] Sixth step - All agent types from master added by IP address of their host");
					break;
				case "Seventh":
					Collection<Agent> allRunningAgents = RestHostBuilder.sendingRunningAgentsToNodeBuilder(this.currentSlaveHost, this.masterHost, new ArrayList<Agent>());
					System.out.println("[INFO] [NEW HOST] Seventh step - Adding all running agents from master by IP address of their host");
					for(Agent a: allRunningAgents) {
						if (this.runningAgents.get(a.getAid().getHost().getIpAddress()) == null) {
							List<Agent> aList = new ArrayList<>();
							aList.add(a);
							this.runningAgents.put(a.getAid().getHost().getIpAddress(), aList);
						} else {
							this.runningAgents.get(a.getAid().getHost().getIpAddress()).add(a);
						}
					}
					System.out.println("[INFO] [NEW HOST] Seventh step - All running agents from master added by IP address of their host");
					break;
			}	
		} catch (Exception e) {
			System.out.println("[INFO] [ERROR] Some error has occured in " + err.toLowerCase() + " step");
			System.out.println("[INFO] [ERROR] Deleting host from master");
			RestHostBuilder.deleteHostBuilder(this.masterHost, this.currentSlaveHost);
			System.out.println("[INFO] [ERROR] Host deleted from master");
		}
	}
	
	public void setHosts() {
		String aliasMaster = this.masterInfo.split(":")[0];
		String ipMaster = this.masterInfo.split(":")[1];
		String portMaster = this.masterInfo.split(":")[2];
		
		String aliasSlave = this.hostInfo.split(":")[0];
		String ipSlave = this.hostInfo.split(":")[1];
		String portSlave = this.hostInfo.split(":")[2];
		
		Host masterHost = new Host(ipMaster + ":" + portMaster, ipMaster + ":" + portMaster);
		this.hosts.put(masterHost.getIpAddress(), masterHost);
		this.masterHost = masterHost;
		
		if (!ipMaster.equals(System.getProperty("jboss.bind.address"))) {
			Host slaveHost = new Host(ipSlave + ":" + portSlave, ipSlave + ":" + portSlave);
			this.hosts.put(slaveHost.getIpAddress(), slaveHost);
			this.currentSlaveHost = slaveHost;
		} else {
			this.currentSlaveHost = masterHost;
		}
	}
	
	public void setAgentTypes() {
		List<AgentType> currentAgentTypes = new ArrayList<>();
		AgentType at1 = new AgentType("FinderAgent", this.currentSlaveHost.getIpAddress());
		AgentType at2 = new AgentType("User", this.currentSlaveHost.getIpAddress());
		currentAgentTypes.add(at1);
		currentAgentTypes.add(at2);
		if(masterHost.getIpAddress().equals(currentSlaveHost.getIpAddress())) {
			AgentType at3 = new AgentType("PredictorAgent", this.currentSlaveHost.getIpAddress());
			AgentType at4 = new AgentType("MasterAgent", this.currentSlaveHost.getIpAddress());
			currentAgentTypes.add(at3);
			currentAgentTypes.add(at4);
		}
		
		this.agentTypes.put(this.currentSlaveHost.getIpAddress(), currentAgentTypes);
	}
	
	public void findMasterIpAddress() {
		String masterIp = "";
		String hostIp = "";
		
		try {
		      File ipConfigFile = new File(User.class.getProtectionDomain().getCodeSource().getLocation().getPath() 
		    		  + File.separator + "META-INF" 
		    		  + File.separator + "ip_config.txt");
		      Scanner reader = new Scanner(ipConfigFile);
		      
		      if (reader.hasNextLine()) {
		    	  masterIp = reader.nextLine();
		      }
		      
		      if (reader.hasNextLine()) {
		    	  hostIp = reader.nextLine();
		      }
		      
		      reader.close();
		      
		    } catch (FileNotFoundException e) {
		      System.out.println("Config file is not found.");
		      e.printStackTrace();
		    }
		
		if (masterIp.equals("master:mLocalhost:8080") || hostIp.equals("host:hLocalhost:8080") || !hostIp.split(":")[1].equals(System.getProperty("jboss.bind.address"))) {
			throw new Error ("Set up ip_config.txt file in META-INF folder");
		}
		
		this.masterInfo = masterIp;
		this.hostInfo = hostIp;
		
	}
	
	public void helpConversion(Map<String, List<String>> helpMap) {
		Map<String, Set<String>> newMap = new HashMap<>();
		for(Map.Entry<String, List<String>> entry: helpMap.entrySet()) {
			newMap.put(entry.getKey(), new HashSet<String>(entry.getValue()));
		}
		foreignRegisteredUsers = newMap;
	}
	
	public void deleteHostFromCurrentHost(String hostIp) {
		this.hosts.remove(hostIp);
		this.foreignLoggedUsers.remove(hostIp);
		this.foreignRegisteredUsers.remove(hostIp);
		System.out.println("[DELETE] [" + currentSlaveHost.getIpAddress() + "] Host {" + hostIp + "} is removed");
		
		hostService.updateUsersInSocket();
		
		hostService.purgeMessages(hostIp);
	}
	
	public void deleteHostFromOtherHosts(Host deletedHost) {
		for(Host h: hosts.values()) {
			if ((!h.getIpAddress().equals(currentSlaveHost.getIpAddress())) && (!h.getIpAddress().equals(deletedHost.getIpAddress()))) {
				System.out.println("[DELETE] [" + currentSlaveHost.getIpAddress() + "] Deleting host {" + deletedHost.getIpAddress() + "} - from host {" + h.getIpAddress() + "}");
				try {
					RestHostBuilder.deleteFromSpecificHostBuilder(h, deletedHost);
				} catch (Exception e) {
					System.out.println("[DELETE] [" + currentSlaveHost.getIpAddress() + "] [ERROR] Deleting host {" + deletedHost.getIpAddress() + "} - from host {" + h.getIpAddress() + "} - Second time");
					try {
						RestHostBuilder.deleteFromSpecificHostBuilder(h, deletedHost);
					} catch (Exception eSecond) {
						System.out.println("[DELETE] [" + currentSlaveHost.getIpAddress() + "] [ERROR] Not able to delete host {" + deletedHost.getIpAddress() + "} - from host {" + h.getIpAddress() + "}");
					}
				}
			}
		}
	}
	
	public Map<String, Host> getHosts() {
		return hosts;
	}

	public void setHosts(Map<String, Host> hosts) {
		this.hosts = hosts;
	}


	public String getMasterInfo() {
		return masterInfo;
	}


	public void setMasterInfo(String masterInfo) {
		this.masterInfo = masterInfo;
	}


	public String getHostInfo() {
		return hostInfo;
	}


	public void setHostInfo(String hostInfo) {
		this.hostInfo = hostInfo;
	}


	public Host getMasterHost() {
		return masterHost;
	}


	public void setMasterHost(Host masterHost) {
		this.masterHost = masterHost;
	}


	public Host getCurrentSlaveHost() {
		return currentSlaveHost;
	}


	public void setCurrentSlaveHost(Host currentSlaveHost) {
		this.currentSlaveHost = currentSlaveHost;
	}

	public Map<String, List<String>> getForeignLoggedUsers() {
		return foreignLoggedUsers;
	}

	public void setForeignLoggedUsers(Map<String, List<String>> foreignLoggedUsers) {
		this.foreignLoggedUsers = foreignLoggedUsers;
	}

	public Map<String, Set<String>> getForeignRegisteredUsers() {
		return foreignRegisteredUsers;
	}

	public void setForeignRegisteredUsers(Map<String, Set<String>> foreignRegisteredUsers) {
		this.foreignRegisteredUsers = foreignRegisteredUsers;
	}

	public Map<String, List<AgentType>> getAgentTypes() {
		return agentTypes;
	}

	public void setAgentTypes(Map<String, List<AgentType>> agentTypes) {
		this.agentTypes = agentTypes;
	}

	public Map<String, List<Agent>> getRunningAgents() {
		return runningAgents;
	}

	public void setRunningAgents(Map<String, List<Agent>> runningAgents) {
		this.runningAgents = runningAgents;
	}

	public Map<UUID, ACLMessage> getACLmessages() {
		return ACLmessages;
	}

	public void setACLmessages(Map<UUID, ACLMessage> aCLmessages) {
		ACLmessages = aCLmessages;
	}

}
