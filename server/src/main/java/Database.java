import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author rachana
 */
public class Database {

  private static final SimpleDateFormat TIME_FROM_TIMESTAMP = new SimpleDateFormat("HH:mm");
  private static final SimpleDateFormat DATE_FROM_TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd HH:mm");

  static Connection createConnection() {
    try {
//      Class.forName("com.mysql.jdbc.Driver");
      String dbURL = "jdbc:postgresql://localhost:5432/postgres";
      String username = "delphi";
      String password = "abjptrzejt";
      return DriverManager.getConnection(dbURL, username, password);
    } catch (Exception ex) {
      System.out.print("Connection to database failed");
      throw new RuntimeException(ex);
    }
  }

  static void closeConnection(Connection connection) {
    try {
      connection.close();
    } catch (SQLException e) {
      System.out.println("Connection close failed");
      throw new RuntimeException(e);
    }
  }

  /**
   * @param username
   * @param password
   * @return the UserId of the inserted row.
   */
  static User createUser(Connection connection, String username, String password) {
    try {
      String sql = "INSERT INTO Users (username, password) VALUES (?,?);";
      PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      preparedStatement.setString(1, username);
      preparedStatement.setString(2, password);
      preparedStatement.executeUpdate();

      ResultSet resultSet = preparedStatement.getGeneratedKeys();
      resultSet.next();
      int userId = resultSet.getInt(1);
      resultSet.close();
      preparedStatement.close();
      return new User(userId, username, User.UserType.human);
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException("User creation failed: " + e.getMessage(), e);
    }
  }

  /**
   * @param connection
   * @param username
   * @param password
   * @return userId of the user
   */
  static User authenticateUser(Connection connection, String username, String password) {
    try {
      String sql = "SELECT userId, password, type FROM Users WHERE username = ? and type = 'human'";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setString(1, username);
      ResultSet resultSet = preparedStatement.executeQuery();
      if (!resultSet.next()) {
        throw new RuntimeException("User not found");
      }
      if (!password.equals(resultSet.getString(2))) {
        throw new RuntimeException("Wrong password for " + username);
      }
      return new User(
          resultSet.getInt(1),
          username,
          User.UserType.valueOf(resultSet.getString(3)));
    } catch (SQLException e) {
      throw new RuntimeException("Error authenticating user", e);
    }
  }

  /**
   *
   * @param connection
   * @param fromUserId
   * @param toUserId
   * @param timestampMillis
   * @param content
   * @return
   */
  static Integer writeMessage(
      Connection connection,
      Integer fromUserId,
      Integer toUserId,
      long timestampMillis,
      String content) {
    try {
      String sql = "INSERT INTO Chats (userIdFrom, userIdTo, timestamp, content) VALUES (?,?,?,?);";
      PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      preparedStatement.setInt(1, fromUserId);
      preparedStatement.setInt(2, toUserId);
      preparedStatement.setTimestamp(3, new Timestamp(timestampMillis));
      preparedStatement.setString(4, content);
      preparedStatement.executeUpdate();

      ResultSet resultSet = preparedStatement.getGeneratedKeys();
      resultSet.next();
      int messageId = resultSet.getInt(1);
      resultSet.close();
      preparedStatement.close();
      return messageId;
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException("Failed to write message", e);
    }
  }

  /**
   *
   * @param connection
   * @param userId1
   * @param userId2
   * @param offsetMessageId
   * @return
   */
  static List<ChatMessage> getMessages(
      Connection connection,
      Integer userId1,
      Integer userId2,
      Integer offsetMessageId) {
    /**
     * SELECT c.messageId,
     *        c.userIdFrom,
     *        fu.username,
     *        c.userIdTo,
     *        tu.username,
     *        c.timestamp,
     *        c.content
     *   FROM Chats c
     *   JOIN Users fu ON (fu.userId = c.userIdFrom)
     *   JOIN Users tu ON (tu.userId = c.userIdTo)
     *  WHERE c.messageId > offsetMessageId
     *    AND ((c.userIdFrom = userId1 AND c.userIdTo = userId2)
     *       OR (c.userIdFrom = userId2 AND c.userIdTo = userId1));
     *
     */
    try {
      String sql = "SELECT c.messageId, " +
          "            c.userIdFrom, " +
          "            fu.username, " +
          "            fu.type, " +
          "            c.userIdTo, " +
          "            tu.username, " +
          "            tu.type, " +
          "            c.timestamp, " +
          "            c.content " +
          "       FROM Chats c " +
          "       JOIN Users fu ON (fu.userId = c.userIdFrom) " +
          "       JOIN Users tu ON (tu.userId = c.userIdTo) " +
          "      WHERE c.messageId > ? " +
          "        AND ((c.userIdFrom = ? AND c.userIdTo = ?) " +
          "           OR (c.userIdFrom = ? AND c.userIdTo = ?)) " +
          "   ORDER BY c.messageId";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setInt(1, offsetMessageId);
      preparedStatement.setInt(2, userId1);
      preparedStatement.setInt(3, userId2);
      preparedStatement.setInt(4, userId2);
      preparedStatement.setInt(5, userId1);
      ResultSet resultSet = preparedStatement.executeQuery();
      List<ChatMessage> messages = new ArrayList<>();
      while (resultSet.next()) {
        int messageId = resultSet.getInt(1);
        User userFrom = new User(
            resultSet.getInt(2),
            resultSet.getString(3),
            User.UserType.valueOf(resultSet.getString(4)));
        User userTo = new User(
            resultSet.getInt(5),
            resultSet.getString(6),
            User.UserType.valueOf(resultSet.getString(7)));
        long timestamp = resultSet.getTimestamp(8).getTime();
        String content = resultSet.getString(9);

        messages.add(new ChatMessage(
            messageId,
            userFrom,
            userTo,
            timestamp,
            content));
      }
      return messages;
    } catch (SQLException e) {
      throw new RuntimeException("Error authenticating user", e);
    }
  }

