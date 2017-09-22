package cz.hrdinajan.perfecthour;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.TreeSet;

public class Utils {
    public static Calendar findNext(TreeSet<Integer> minutePoints) {
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
                break;
            }
        }

        if (!nextIsSet) {
            next.set(Calendar.MINUTE, minutePoints.first());
            next.set(Calendar.SECOND, 0);

            next.add(Calendar.HOUR_OF_DAY, 1);
        }

        return next;
    }
    
    public static TreeSet<Integer> getDebugMinutePoints() {
        TreeSet<Integer> minPoints = new TreeSet<>();
        for (int i = 0; i < 60; i++) {
            minPoints.add(i);
        }
        return minPoints;
    }

    public static TreeSet<Integer> getDefaultMinutePoints() {
        TreeSet<Integer> minPoints = new TreeSet<>();
        minPoints.add(0);
        minPoints.add(50);
        return minPoints;
    }

    public static TreeSet<Integer> minutePointsFromStringSet(Set<String> strSet) {
        if (strSet == null) return null;

        TreeSet<Integer> minPoints = new TreeSet<>();
        for (String strVal : strSet) {
            minPoints.add(Integer.parseInt(strVal));
        }
        return minPoints;
    }

    public static Set<String> minutePointsToStringSet(TreeSet<Integer> minPoints) {
        if (minPoints == null) return null;

        Set<String> strSet = new TreeSet<>();
        for (Integer minPoint : minPoints) {
            strSet.add(Integer.toString(minPoint));
        }
        return strSet;
    }

    public static TreeSet<Integer> addPoint(TreeSet<Integer> minPoints) {

        // First try to fill 5 minutes interval.
        for (int i = 0; i < 60; i += 5) {
            if (!minPoints.contains(i)) {
                minPoints.add(i);
                return minPoints;
            }
        }

        // Than use any other free minute.
        for (int i = 0; i < 60; i ++) {
            if (!minPoints.contains(i)) {
                minPoints.add(i);
                return minPoints;
            }
        }
        return minPoints;
    }

    public static TreeSet<Integer> removePoint(TreeSet<Integer> minPoints) {
        if (minPoints.size() > 1) {
            minPoints.remove(minPoints.last());
        }
        return minPoints;
    }
}
