package agents;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import chatmanager.ChatManagerRemote;
import messagemanager.AgentMessage;
import messagemanager.MessageManagerRemote;
import models.User;
import util.JNDILookup;
import ws.WSChat;

@Stateful
@Remote(Agent.class)
public class ChatAgent implements Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String agentId;

	//@EJB
	//private ChatManagerRemote chatManager;
	@EJB
	private CachedAgentsRemote cachedAgents;
	@EJB
	private WSChat ws;

	@PostConstruct
	public void postConstruct() {
		System.out.println("Created Chat Agent!");
	}

	//private List<String> chatClients = new ArrayList<String>();

	protected MessageManagerRemote msm() {
		return (MessageManagerRemote) JNDILookup.lookUp(JNDILookup.MessageManagerLookup, MessageManagerRemote.class);
	}

	@Override
	public void handleMessage(Message message) {
		/*TextMessage tmsg = (TextMessage) message;

		String receiver;
		try {
			receiver = (String) tmsg.getObjectProperty("receiver");
			if (agentId.equals(receiver)) {
				String option = "";
				String response = "";
				try {
					option = (String) tmsg.getObjectProperty("command");
					switch (option) {
					case "REGISTER":
						String username = (String) tmsg.getObjectProperty("username");
						String password = (String) tmsg.getObjectProperty("password");
	
						boolean result = chatManager.register(new User(username, password));

						response = "REGISTER!Registered: " + (result ? "Yes!" : "No!");
						break;
					case "LOG_IN":
						username = (String) tmsg.getObjectProperty("username");
						password = (String) tmsg.getObjectProperty("password");
						result = chatManager.login(username, password);
						if(result) {
							response = "LOG_IN!Logged in: Yes!" + username;
						} else {
							response = "LOG_IN!Logged in: No!";
						}
						break;
					case "GET_LOGGEDIN":
						response = "LOGGEDIN!";
						List<User> loggedUsers = chatManager.loggedInUsers();
						for (User u : loggedUsers) {
							response += u.toString() + "|";
						}

						break;
					case "GET_REGISTERED":
						response = "REGISTERED!";
						List<User> registeredUsers = chatManager.registeredUsers();
						for (User u : registeredUsers) {
							response += u.toString() + "|";
						}

						break;
					case "LOG_OUT":
						username = (String) tmsg.getObjectProperty("username");
						result = chatManager.logout(username);
						response = "LOG_OUT!Logged out: " + (result ? "Yes!" : "No!");
						break;
					case "x":
						break;
					default:
						response = "ERROR!Option: " + option + " does not exist.";
						break;
					}
					System.out.println(response);
					ws.onMessage("chat", response);
					
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}*/
	}

	@Override
	public String init(String agentId) {
		this.agentId = agentId;
		cachedAgents.addRunningAgent(agentId, this);
		return agentId;
	}

	@Override
	public String getAgentId() {
		return agentId;
	}
}
