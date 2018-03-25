package ChatClient;

import shared.User;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rachana
 */
public class ChatroomModel {
  private int offset = -1;
  private User loggedInUser;
  private User toUser;
  private final List<User> onlineUsers = new ArrayList<>();

  public User getToUser() {
    return toUser;
  }

  public void setToUser(User toUser) {
    this.toUser = toUser;
  }

  public void addOnlineUser(User user) {
    onlineUsers.add(user);
  }

  public void clearOnlineUsers() {
    onlineUsers.clear();
  }

  public List<User> getOnlineUsers() {
    return onlineUsers;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public User getLoggedInUser() {
    return loggedInUser;
  }

  public void setLoggedInUser(User loggedInUser) {
    this.loggedInUser = loggedInUser;
  }
}
