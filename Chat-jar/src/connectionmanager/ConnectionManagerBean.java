package connectionmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.Path;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import agents.AID;
import agents.AgentType;
import agents.AgentTypesRemote;
import agents.CachedAgentsRemote;
import chatmanager.ChatManagerRemote;
import messagemanager.ACLMessage;
import messagemanager.MessageManagerRemote;
import models.User;
import util.AgentCenter;
import util.FileUtils;
import ws.WSChat;

@Singleton
@Startup
@Remote(ConnectionManager.class)
@Path("/connection")
public class ConnectionManagerBean implements ConnectionManager {

	private String nodeAddress;
	private String nodeAlias;
	private List<String> connections = new ArrayList<>();
	private String masterAlias = null;
	
	@EJB
	private ChatManagerRemote chatManager;
	
	@EJB
	private WSChat ws;
	
	@EJB
	private MessageManagerRemote messageManager;
	
	@EJB
	private CachedAgentsRemote cachedAgents;
	
	@EJB
	private AgentTypesRemote agentTypes;
	
	@PostConstruct
	private void init() {
		nodeAddress = AgentCenter.getNodeAddress();
		nodeAlias = AgentCenter.getNodeAlias();
		masterAlias = getMasterAlias();
		System.out.println("MASTER ADDR: " + masterAlias + ", node name: " + nodeAlias + ", node address: " + nodeAddress);
		if (masterAlias != null && !masterAlias.equals("")) {
			ResteasyClient client = new ResteasyClientBuilder().build();
			ResteasyWebTarget rtarget = client.target("http://" + masterAlias + "/Chat-war/api/connection");
			ConnectionManager rest = rtarget.proxy(ConnectionManager.class);
			connections = rest.registerNode(nodeAlias);
			connections.remove(nodeAlias);
			connections.add(masterAlias);
			client.close();
			System.out.println("Number of connected nodes: " + connections.size());
		}
		else {
			System.out.println("Master node started");
		}

	}
	
