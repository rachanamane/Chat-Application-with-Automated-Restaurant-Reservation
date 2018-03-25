package ChatClient;

import java.awt.EventQueue;

import javax.swing.*;

/**
 * @author rachana
 */

@SuppressWarnings("serial")
public class ChatClientApp extends JFrame {

  public GuiSignin guiSignin = new GuiSignin();
  public GuiSignUp guiSignUp = new GuiSignUp();
  public GuiChatroom guiChatroom = new GuiChatroom();
  public Client client = new Client();
  public static ChatClientApp frame = null;

  /**
   * Launch the application.
   */
  public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
    javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());

    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          frame = new ChatClientApp();
          frame.setVisible(true);
          frame.client.setHost("localhost");
          frame.client.setPort(8080);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  /**
   * Create the frame.
   */
  public ChatClientApp() {
    setTitle("Sign in");
    setResizable(true);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setBounds(100, 100, 450, 300);
    add(guiChatroom);
    add(guiSignin);
    add(guiSignUp);
    setContentPane(guiSignin);
  }

}
