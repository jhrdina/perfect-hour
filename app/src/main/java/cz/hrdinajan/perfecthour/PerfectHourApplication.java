package cz.hrdinajan.perfecthour;

import android.app.Application;

import java.util.ArrayList;

public class PerfectHourApplication extends Application {

    private ArrayList<Integer> minutePoints;
    private boolean enabled = false;

    public PerfectHourApplication() {
        minutePoints = new ArrayList<>();

        minutePoints.add(0);
        minutePoints.add(50);
    }

    public ArrayList<Integer> getMinutePoints() {
        return minutePoints;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