  /**
   *
   * @param connection
   * @return
   */
  static Connection showUsers(Connection connection) {
    try {
      String sql = "SELECT userId, username, password FROM Users";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      ResultSet resultSet = preparedStatement.executeQuery();
      int count = 0;
      while (resultSet.next()) {
        count++;
        System.out.println(
            String.format("%s: %s: %s",
                resultSet.getInt(1),
                resultSet.getString(2),
                resultSet.getString(3)));
      }
      if (count == 0) {
        System.out.println("No users");
      }
      return connection;
    } catch (SQLException e) {
      throw new RuntimeException("Error authenticating user", e);
    }
  }

  /**
   *
   * @param connection
   * @return
   */
  static List<User> getAllRestaurantUsers(Connection connection) {
    List<User> restaurantUsers = new ArrayList<>();
    try {
      String sql = "SELECT userId, username, type FROM Users WHERE type = 'restaurant'";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) {
        restaurantUsers.add(new User(resultSet.getInt(1),
            resultSet.getString(2),
            User.UserType.valueOf(resultSet.getString(3))));
      }
      return restaurantUsers;

    } catch (SQLException e) {
      throw new RuntimeException("Error getting restaurants", e);
    }
  }

  /**
   *
   * @param chatConnection
   * @param restaurantID
   * @param userId
   * @return
   */
  public static List<String> getBookings(Connection chatConnection, int restaurantID, int userId) {

    List<String> bookingTimes = new ArrayList<>();
    String sql = "SELECT bookingTime FROM Bookings WHERE restaurantUserID = ? AND humanUserID = ? ORDER BY bookingTime";
    try {
      PreparedStatement preparedStatement = chatConnection.prepareStatement(sql);
      preparedStatement.setInt(1, restaurantID);
      preparedStatement.setInt(2, userId);
      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) {
        Timestamp timestamp = resultSet.getTimestamp(1);
        Date dateFromTimestamp = new Date(timestamp.getTime());
        bookingTimes.add(DATE_FROM_TIMESTAMP.format(dateFromTimestamp));
      }
      return bookingTimes;
    } catch (SQLException e) {
      throw new RuntimeException("Error getting your bookings" + e.getMessage(), e);
    }

  }

  /**
   *
   * @param connection
   * @param restaurantUserID
   * @param timestamp
   * @param userID
   */
  static void bookSlot(Connection connection,
                       int restaurantUserID,
                       Timestamp timestamp,
                       int userID) {
    try {
      String sql = "UPDATE Bookings SET humanUserId = ? " +
          "WHERE restaurantUserID = ? AND bookingTime = ? AND humanuserid IS NULL";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setInt(1, userID);
      preparedStatement.setInt(2, restaurantUserID);
      preparedStatement.setTimestamp(3, timestamp);
      int updatedRecords = preparedStatement.executeUpdate();
      if (updatedRecords == 0) {
        throw new RuntimeException("This slot is already booked.");
      }

    } catch (SQLException e) {
      throw new RuntimeException("Error booking a slot for this time: " + e.getMessage(), e);
    }
  }


  /**
   *
   * @param connection
   * @param restaurantUserID
   * @param timestamp
   * @param userID
   */
  static void checkandBookSlot(Connection connection,
                               int restaurantUserID,
                               Timestamp timestamp, int userID) {
    try {
      if (timestamp.before(new Date())){
        throw new RuntimeException("Can't book slot in past. Please choose another slot.");
      }

      String sql = "SELECT humanUserID FROM Bookings WHERE restaurantUserID = ? AND bookingTime = ?";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setInt(1, restaurantUserID);
      preparedStatement.setTimestamp(2, timestamp);

      ResultSet resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) {
        int humanUserId = resultSet.getInt(1);
        if (humanUserId != 0) {
          throw new RuntimeException("This slot is already booked");
        } else {
          bookSlot(connection, restaurantUserID, timestamp, userID);
        }
      } else {
        throw new RuntimeException("No booking corresponding to given restaurant and time");
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error checking for available slots: " + e.getMessage(), e);
    }

  }

  /**
   *
   * @param connection
   * @param restaurantID
   * @param date
   * @return
   */
  static List<String> showOpenSlots(Connection connection, int restaurantID, java.sql.Date date) {
    if (date.before(new Date())) {
      throw new RuntimeException("Please choose date in present or future");
    }
    List<String> openSlotsTimes = new ArrayList<>();
    try {
      String sql = "SELECT bookingTime FROM Bookings " +
          "WHERE restaurantUserID = ? " +
          " AND bookingTime::date = ? " +
          " AND humanUserID IS NULL" +
          " ORDER BY bookingTime";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setInt(1, restaurantID);
      preparedStatement.setDate(2, date);
      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) {
        Timestamp timestamp = resultSet.getTimestamp(1);
        Date dateFromTimestamp = new Date(timestamp.getTime());
        openSlotsTimes.add(TIME_FROM_TIMESTAMP.format(dateFromTimestamp));
      }
      return openSlotsTimes;
    } catch (SQLException e) {
      throw new RuntimeException("Error getting booking times: " + e.getMessage(), e);
    }
  }
}
