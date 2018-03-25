package shared;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author rachana
 * This is a program to create SQL query to add restaurant slots in database
 */
public class CreateInsertQueries {
  private static final SimpleDateFormat DATE_FORMATTER =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private static final long ONE_HOUR_IN_MILLISECONDS = 60*60*1000;
  private static final long ONE_DAY_IN_MILLISECONDS = 24*60*60*1000;

  public static void main(String[] args) {
    List<Integer> restaurantIds = Arrays.asList(6,16,17,18);
    long epochTime = java.sql.Timestamp.valueOf("2018-03-20 18:00:00").getTime();

    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO bookings (restaurantUserId, bookingtime) VALUES ");
    for (int i=0;i<20;i++) {
      long currentEpoch = epochTime;
      for (int j=0;j<6;j++) {
        for (int restaurantId : restaurantIds) {
          sb.append(String.format("(%s, TIMESTAMP '%s'), ",
              restaurantId,
              DATE_FORMATTER.format(new Date(currentEpoch))));
        }
        currentEpoch += ONE_HOUR_IN_MILLISECONDS;
      }
      epochTime += ONE_DAY_IN_MILLISECONDS;
    }
    sb.delete(sb.length() - 2, sb.length());
    sb.append(";");
    System.out.println(sb.toString());
  }
}
