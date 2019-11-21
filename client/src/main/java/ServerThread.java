import javax.swing.*;

import java.util.*;

/**
 * @author rachana
 */
public class ServerThread extends Thread {

  private final JList<String> listChat;
  private final JList<String> listOnlineUsers;
  private final ChatroomModel chatroomModel;

  ServerThread(JList<String> listChat,
               JList<String> listOnlineUsers,
               ChatroomModel chatroomModel) {
    this.listChat = listChat;
    this.listOnlineUsers = listOnlineUsers;
    this.chatroomModel = chatroomModel;
  }

  @Override
  public void run() {
    while (true) {
      try {
        String[] response = ChatClientApp.frame.client.getResponse();
        handleServerResponse(response);
      } catch (RuntimeException e){

        ChatClientApp.frame.setBounds(0, 0, 450, 278);
        ChatClientApp.frame.setContentPane(
            ChatClientApp.frame.guiSignin);
        ChatClientApp.frame.setTitle("Sign in");


        JOptionPane.showMessageDialog(ChatClientApp.frame,
            "Oops! Serve is closed!",
            "Error",
            JOptionPane.ERROR_MESSAGE);
        break;
      }
    }
  }

  private void handleServerResponse(String[] response) {
    switch (response[0]) {
      case ProtocolTags.GET_MESSAGE_TAG:
        handleGetMessage(response);
        break;
      case ProtocolTags.SEND_MESSAGE_TAG:
        handleSendMessage(response[1]);
        break;
      case ProtocolTags.SHOW_ONLINE_USERS_TAG:
        handleShowOnlineUsers(response);
        break;
      case ProtocolTags.SHOW_OPEN_SLOTS_TAG:
        handleShowOpenSlots(response);
        break;
      case ProtocolTags.BOOK_SLOT_TAG:
        handleBookSlot(response);
        break;
      case ProtocolTags.SHOW_BOOKINGS_TAG:
        handleShowBookings(response);
        break;
      default:
        System.out.println("Unhandled yet: " + response[0]);
    }
  }

  private void handleShowBookings(String[] response) {
    DefaultListModel<String> model = (DefaultListModel<String>) listChat.getModel();
    if (response[1].equals(ProtocolTags.TRUE)) {
      if (response.length == 2) {
        displayBookingsServerResponse(model, "You don't have any bookings");
        return;
      }
      Map<String, StringBuilder> bookingsByDate = new TreeMap<>();
      for (int i = 2; i < response.length; i++) {
        String[] parts = response[i].split(" ");
        StringBuilder bookingsForDate = bookingsByDate.get(parts[0]);
        if (bookingsForDate == null) {
          bookingsForDate = new StringBuilder();
          bookingsByDate.put(parts[0], bookingsForDate);
        }
        bookingsForDate.append(parts[1]);
        bookingsForDate.append(", ");
      }
      for (Map.Entry<String, StringBuilder> entry : bookingsByDate.entrySet()) {
        StringBuilder times = entry.getValue();
        // Remove last ", "
        times.delete(times.length() - 2, times.length());
        displayBookingsServerResponse(model,
            String.format(
                "Bookings at %s on %s: %s",
                chatroomModel.getToUser().getUsername(),
                entry.getKey(),
                times.toString()));
      }
    } else {
      displayBookingsServerResponse(model, response[2]);
    }
  }

  private void handleBookSlot(String[] response) {
    DefaultListModel<String> model = (DefaultListModel<String>) listChat.getModel();
    if (response[1].equals(ProtocolTags.TRUE)) {
      displayBookingsServerResponse(model, String.format("Booking confirmed at %s at %s %s",
          chatroomModel.getToUser().getUsername(),
          response[2], response[3]));
    } else {
      displayBookingsServerResponse(model, response[2]);
    }
  }

  private void handleGetMessage(String[] response) {
    DefaultListModel<String> model = (DefaultListModel<String>) listChat.getModel();
    int userId1 = Integer.parseInt(response[1]);
    int userId2 = Integer.parseInt(response[2]);
    if (!shouldShowMessages(userId1, userId2)) {
      return;
    }
    for (int i = 3; i < response.length; i = i + 4) {
      if (chatroomModel.getOffset() < Integer.parseInt(response[i])) {
        chatroomModel.setOffset(Integer.parseInt(response[i]));
        ListModelHelper.addToModel(model,
            String.format("%s @ (%s): %s",
                response[i + 1],
                response[i + 2],
                response[i + 3]));
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private boolean shouldShowMessages(int userId1, int userId2) {
    if (chatroomModel.getLoggedInUser() == null) {
      return false;
    }
    if (chatroomModel.getToUser() == null) {
      return false;
    }
    boolean first = (chatroomModel.getLoggedInUser().getUserId() == userId1)
        && (chatroomModel.getToUser().getUserId() == userId2);
    boolean second = (chatroomModel.getLoggedInUser().getUserId() == userId2)
        && (chatroomModel.getToUser().getUserId() == userId1);
    return first || second;
  }

  private void handleSendMessage(String status) {
    if (status.equals(ProtocolTags.TRUE)) {
      System.out.println("Message sent.");
    } else {
      JOptionPane.showMessageDialog(ChatClientApp.frame,
          "Cannot send message!",
          "Error",
          JOptionPane.WARNING_MESSAGE);
    }
  }

  private void handleShowOpenSlots(String[] response) {
    DefaultListModel<String> model = (DefaultListModel<String>) listChat.getModel();
    if (ProtocolTags.FALSE.equals(response[1])) {
      displayBookingsServerResponse(model, String.format("Error: %s", response[2]));
      return;
    }
    String date = response[2];
    if (response.length == 3) {
      displayBookingsServerResponse(model,
          String.format(
              "No open slots at %s for %s",
              chatroomModel.getToUser().getUsername(),
              date));
    } else {
      StringBuilder sb = new StringBuilder();
      for (int i = 3; i < response.length - 1; i++) {
        sb.append(response[i]);
        sb.append(", ");
      }
      sb.append(response[response.length - 1]);
      String slotsString = sb.toString();
      displayBookingsServerResponse(model, String.format("Open slots for %s: %s", date, slotsString));
      displayBookingsServerResponse(model, String.format(
          "To reserve a slot, type: %s <date> <time>. E.g. %s 2018-03-17 17:30",
          ProtocolTags.BOOK_SLOT_TAG,
          ProtocolTags.BOOK_SLOT_TAG));
    }
  }

  private void handleShowOnlineUsers(String[] response) {
    DefaultListModel<String> model = (DefaultListModel<String>) listOnlineUsers.getModel();
    model.clear();
    chatroomModel.clearOnlineUsers();

    for (int i = 1; i < response.length; i = i + User.FIELD_COUNT) {
      User currentUser = new User(
          Integer.parseInt(response[i]),
          response[i + 1],
          User.UserType.valueOf(response[i + 2]));
      chatroomModel.addOnlineUser(currentUser);
      if (currentUser.getUserType() == User.UserType.human) {
        ListModelHelper.addToModel(model, response[i + 1]);
      } else {
        ListModelHelper.addToModel(model, response[i + 1] + " - Restaurant");
      }
    }
  }


  private void displayBookingsServerResponse(DefaultListModel<String> model, String string) {
    ListModelHelper.addToModel(model, String.format("@%s: %s", chatroomModel.getToUser().getUsername(), string));
  }
}
