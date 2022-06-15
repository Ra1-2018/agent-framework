package rest;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import agentmanager.AgentManagerRemote;
import agents.AID;
import agents.AgentType;
import chatmanager.ChatManagerRemote;
import messagemanager.ACLMessage;
import messagemanager.MessageManagerRemote;
import models.User;
import util.JNDILookup;

@Stateless
@LocalBean
@Path("/users")
public class ChatRestBean implements ChatRest, ChatRestLocal {

	@EJB
	private MessageManagerRemote messageManager;
	
	@EJB
	private ChatManagerRemote chatManager;
	
	@EJB
	private AgentManagerRemote agentManager;
	
	@Override
	public Response register(User user) {
		if(!chatManager.register(user)) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		for(User u : chatManager.loggedInUsers()) {
			if(!u.getHost().getAlias().equals(getNodeAlias() + ":8080")) {
				continue;
			}
			ACLMessage message = new ACLMessage();
			message.receivers.add(new AID(u.getUsername(), u.getHost(), new AgentType("UserAgent")));
			message.userArgs.put("command", "GET_REGISTERED");
			messageManager.post(message);
		}
		return Response.status(Response.Status.CREATED).entity(user).build();
	}

	@Override
	public Response login(User user) {
		if(!chatManager.login(user)) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		user = chatManager.getByUsername(user.getUsername());
		AID agentId = new AID(user.getUsername(), user.getHost(), new AgentType("UserAgent"));
		agentManager.startAgent(JNDILookup.UserAgentLookup, agentId);
		for(User u : chatManager.loggedInUsers()) {
			if(!u.getHost().getAlias().equals(getNodeAlias() + ":8080")) {
				continue;
			}
			ACLMessage message = new ACLMessage();
			message.receivers.add(new AID(u.getUsername(), u.getHost(), new AgentType("UserAgent")));
			message.userArgs.put("command", "GET_LOGGEDIN");
			messageManager.post(message);
		}
		return Response.status(Response.Status.OK).entity(user).build();
	}

	@Override
	public void getloggedInUsers(String username) {
		User user = chatManager.getByUsername(username);
		ACLMessage message = new ACLMessage();
		message.receivers.add(new AID(user.getUsername(), user.getHost(), new AgentType("UserAgent")));
		message.userArgs.put("command", "GET_LOGGEDIN");
		messageManager.post(message);
	}

	@Override
	public Response logout(String username) {
		User user = chatManager.getByUsername(username);
		AID agentId = new AID(user.getUsername(), user.getHost(), new AgentType("UserAgent"));
		if(!chatManager.logout(username)) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		agentManager.stopAgent(agentId);
		for(User u : chatManager.loggedInUsers()) {
			if(!u.getHost().getAlias().equals(getNodeAlias() + ":8080")) {
				continue;
			}
			ACLMessage message = new ACLMessage();
			message.receivers.add(new AID(u.getUsername(), u.getHost(), new AgentType("UserAgent")));
			message.userArgs.put("command", "GET_LOGGEDIN");
			messageManager.post(message);
		}
		return Response.status(Response.Status.OK).build();
	}

	@Override
	public void getRegisteredUsers(String username) {
		User user = chatManager.getByUsername(username);
		ACLMessage message = new ACLMessage();
		message.receivers.add(new AID(user.getUsername(), user.getHost(), new AgentType("UserAgent")));
		message.userArgs.put("command", "GET_REGISTERED");
		messageManager.post(message);
	}
	
	private String getNodeAlias() {		
		return System.getProperty("jboss.node.name");
	}
}