	private String getMasterAlias() {
		try {
			File f = FileUtils.getFile(ConnectionManager.class, "", "connections.properties");
			FileInputStream fileInput = new FileInputStream(f);
			Properties properties = new Properties();
			properties.load(fileInput);
			fileInput.close();
			return properties.getProperty("master");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public List<String> registerNode(String nodeAlias) {
		System.out.println("New node registered: " + nodeAlias);
		for (String c : connections) {
			ResteasyClient client = new ResteasyClientBuilder().build();
			ResteasyWebTarget rtarget = client.target("http://" + c + "/Chat-war/api/connection");
			ConnectionManager rest = rtarget.proxy(ConnectionManager.class);
			rest.addNode(nodeAlias);
			client.close();
		}
		connections.add(nodeAlias);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				ResteasyClient resteasyClient = new ResteasyClientBuilder().build();
				ResteasyWebTarget rtarget = resteasyClient.target("http://"  + nodeAlias + "/Chat-war/api/connection");
				ConnectionManager rest = rtarget.proxy(ConnectionManager.class);
				rest.setLoggedInRemote(chatManager.loggedInUsers());
				rest.setRegisteredRemote(chatManager.registeredUsers());
				rest.setRunningRemote(cachedAgents.getAllAgents());
				List<AgentType> types = rest.getClasses();
				System.out.println("Number of agent classes: " + types.size());
				agentTypes.addAgentTypes(types);
				notifyUserAgents("GET_CLASSES");
				resteasyClient.close();
				notifyAllAgentClasses();
			}
		}).start();
		return connections;
	}

	private void notifyAllAgentClasses() {
		for(String c : connections) {
			ResteasyClient client = new ResteasyClientBuilder().build();
			ResteasyWebTarget rtarget = client.target("http://" + c + "/Chat-war/api/connection");
			ConnectionManager rest = rtarget.proxy(ConnectionManager.class);
			rest.postClasses(agentTypes.getAgentTypes());
			client.close();
		}
	}
	
	@Override
	public void addNode(String nodeAlias) {
		System.out.println("New node added: " + nodeAlias);
		connections.add(nodeAlias);
	}

	@Override
	public void deleteNode(String nodeAlias) {
		System.out.println("Node removed: " + nodeAlias);
		connections.remove(nodeAlias);
		chatManager.logoutNode(nodeAlias);
		cachedAgents.removeNode(nodeAlias);
		agentTypes.removeNode(nodeAlias);
		notifyUserAgents("GET_LOGGEDIN");
		notifyUserAgents("GET_RUNNING");
		notifyUserAgents("GET_CLASSES");
	}

	@Override
	public String pingNode() {
		System.out.println("Node pinged");
		return "OK";
	}
	
	@PreDestroy
	private void shutDown() {
		notifyAllDelete(nodeAlias);
	}
	
	
	private void notifyAllDelete(String alias) {
		for(String c : connections) {
			ResteasyClient client = new ResteasyClientBuilder().build();
			ResteasyWebTarget rtarget = client.target("http://" + c + "/Chat-war/api/connection");
			ConnectionManager rest = rtarget.proxy(ConnectionManager.class);
			rest.deleteNode(alias);
			client.close();
		}
	}

	@Schedule(hour = "*", minute="*/1", persistent=false)
	private void heartbeat() {
		System.out.println("Heartbeat protocol started");
		for(String c : connections) {
			System.out.println("Pinging node: " + c);
			new Thread(new Runnable() {
				@Override
				public void run() {
					if(!nodeResponded(c)) {
						System.out.println("Deleting node: " + c);
						deleteNode(c);
						notifyAllDelete(c);
					}
				}
			}).start();
		}
	}
	
	private boolean nodeResponded(String alias) {
		for(int i=0; i<2; i++) {
			try {
				ResteasyClient client = new ResteasyClientBuilder().build();
				ResteasyWebTarget rtarget = client.target("http://" + alias + "/Chat-war/api/connection");
				ConnectionManager rest = rtarget.proxy(ConnectionManager.class);
				String response = rest.pingNode();
				client.close();
				if(response.equals("OK")) {
					return true;
				}
			} catch(Exception e) {
				System.out.println("Node: " + alias + " not responding");
			}
		}
		return false;
	}

	@Override
	public void notifyAllLoggedIn() {
		for (String c: connections) {
			System.out.println("Sending logged users to node: " + c);
			ResteasyClient resteasyClient = new ResteasyClientBuilder().build();
			ResteasyWebTarget rtarget = resteasyClient.target("http://" + c + "/Chat-war/api/connection");
			ConnectionManager rest = rtarget.proxy(ConnectionManager.class);
			rest.setLoggedInRemote(chatManager.loggedInUsers());
			resteasyClient.close();
		}		
	}

	@Override
	public void setLoggedInRemote(List<User> users) {
		System.out.println("Number of logged users: " + users.size());
		chatManager.setLoggedInUsers(users);
		notifyUserAgents("GET_LOGGEDIN");
	}

	@Override
	public void notifyAllRegistered() {
		for (String c: connections) {
			System.out.println("Sending registered users to node: " + c);
			ResteasyClient resteasyClient = new ResteasyClientBuilder().build();
			ResteasyWebTarget rtarget = resteasyClient.target("http://" + c + "/Chat-war/api/connection");
			ConnectionManager rest = rtarget.proxy(ConnectionManager.class);
			rest.setRegisteredRemote(chatManager.registeredUsers());
			resteasyClient.close();
		}	
	}

	@Override
	public void setRegisteredRemote(List<User> users) {
		System.out.println("Number of registered users: " + users.size());
		chatManager.setRegisteredUsers(users);
		notifyUserAgents("GET_REGISTERED");
	}

	@Override
	public void setRunningRemote(List<AID> agentIds) {
		System.out.println("Number of running agents: " + agentIds.size());
		cachedAgents.setAllAgents(agentIds);
		notifyUserAgents("GET_RUNNING");
	}

	@Override
	public void notifyAllRunning() {
		for (String c: connections) {
			System.out.println("Sending running users to node: " + c);
			ResteasyClient resteasyClient = new ResteasyClientBuilder().build();
			ResteasyWebTarget rtarget = resteasyClient.target("http://" + c + "/Chat-war/api/connection");
			ConnectionManager rest = rtarget.proxy(ConnectionManager.class);
			rest.setRunningRemote(cachedAgents.getAllAgents());
			resteasyClient.close();
		}	
	}
	
	private void notifyUserAgents(String command) {
		ACLMessage message = new ACLMessage();
		for(User u : chatManager.loggedInUsers()) {
			if(u.getHost().getAlias().equals(AgentCenter.getNodeAlias())) {
				message.receivers.add(new AID(u.getUsername(), new AgentType("UserAgent", u.getHost())));
			}
		}
		message.userArgs.put("command", command);
		messageManager.post(message);
	}

	@Override
	public List<AgentType> getClasses() {
		return agentTypes.getAgentTypes();
	}

	@Override
	public void postClasses(List<AgentType> types) {
		System.out.println("Number of agent classes: " + types.size());
		agentTypes.setAgentTypes(types);
		notifyUserAgents("GET_CLASSES");
	}

	@Override
	public List<String> getConnections() {
		return connections;
	}
	
}
