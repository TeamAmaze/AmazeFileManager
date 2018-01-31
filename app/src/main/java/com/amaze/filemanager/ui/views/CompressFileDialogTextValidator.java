package com.amaze.filemanager.ui.views;

import android.content.Context;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.SimpleTextWatcher;

import java.util.regex.Pattern;

public final class CompressFileDialogTextValidator extends SimpleTextWatcher implements View.OnFocusChangeListener {
    private static final Pattern ZIP_FILE_REGEX = Pattern.compile("[\\\\\\/:\\*\\?\"<>\\|\\x01-\\x1F\\x7F]", Pattern.CASE_INSENSITIVE);

    private final Context mContext;
    private final EditText mEditText;
    private final WarnableTextInputLayout mTextInputLayout;
    private final View mPositiveButton;

    public CompressFileDialogTextValidator(Context context, EditText editText, WarnableTextInputLayout textInputLayout, View positiveButton) {
        mContext = context;
        mEditText = editText;
        mTextInputLayout = textInputLayout;
        mPositiveButton = positiveButton;
    }

    private void doValidate() {
        String value = mEditText.getText().toString();
        //It's not easy to use regex to detect single/double dot while leaving valid values (filename.zip) behind...
        //So we simply use equality to check them
        boolean isValidFilename = (!ZIP_FILE_REGEX.matcher(value).find()) && !".".equals(value) && !"..".equals(value);

        if (isValidFilename && value.length() > 0) {
            mTextInputLayout.setError(null);
            mPositiveButton.setEnabled(true);
            if (!value.toLowerCase().endsWith(".zip")) {
                mTextInputLayout.setWarning(R.string.compress_file_suggest_zip_extension);
            } else {
                mTextInputLayout.setHint(mContext.getString(R.string.enterzipname));
            }
        } else {
            mTextInputLayout.setHint(mContext.getString(R.string.enterzipname));

            if (!isValidFilename) {
                mTextInputLayout.setError(mContext.getString(R.string.invalid_name));
            } else if (value.length() < 1) {
                mTextInputLayout.setError(mContext.getString(R.string.field_empty));
            }

            mPositiveButton.setEnabled(false);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        doValidate();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            doValidate();
        }
    }
}
