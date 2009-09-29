import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;


public class DateTestCase extends TestCase {

	public void testDate() {
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date startOfWeek = calendar.getTime();

		calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		Date endOfWeek = calendar.getTime();
		
		System.out.println(startOfWeek);
		System.out.println(endOfWeek);

		long millis = endOfWeek.getTime() - startOfWeek.getTime();
		for (int i=0; i<100; i++) {
		Date rndDate = new Date(startOfWeek.getTime() + (long)(Math.random() * millis));
		System.out.println(rndDate);
		}
	}
	
}
