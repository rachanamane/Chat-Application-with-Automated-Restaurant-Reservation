
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author rachana
 */
public class Server {

  public static void main(String[] args) throws SQLException, ClassNotFoundException {
    Connection connection = Database.createConnection();
    List<User> restaurantUsers = Database.getAllRestaurantUsers(connection);
    connection.close();

    try {
      ServerSocket serverSocket = new ServerSocket(8080);

      ExecutorService service = Executors.newCachedThreadPool();

      // List shared between all threads containing all chat messages.
      final List<ClientThread> allClients = Collections.synchronizedList(new ArrayList<>());

      final Set<User> onlineUsers = Collections.synchronizedSet(new TreeSet<>());
      onlineUsers.addAll(restaurantUsers);

      while (true) {
        final Socket socket = serverSocket.accept();
        ClientThread clientThread = new ClientThread(
            socket,
            onlineUsers,
            allClients);
        allClients.add(clientThread);
        service.submit(clientThread);
      }



    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
