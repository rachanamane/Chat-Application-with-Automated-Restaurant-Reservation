package ChatClient;

import javax.swing.*;

/**
 * @author rachana
 */
final class ListModelHelper {

  static void addToModel(DefaultListModel<String> model, String string) {
    model.addElement(string);
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
