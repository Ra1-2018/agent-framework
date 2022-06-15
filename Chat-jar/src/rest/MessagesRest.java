package rest;

import javax.ejb.Remote;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import models.Message;

@Remote
public interface MessagesRest {
	@POST
	@Path("/all")
	@Consumes(MediaType.APPLICATION_JSON)
	public void messageAll(Message message);
	
	@POST
	@Path("/user")
	@Consumes(MediaType.APPLICATION_JSON)
	public void messageUser(Message message);
	
	@GET
	@Path("/{user}")
	public void getUserMessages(@PathParam("user") String username);
}
