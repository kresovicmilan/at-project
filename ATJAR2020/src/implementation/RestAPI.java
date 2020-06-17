package implementation;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import DTO.HandshakeDTO;
import models.ACLMessage;
import models.Agent;
import models.AgentType;
import models.ForeignMessage;
import models.Host;
import models.UpdatePackage;

public interface RestAPI {
	@POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
	public void registerNode(Host newHost);


	@POST
    @Path("/node")
    @Consumes(MediaType.APPLICATION_JSON)
    public void sendNewHostToHost(Host newHost);
	
	@POST
    @Path("/nodes")
    @Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public Collection<Host> sendHostsToNewHost(Host newHost);

	@POST
    @Path("/users/loggedIn")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public UpdatePackage sendAllLoggedInUsersToNode(HandshakeDTO handshakeDTO);
	
	@DELETE
    @Path("/node/{alias}")
	@Consumes(MediaType.APPLICATION_JSON)
    public void deleteHost(@PathParam("alias") String alias);
	
	@POST
    @Path("/message")
    @Consumes(MediaType.APPLICATION_JSON)
	public int sendMessage(ForeignMessage foreignMessage);
	
	@GET
	@Path("/node")
	@Produces(MediaType.APPLICATION_JSON)
    public int checkIfAlive();
	
	@DELETE
    @Path("/node/specific/{alias}")
	@Consumes(MediaType.APPLICATION_JSON)
    public void deleteFromSpecificHost(@PathParam("alias") String alias);
	
	@POST
	@Path("/agent/classes")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void sendAgentTypesToSpecificHost(Host currentSlaveHost, Collection<AgentType> agentTypes);
	
	@GET
	@Path("/agent/classes")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<AgentType> getAgentTypesFromMaster();
	
	@POST
	@Path("/agent/running")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<Agent> sendingRunningAgentsToNode(Host sendingHost, Collection<Agent> receivingRunningAgents);
	
	@POST
	@Path("/messages")
	@Consumes(MediaType.APPLICATION_JSON)
	public void sendACLMessage(Host sendingHost, ACLMessage newACLMessage);
	
	@DELETE
	@Path("/running/{aid}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response stopAgent(@PathParam("aid") String name);
	
	@PUT
	@Path("/running/{type}/{name}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response startAgent(@PathParam("type") String type, @PathParam("name") String name);
	
	/*@GET
	@Path("/classes")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<AgentType> getAgentTypes();
	
	@GET
	@Path("/running")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<Agent> getRunningAgents();
	
	@PUT
	@Path("/running/{type}/{name}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response startAgent(@PathParam("type") String type, @PathParam("name") String name);
	
	@DELETE
	@Path("/running/{aid}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response stopAgent(@PathParam("aid") String name);*/
}