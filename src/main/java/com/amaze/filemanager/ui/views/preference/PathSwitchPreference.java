package com.amaze.filemanager.ui.views.preference;

import android.content.Context;
import android.preference.Preference;
import android.support.annotation.IdRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.amaze.filemanager.R;

/**
 * @author Emmanuel
 *         on 17/4/2017, at 22:22.
 */

public class PathSwitchPreference extends Preference {

    public static final int EDIT = 0, SWITCH = 1, DELETE = 2;

    private int lastItemClicked = -1;
    private Switch switchView;
    private View.OnClickListener switchListener;
    /**
     * shouldEnable is the same thing as enabled, but is used before super.onBindView(view) has been called
     * enabled is the current state of this Preference (check this.updateSwitch())
     */
    private boolean shouldEnable = true, enabled = true;

    public PathSwitchPreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        setWidgetLayoutResource(R.layout.namepathswich_preference);
        return super.onCreateView(parent);
    }

    @Override
    protected void onBindView(View view) {
        setListener(view, R.id.edit, EDIT);
        switchListener = setListener(view, R.id.switch_button, SWITCH);
        setListener(view, R.id.delete, DELETE);

        super.onBindView(view);//Keep this before things that need changing what's on screen

        switchView = (Switch) view.findViewById(R.id.switch_button);

        switchView.setChecked(shouldEnable);
        updateSwitch(view);
    }

    public void setChecked(boolean checked) {
        if(switchView != null) {
            switchView.setChecked(checked);
            switchListener.onClick(switchView);
        } else shouldEnable = checked;
    }

    public boolean isChecked() {
        return enabled;
    }

    public int getLastItemClicked() {
        return lastItemClicked;
    }

    private View.OnClickListener setListener(final View v, @IdRes int id, final int elem) {
        final PathSwitchPreference t = this;

        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lastItemClicked = elem;

                if(lastItemClicked == SWITCH) {
                    updateSwitch(v);
                }

                if(enabled || lastItemClicked != EDIT) {
                    getOnPreferenceClickListener().onPreferenceClick(t);
                }
            }
        };

        v.findViewById(id).setOnClickListener(l);

        return l;
    }
    
    /**
     * Updates this Preference's state to coincide with the switch's state
     * (after the switch has changed state)
     * @param v the view witch contains the switch
     */
    private void updateSwitch(View v) {
        Switch s = (Switch) v.findViewById(R.id.switch_button);
        enabled = s.isChecked();

        v.findViewById(android.R.id.title).setEnabled(enabled);
        v.findViewById(android.R.id.summary).setEnabled(enabled);
        v.findViewById(R.id.edit).setEnabled(enabled);
    }

}
