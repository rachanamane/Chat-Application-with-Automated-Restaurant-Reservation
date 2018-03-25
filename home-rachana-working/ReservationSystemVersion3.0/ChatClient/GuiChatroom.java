package ChatClient;

import shared.User;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import static ChatClient.ListModelHelper.addToModel;
import static Protocol.ProtocolTags.BOOK_SLOT_TAG;
import static Protocol.ProtocolTags.SHOW_BOOKINGS_TAG;
import static Protocol.ProtocolTags.SHOW_OPEN_SLOTS_TAG;

/**
 * @author rachana
 */
@SuppressWarnings("serial")
public class GuiChatroom extends JPanel {
  private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

  private JTextField txInput;
  private JList<String> listChat;
  private JList<String> listOnlineUsers;
  private JButton btnSend;
  private final ChatroomModel chatroomModel = new ChatroomModel();

  /**
   * Create the panel.
   */
  public GuiChatroom() {
    setLayout(null);
    this.setBounds(0, 0, 1200, 600);

    createOnlineUserListUi();
    createChatUi();
    createInputElements();
  }

  private void createInputElements() {
    txInput = new JTextField();
    txInput.setBounds(416, 532, 587, 29);
    txInput.setColumns(10);
    txInput.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        btnSend.doClick();
      }
    });
    add(txInput);

    btnSend = new JButton("Send");
    btnSend.setEnabled(false);
    btnSend.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String msg = txInput.getText();
        if (msg.equals("")) {
          JOptionPane.showMessageDialog(ChatClientApp.frame,
              "You can not send empty messages!",
              "Warning",
              JOptionPane.WARNING_MESSAGE);
          return;
        }
        txInput.setText("");
        if (chatroomModel.getToUser().getUserType() == User.UserType.human) {
          ChatClientApp.frame.client.send_message(chatroomModel.getToUser(), msg);
        } else {
          DefaultListModel<String> listChatModel =
              (DefaultListModel<String>) listChat.getModel();
          addToModel(listChatModel,
              //show the command typed by user on the message list model
              String.format("%s: %s", chatroomModel.getLoggedInUser().getUsername(), msg));
          String[] parts = msg.split(" ");
          switch (parts[0]) {
            case SHOW_OPEN_SLOTS_TAG:
              String date;
              if (parts.length == 1) {
                date = DATE_FORMATTER.format(new Date());
              } else {
                date = parts[1];
              }
              ChatClientApp.frame.client.show_open_slots(chatroomModel.getToUser(), date);
              break;
            case BOOK_SLOT_TAG:
              String day = parts[1];
              String time = parts[2];
              ChatClientApp.frame.client.book_slot(chatroomModel.getToUser(),
                  day, time);
              break;
            case SHOW_BOOKINGS_TAG:
              ChatClientApp.frame.client.show_bookings(chatroomModel.getToUser());
              break;
            default:
              addToModel(listChatModel, "Error: Command '" + parts[0] +"' not supported.");
              break;
          }
        }


        txInput.grabFocus();
      }
    });
    btnSend.setBounds(1040, 533, 83, 29);
    add(btnSend);
  }

  private void createOnlineUserListUi() {
    listOnlineUsers = new JList<>();
    listOnlineUsers.setValueIsAdjusting(true);
    listOnlineUsers.setModel(new DefaultListModel<>());
    listOnlineUsers.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        synchronized (chatroomModel) {
          int selectedUserIndex = ((JList<String>) e.getSource()).getSelectedIndex();

          chatroomModel.setOffset(-1);
          if (selectedUserIndex < 0) {
            ((DefaultListModel<String>) listChat.getModel()).clear();
            btnSend.setEnabled(false);
            chatroomModel.setToUser(null);
          } else {
            btnSend.setEnabled(true);
            User toUser = chatroomModel.getOnlineUsers().get(selectedUserIndex);
            if (chatroomModel.getToUser() != null &&
                chatroomModel.getToUser().getUserId() == toUser.getUserId()) {
              return;
            }
            DefaultListModel<String> listChatModel =
                (DefaultListModel<String>) listChat.getModel();

            listChatModel.clear();
            chatroomModel.setToUser(toUser);
            if (toUser.getUserType() == User.UserType.human) {
              // If you want to support chat with restaurants, remove the above condition.
              ChatClientApp.frame.client.get_message(toUser, -1);
            } else {
              showHelpText(listChatModel);
              ChatClientApp.frame.client.show_bookings(toUser);
            }
          }
        }
      }
    });
    JScrollPane scrollPane = new JScrollPane(listOnlineUsers);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setBounds(6, 6, 388, 585);
    scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
      private int prevMax = 0;

      public void adjustmentValueChanged(AdjustmentEvent e) {
        if (e.getAdjustable().getMaximum() != prevMax) {
          prevMax = e.getAdjustable().getMaximum();
          e.getAdjustable().setValue(e.getAdjustable().getMaximum());
        }

      }
    });
    add(scrollPane);
  }

  private void showHelpText(DefaultListModel<String> listChatModel) {
    addToModel(listChatModel, "Available commands:");
    addToModel(listChatModel, "1) show-open-slots <date> " +
        "e.g. show-open-slots 2018-03-18");
    addToModel(listChatModel, "2) book-slot <date> <time> " +
        "e.g. book-slot 2018-03-18 19:30");
    addToModel(listChatModel, "3) show-bookings <date> " +
        "e.g. show-bookings 2018-03-18");
    String divider = new String(new char[130]).replace('\0', '-');
    addToModel(listChatModel, divider);
  }

  private void createChatUi() {
    listChat = new JList<>();
    listChat.setValueIsAdjusting(true);
    listChat.setModel(new DefaultListModel<>());
    JScrollPane chatScrollPane = new JScrollPane(listChat);
    chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    chatScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    chatScrollPane.setBounds(406, 6, 788, 514);
    chatScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
      private int prevMax = 0;

      public void adjustmentValueChanged(AdjustmentEvent e) {
        if (e.getAdjustable().getMaximum() != prevMax) {
          prevMax = e.getAdjustable().getMaximum();
          e.getAdjustable().setValue(e.getAdjustable().getMaximum());
        }

      }
    });
    add(chatScrollPane);
  }

  public void setLoggedInUser(User loggedInUser) {
    chatroomModel.setLoggedInUser(loggedInUser);
  }

  public void startChatRoom() {
    Thread thread = new ServerThread(listChat, listOnlineUsers, chatroomModel);
    thread.start();
    ChatClientApp.frame.client.show_online_users();
  }
}


