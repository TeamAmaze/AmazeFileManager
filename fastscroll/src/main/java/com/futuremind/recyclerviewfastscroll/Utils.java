package com.futuremind.recyclerviewfastscroll;

import android.view.View;

/**
 * Created by mklimczak on 31/07/15.
 */
public class Utils {

    public static float getViewRawY(View view) {
        int[] location = new int[2];
        location[0] = 0;
        location[1] = (int) view.getY();
        ((View)view.getParent()).getLocationInWindow(location);
        return location[1];
    }

    public static float getViewRawX(View view) {
        int[] location = new int[2];
        location[0] = (int) view.getX();
        location[1] = 0;
        ((View)view.getParent()).getLocationInWindow(location);
        return location[0];
    }

    public static float getValueInRange(float min, float max, float value) {
        float minimum = Math.max(min, value);
        return Math.min(minimum, max);
    }

}
