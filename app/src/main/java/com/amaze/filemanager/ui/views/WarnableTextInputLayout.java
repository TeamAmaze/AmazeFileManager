package com.amaze.filemanager.ui.views;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.material.textfield.TextInputLayout;
import android.util.AttributeSet;

import com.amaze.filemanager.R;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 31/1/2018, at 14:50.
 */

public class WarnableTextInputLayout extends TextInputLayout {

    private boolean isStyleWarning = false;

    public WarnableTextInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Remove error or warning
     */
    public void removeError() {
        super.setError(null);
        setErrorEnabled(false);
    }

    @Override
    public void setError(@Nullable CharSequence error) {
        if(isStyleWarning) {
            setErrorEnabled(true);
            setErrorTextAppearance(R.style.error_inputTextLayout);
            isStyleWarning = false;
        }
        super.setError(error);
    }

    public void setWarning(@StringRes int text) {
        if(!isStyleWarning) {
            removeError();
            setErrorEnabled(true);
            setErrorTextAppearance(R.style.warning_inputTextLayout);
            isStyleWarning = true;
        }
        super.setError(getContext().getString(text));
    }


}
