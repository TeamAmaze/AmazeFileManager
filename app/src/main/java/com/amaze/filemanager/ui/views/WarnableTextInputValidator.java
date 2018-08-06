package com.amaze.filemanager.ui.views;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.text.Editable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.SimpleTextWatcher;

public final class WarnableTextInputValidator extends SimpleTextWatcher implements View.OnFocusChangeListener, View.OnTouchListener {
    private final Context mContext;
    private final EditText mEditText;
    private final View mButton;
    private final WarnableTextInputLayout mTextInputLayout;
    private final OnTextValidate mValidator;
    private @DrawableRes int warningDrawable, errorDrawable;

    public WarnableTextInputValidator(Context context, EditText editText,
                                      WarnableTextInputLayout textInputLayout,
                                      View positiveButton, OnTextValidate validator) {
        mContext = context;
        mEditText = editText;
        mEditText.setOnFocusChangeListener(this);
        mEditText.addTextChangedListener(this);
        mTextInputLayout = textInputLayout;
        mButton = positiveButton;
        mButton.setOnTouchListener(this);
        mButton.setEnabled(false);
        mValidator = validator;

        warningDrawable = R.drawable.ic_warning_24dp;
        errorDrawable = R.drawable.ic_error_24dp;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            int state = doValidate(false);
            mButton.setEnabled(state != ReturnState.STATE_ERROR);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return performClick();
    }

    public boolean performClick () {
        boolean blockTouchEvent = doValidate(false) == ReturnState.STATE_ERROR;
        return blockTouchEvent;
    }

    @Override
    public void afterTextChanged(Editable s) {
        doValidate(false);
    }

    /**
     * @return ReturnState.state
     */
    private int doValidate(boolean onlySetWarning) {
        ReturnState state = mValidator.isTextValid(mEditText.getText().toString());
        switch (state.state) {
            case ReturnState.STATE_NORMAL:
                mTextInputLayout.removeError();
                setEditTextIcon(null);
                mButton.setEnabled(true);
                break;
            case ReturnState.STATE_ERROR:
                if(!onlySetWarning) {
                    mTextInputLayout.setError(mContext.getString(state.text));
                    setEditTextIcon(errorDrawable);
                }
                mButton.setEnabled(false);
                break;
            case ReturnState.STATE_WARNING:
                mTextInputLayout.setWarning(state.text);
                setEditTextIcon(warningDrawable);
                mButton.setEnabled(true);
                break;
        }

        return state.state;
    }

    private void setEditTextIcon(@DrawableRes Integer drawable) {
        @DrawableRes int drawableInt = drawable != null? drawable:0;
        mEditText.setCompoundDrawablesWithIntrinsicBounds(0,0, drawableInt,0);
    }

    public interface OnTextValidate {
        ReturnState isTextValid(String text);
    }

    public static class ReturnState {
        public static final int STATE_NORMAL = 0, STATE_ERROR = -1, STATE_WARNING = -2;

        public final int state;
        public final @StringRes int text;

        public ReturnState() {
            state = STATE_NORMAL;
            text = 0;
        }

        public ReturnState(int state, @StringRes int text) {
            this.state = state;
            this.text = text;
        }
    }

}
