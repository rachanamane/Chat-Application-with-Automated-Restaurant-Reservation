package ChatServer;

import Protocol.Protocol;
import Protocol.SimpleProtocol;
import shared.ChatMessage;
import shared.User;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static Protocol.ProtocolTags.*;

/**
 * @author rachana
 */
final class ClientThread implements Runnable {

  private static final Protocol PROTOCOL = new SimpleProtocol();

  // Date format for chat window output.
  private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private final Socket clientSocket;
  private final Set<User> onlineUsers;
  private final List<ClientThread> allClients;

  private DataOutputStream outToClientStream = null;
  private User user = null;

  private Connection chatConnection = null;
  private static boolean isOn;

  ClientThread(Socket clientSocket,
               Set<User> onlineUsers,
               List<ClientThread> allClients) {
    this.clientSocket = clientSocket;
    this.onlineUsers = onlineUsers;
    this.allClients = allClients;
  }

  @Override
  public void run() {
    try {
      isOn = true;
      outToClientStream = new DataOutputStream(clientSocket.getOutputStream());
      BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      // Necessary to print the response in Client.start() method.
      sendMessageToClient("client-server-connection", "Client-server connection established");

      chatConnection = Database.createConnection();
      String input;
      while ((input = inFromClient.readLine()) != null) {
        handleInput(input);
      }

      // Close everything after client disconnects:
      if (user != null) {
        onlineUsers.remove(user);
      }
      sendUserChangeUpdateToAllClients();

      user = null;
      allClients.remove(this);

      // closing all streams
      Database.closeConnection(chatConnection);
      outToClientStream.close();
      inFromClient.close();
      clientSocket.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected static void stop(){
    isOn = false;
    try {
      new Socket("localhost", 8080);
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private void handleInput(String input) throws IOException {
    String[] decodedInput = PROTOCOL.decodeMessage(input);
    switch (decodedInput[0]) {
      case SIGN_UP_TAG:
        handleSignUp(decodedInput[1], decodedInput[2]);
        break;
      case SIGN_IN_TAG:
        handleSignIn(decodedInput[1], decodedInput[2]);
        break;
      case GET_MESSAGE_TAG:
        handleGetMessage(decodedInput[1], decodedInput[2]);
        break;
      case SEND_MESSAGE_TAG:
        handleSendMessage(decodedInput[1], decodedInput[2]);
        break;
      case SHOW_ONLINE_USERS_TAG:
        handleShowOnlineUsers();
        break;
      case SHOW_OPEN_SLOTS_TAG:
        handleShowOpenSlots(decodedInput[1], decodedInput[2]);
        break;
      case BOOK_SLOT_TAG:
        handleBookSlot(decodedInput[1], decodedInput[2], decodedInput[3]);
        break;
      case SHOW_BOOKINGS_TAG:
        handleShowBookings(decodedInput[1]);
        break;
      default:
        sendMessageToClient(decodedInput[0], FALSE, "Action not recognized: " + decodedInput[0]);
        break;
    }
  }

  private void handleShowBookings(String restaurantUserIDString) throws IOException {
    try {
      int restaurantID = Integer.parseInt(restaurantUserIDString);
      List<String> bookings = Database.getBookings(chatConnection, restaurantID, this.user.getUserId());
      String[] arguments = new String[bookings.size() + 2];
      arguments[0] = SHOW_BOOKINGS_TAG;
      arguments[1] = TRUE;
      int i = 2;
      for (String s : bookings) {
        arguments[i++] = s;
      }
      sendMessageToClient(arguments);
    } catch (RuntimeException e) {
      sendMessageToClient(SHOW_BOOKINGS_TAG, FALSE, e.getMessage());
    }
  }

  private void handleBookSlot(String restaurantUserIDString, String date, String time) throws IOException {
    String formattedTime = date + " " + time + ":00";
    java.sql.Timestamp timestamp;
    try {
      timestamp = java.sql.Timestamp.valueOf(formattedTime);
    } catch (IllegalArgumentException e) {
      sendMessageToClient(BOOK_SLOT_TAG, FALSE,
          String.format("Invalid date/time: %s %s", date, time));
      return;
    }

    try {
      int restaurantID = Integer.parseInt(restaurantUserIDString);
      Database.checkandBookSlot(chatConnection, restaurantID, timestamp, this.user.getUserId());
      sendMessageToClient(BOOK_SLOT_TAG, TRUE, date, time);
    } catch (RuntimeException e) {
      sendMessageToClient(BOOK_SLOT_TAG, FALSE, e.getMessage());
    }

  }

  private void handleShowOpenSlots(String restaurantUserIDString, String dateString) throws IOException {
    java.sql.Date date;
    try {
      date = java.sql.Date.valueOf(dateString);
    } catch (IllegalArgumentException e) {
      sendMessageToClient(
          SHOW_OPEN_SLOTS_TAG,
          FALSE,
          String.format("Invalid date %s. Please enter yyyy-mm-dd", dateString));
      return;
    }
    try {
      int restaurantID = Integer.parseInt(restaurantUserIDString);
      List<String> openTimeSlots = Database.showOpenSlots(chatConnection, restaurantID, date);
      String[] arguments = new String[openTimeSlots.size() + 3];
      arguments[0] = SHOW_OPEN_SLOTS_TAG;
      arguments[1] = TRUE;
      arguments[2] = dateString;
      int i = 3;
      for (String s : openTimeSlots) {
        arguments[i++] = s;
      }
      sendMessageToClient(arguments);
    } catch (RuntimeException e) {
      sendMessageToClient(SHOW_OPEN_SLOTS_TAG, FALSE, e.getMessage());
    }
  }

  private void handleShowOnlineUsers() throws IOException {
    sendUserChangeUpdate();
  }

  private void handleSignUp(String username, String password) throws IOException {
    if (username.length() < 5) {
      sendMessageToClient(SIGN_UP_TAG, FALSE, "username should be at least 5 characters");
    } else if (username.length() > 20) {
      sendMessageToClient(SIGN_UP_TAG, FALSE, "username should be at most 20 characters");
    } else if (password.length() < 8) {
      sendMessageToClient(SIGN_UP_TAG, FALSE, "password should be at least 8 characters");
    } else if (password.length() > 32) {
      sendMessageToClient(SIGN_UP_TAG, FALSE, "password should be at most 32 characters");
    } else if (password.contains(" ")) {
      sendMessageToClient(SIGN_UP_TAG, FALSE, "password should not contain space");
    }
    else {
      try {
        user = Database.createUser(chatConnection, username, password);
        sendMessageToClient(SIGN_UP_TAG, TRUE, "Sign-up successful. Please sign-in");
      } catch (RuntimeException e) {
        sendMessageToClient(SIGN_UP_TAG, FALSE, e.getMessage());
      }

    }
  }

  private void handleSignIn(String username, String password) throws IOException {
    try {
      this.user = Database.authenticateUser(chatConnection, username, password);
      onlineUsers.add(user);
      sendMessageToClient(
          SIGN_IN_TAG,
          TRUE,
          String.valueOf(this.user.getUserId()),
          this.user.getUserType().name(),
          "Welcome back " + username);
      sendUserChangeUpdateToAllClients();
    } catch (RuntimeException e) {
      sendMessageToClient(SIGN_IN_TAG, FALSE, e.getMessage());
    }
  }

  private void handleGetMessage(String toUserString, String offsetString) throws IOException {
    int toUserId = Integer.parseInt(toUserString);
    int offset = Integer.parseInt(offsetString);

    List<ChatMessage> messagesToSend = Database.getMessages(
        chatConnection,
        this.user.getUserId(),
        toUserId,
        offset);
    if (messagesToSend.isEmpty()) {
      return;
    }
    // 4 values per message, adding "+1" for the first GET_MESSAGE_TAG string.
    String[] arguments = new String[(4 * messagesToSend.size()) + 3];
    arguments[0] = GET_MESSAGE_TAG;
    arguments[1] = String.valueOf(this.user.getUserId());
    arguments[2] = toUserString;
    int index = 3;
    for (ChatMessage chatMessage : messagesToSend) {
      arguments[index++] = String.valueOf(chatMessage.getMessageId());   // offset
      arguments[index++] = chatMessage.getFromUser().getUsername();
      arguments[index++] = DATE_FORMATTER.format(new Date(chatMessage.getTimestamp()));
      arguments[index++] = chatMessage.getContent();
    }
    sendMessageToClient(arguments);
  }

  private void handleSendMessage(String toUserString, String newChatMessage) throws IOException {
    int toUserId = Integer.parseInt(toUserString);
    if (newChatMessage.isEmpty()) {
      return;
    }
    try {
      int messageId = Database.writeMessage(
          chatConnection,
          this.user.getUserId(),
          toUserId,
          System.currentTimeMillis(),
          newChatMessage);
      sendMessageToClient(SEND_MESSAGE_TAG, TRUE, String.valueOf(messageId));
      sendMessageToRelevantReceivers(toUserId, messageId - 1);
    } catch (Exception e) {
      sendMessageToClient(SEND_MESSAGE_TAG, FALSE, "Failed to send message: " + e.getMessage());
    }
  }

  private void sendMessageToRelevantReceivers(int toUserId, int offset) throws IOException {
    for (ClientThread clientThread : allClients) {
      if (clientThread.isSignedIn()) {
        if ((clientThread.user.getUserId() == toUserId)) {
          clientThread.handleGetMessage(
              String.valueOf(this.user.getUserId()),
              String.valueOf(offset));
        }
        if (clientThread.user.getUserId() == this.user.getUserId()) {
          clientThread.handleGetMessage(
              String.valueOf(toUserId),
              String.valueOf(offset));
        }
      }
    }
  }

  private void sendUserChangeUpdateToAllClients() throws IOException {
    for (ClientThread clientThread : allClients) {
      if (clientThread.isSignedIn()) {
        if (!clientThread.user.equals(this.user)) {
          clientThread.sendUserChangeUpdate();
        }
      }
    }
  }

  private void sendUserChangeUpdate() throws IOException {
    Integer usersToReturnCount = onlineUsers.size() - 1;
    String[] args = new String[1 + (User.FIELD_COUNT * usersToReturnCount)];
    args[0] = SHOW_ONLINE_USERS_TAG;
    int index = 1;
    for (User user : onlineUsers) {
      if (user.getUserId() != this.user.getUserId()) {
        args[index++] = String.valueOf(user.getUserId());
        args[index++] = user.getUsername();
        args[index++] = user.getUserType().name();
      }
    }
    sendMessageToClient(args);
  }

  private void sendMessageToClient(String... msg) throws IOException {
    outToClientStream.writeBytes(PROTOCOL.createMessage(msg) + "\n");
  }

  boolean isSignedIn() {
    return (user != null);
  }
}
