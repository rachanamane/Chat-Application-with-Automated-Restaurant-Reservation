/**
 * @author rachana
 */
public interface Protocol {

  public abstract String createMessage(String... args);

  public abstract String[] decodeMessage(String str);
}
