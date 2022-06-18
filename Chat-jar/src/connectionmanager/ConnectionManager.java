package connectionmanager;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import agents.AID;
import agents.AgentType;
import models.User;

public interface ConnectionManager {

	@POST
	@Path("/register")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public List<String> registerNode(String nodeAlias);
	
	@POST
	@Path("/node")
	@Consumes(MediaType.APPLICATION_JSON)
	public void addNode(String nodeAlias);
	
	@DELETE
	@Path("/node/{alias}")
	public void deleteNode(@PathParam("alias") String nodeAlias);
	
	@GET
	@Path("/node")
	@Produces(MediaType.TEXT_PLAIN)
	public String pingNode();
	
	@POST
	@Path("/notify/loggedIn")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void notifyAllLoggedIn();

	@POST
	@Path("/users/loggedIn")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void setLoggedInRemote(List<User> users);
	
	@POST
	@Path("/notify/registered")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void notifyAllRegistered();

	@POST
	@Path("/users/registered")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void setRegisteredRemote(List<User> users);
	
	@POST
	@Path("/users/running")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void setRunningRemote(List<AID> agentIds);
	
	@POST
	@Path("/notify/running")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void notifyAllRunning();
	
	@GET
	@Path("/agents/classes")
	public List<AgentType> getClasses();
	
	@POST
	@Path("/agents/classes")
	public void postClasses(List<AgentType> types);
}
