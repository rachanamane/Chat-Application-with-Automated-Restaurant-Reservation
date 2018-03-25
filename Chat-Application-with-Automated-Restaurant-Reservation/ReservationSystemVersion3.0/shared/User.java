package shared;

public final class User implements Comparable<User> {

  public enum UserType {
    human,
    restaurant,
  }

  public static final Integer FIELD_COUNT = 3;

  private final Integer userId;
  private final String username;
  private final UserType userType;

  public User(Integer userId, String username, UserType userType) {
    this.userId = userId;
    this.username = username;
    this.userType = userType;
  }

  public int getUserId() {
    return userId;
  }

  public String getUsername() {
    return username;
  }

  @Override
  public int compareTo(User o) {
      int firstCompare = this.userType.compareTo(o.userType);
      if (firstCompare == 0) {
        return username.compareTo(o.username);
      }
      return firstCompare;
  }

  @Override
  public String toString() {
    return String.format("User: %s - %s", userId, username);
  }

  public UserType getUserType() {
    return userType;
  }
}

