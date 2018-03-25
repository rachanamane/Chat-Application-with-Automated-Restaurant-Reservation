package shared;

/**
 * @author rachana
 */
public final class ChatMessage {

  private final int messageId;
  private final User fromUser;
  private final User toUser;
  private final long timestamp;
  private final String content;

  public ChatMessage(int messageId,
                     User fromUser,
                     User toUser,
                     long timestamp,
                     String content) {
    this.messageId = messageId;
    this.fromUser = fromUser;
    this.toUser = toUser;
    this.timestamp = timestamp;
    this.content = content;
  }

  public int getMessageId() {
    return messageId;
  }

  public User getFromUser() {
    return fromUser;
  }

  public User getToUser() {
    return toUser;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getContent() {
    return content;
  }
}
