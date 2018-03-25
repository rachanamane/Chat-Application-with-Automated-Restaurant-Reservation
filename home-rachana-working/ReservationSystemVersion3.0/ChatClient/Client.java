package ChatClient;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import Protocol.Protocol;
import Protocol.SimpleProtocol;
import shared.User;

import static Protocol.ProtocolTags.*;

/**
 * @author rachana
 */
public class Client {

	private static final Protocol PROTOCOL = new SimpleProtocol();
	
	private Socket clientSocket;			// socket connecting to server
	private DataOutputStream outToServer;	// output stream to server
	private BufferedReader inFromServer;	// input stream from server
	private String host;		// IP address of server
	private Integer port;		// Port number of server
	
	/*
	 * 		Read a line from server and unpack it using SimpleProtocol
	 */
	public String[] getResponse() {
	  try {
        return PROTOCOL.decodeMessage(inFromServer.readLine());
		} catch (IOException e) {
			throw new RuntimeException("Server is closed!" + e.getMessage() + e);
		}
	}
	
	public String[] signup(String user, String pass) {
		sendMessageToServer(SIGN_UP_TAG, user, pass);
		return this.getResponse();
	}
	
	public String[] signin(String user, String pass){
		sendMessageToServer(SIGN_IN_TAG, user, pass);
		return this.getResponse();
	}

	public void get_message(User toUser, int offset) {
		sendMessageToServer(
				GET_MESSAGE_TAG,
				String.valueOf(toUser.getUserId()),
				String.valueOf(offset));
	}
	
	public void send_message(User toUser, String msg) {
		sendMessageToServer(SEND_MESSAGE_TAG, String.valueOf(toUser.getUserId()), msg);
	}

	public void show_online_users() {
		sendMessageToServer(SHOW_ONLINE_USERS_TAG);
	}

	public void book_slot(User restaurantUser, String date, String time){
		sendMessageToServer(BOOK_SLOT_TAG, String.valueOf(restaurantUser.getUserId()),date,time);
	}

	public void show_bookings(User toUser) {
		sendMessageToServer(SHOW_BOOKINGS_TAG,String.valueOf(toUser.getUserId()));
	}

	public void show_open_slots(User restaurantUser, String date){
		sendMessageToServer(SHOW_OPEN_SLOTS_TAG,String.valueOf(restaurantUser.getUserId()),date);
	}
	
	/*
	 * 		Initialize socket and input/output streams
	 */
	public void start(){
		try {
			clientSocket = new Socket(this.host, this.port);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			System.out.println(this.getResponse()[1]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		try {
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	private void sendMessageToServer(String... msg) {
		String encodedMsg = PROTOCOL.createMessage(msg);
		try {
			outToServer.writeBytes(encodedMsg + "\n");
		} catch (IOException e) {
			System.out.println("Unable to write to server: " + encodedMsg);
			e.printStackTrace();
		}
	}
}
