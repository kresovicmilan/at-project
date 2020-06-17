package services;

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

import models.Agent;
import models.AgentType;

public interface AgentServiceRemote {
	@GET
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
	public Response stopAgent(@PathParam("aid") String name);
	
	@POST
	@Path("/running/{type}/{name}/{ipaddress}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response startAgentOtherHost(@PathParam("type") String type, @PathParam("name") String name, @PathParam("ipaddress") String hostIp);
}
