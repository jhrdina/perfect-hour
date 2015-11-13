package cz.hrdinajan.perfecthour;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class Utils {
    public static Calendar findNext(ArrayList<Integer> minutePoints) {
        Calendar now = GregorianCalendar.getInstance();
        now.add(Calendar.SECOND, 3);
        now.set(Calendar.MILLISECOND, 0);

        Calendar next = GregorianCalendar.getInstance();
        next.setTime(now.getTime());

        boolean nextIsSet = false;

        for (int item : minutePoints) {
            if (item > now.get(Calendar.MINUTE)) {
                next.set(Calendar.MINUTE, item);
                next.set(Calendar.SECOND, 0);

                nextIsSet = true;
            }
        }

        if (!nextIsSet) {
            next.set(Calendar.MINUTE, minutePoints.get(0));
            next.set(Calendar.SECOND, 0);

            next.add(Calendar.HOUR_OF_DAY, 1);
        }

        return next;
    }
}
