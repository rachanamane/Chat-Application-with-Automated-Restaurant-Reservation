package ChatClient;

import shared.User;
import shared.User.UserType;

import javax.swing.*;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import static Protocol.ProtocolTags.TRUE;
import static shared.User.UserType.human;

/**
 * @author rachana
 */

@SuppressWarnings("serial")
public class GuiSignin extends JPanel {
  private JTextField txUsername;
  private JTextField txPassword;

  /**
   * Create the panel.
   */
  public GuiSignin() {
    setLayout(null);
    this.setBounds(0, 0, 450, 278);

    JLabel lblSignUp = new JLabel("Sign In");
    lblSignUp.setBounds(34, 25, 61, 16);
    add(lblSignUp);

    JLabel lblUsername = new JLabel("Username:");
    lblUsername.setBounds(34, 73, 79, 16);
    add(lblUsername);

    txUsername = new JTextField();
    txUsername.setBounds(118, 68, 130, 26);
    add(txUsername);
    txUsername.setColumns(10);

    JLabel lblPassword = new JLabel("Password:");
    lblPassword.setBounds(34, 114, 79, 16);
    add(lblPassword);

    txPassword = new JPasswordField();
    txPassword.setBounds(118, 109, 130, 26);
    add(txPassword);
    txPassword.setColumns(10);

    JButton btnNewButton = new JButton("Sign In");
    btnNewButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ChatClientApp.frame.client.start();
        String username = txUsername.getText();
        String password = txPassword.getText();
        if (username.equals("") || password.equals("")) {
          JOptionPane.showMessageDialog(ChatClientApp.frame,
              "Both username and password can not be empty!",
              "Warning",
              JOptionPane.WARNING_MESSAGE);
          return;
        }
        String[] response = ChatClientApp.frame.client.signin(username, password);
        if (response[1].equals(TRUE)) {
          ChatClientApp.frame.guiChatroom.setLoggedInUser(
              new User(
                  Integer.parseInt(response[2]),
                  username,
                  UserType.valueOf(response[3])));
          ChatClientApp.frame.setBounds(ChatClientApp.frame.getX(), ChatClientApp.frame.getY(), 1200, 600);
          ChatClientApp.frame.setContentPane(
              ChatClientApp.frame.guiChatroom);
          ChatClientApp.frame.setTitle("Chat Room - " + username);
          ChatClientApp.frame.guiChatroom.startChatRoom();
        } else {
          JOptionPane.showMessageDialog(ChatClientApp.frame,
              response[2],
              "Sign-in Error",
              JOptionPane.WARNING_MESSAGE);
          ChatClientApp.frame.client.stop();
        }

      }
    });
    btnNewButton.setBounds(34, 172, 91, 29);
    add(btnNewButton);

    JButton btnSignUp = new JButton("Sign Up");
    btnSignUp.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ChatClientApp.frame.setContentPane(
            ChatClientApp.frame.guiSignUp);
        ChatClientApp.frame.setTitle("Sign up");
      }
    });
    btnSignUp.setBounds(157, 172, 91, 29);
    add(btnSignUp);
  }
}
