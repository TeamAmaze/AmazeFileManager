package com.amaze.filemanager.ui.views.preference;

import android.content.Context;
import android.preference.Preference;
import android.support.annotation.IdRes;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.amaze.filemanager.R;

/**
 * @author Emmanuel
 *         on 17/4/2017, at 22:22.
 */

public class NamePathSwitchPreference extends Preference {

    public static final int EDIT = 0, SWITCH = 1, DELETE = 2;

    private int lastItemClicked = -1;

    public NamePathSwitchPreference(Context context) {
        super(context);
    }

    public NamePathSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NamePathSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected View onCreateView(ViewGroup parent) {
        setWidgetLayoutResource(R.layout.namepathswich_preference);
        return super.onCreateView(parent);
    }

    @Override
    protected void onBindView(View view) {
        setListener(view, R.id.edit, EDIT);
        setListener(view, R.id.switch_button, SWITCH);
        setListener(view, R.id.delete, DELETE);

        ((Switch) view.findViewById(R.id.switch_button)).setChecked(true);

        super.onBindView(view);
    }

    public int getLastItemClicked() {
        return lastItemClicked;
    }

    private void setListener(final View v, @IdRes int id, final int elem) {
        final NamePathSwitchPreference t = this;

        v.findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lastItemClicked = elem;

                Switch s = (Switch) v.findViewById(R.id.switch_button);
                if(lastItemClicked == SWITCH) {
                    v.findViewById(android.R.id.title).setEnabled(s.isChecked());
                    v.findViewById(android.R.id.summary).setEnabled(s.isChecked());
                    v.findViewById(R.id.edit).setEnabled(s.isChecked());
                }

                if(s.isChecked() || lastItemClicked != EDIT) {
                    getOnPreferenceClickListener().onPreferenceClick(t);
                }
            }
        });
    }

}
