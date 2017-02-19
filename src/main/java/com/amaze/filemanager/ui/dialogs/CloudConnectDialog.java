package com.amaze.filemanager.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.EditTextColorStateUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by vishal on 19/2/17.
 *
 * Class represents implementation of adding new/renaming existing cloud service
 */

public class CloudConnectDialog extends DialogFragment {

    private UtilitiesProviderInterface utilitiesProviderInterface;
    private CloudConnectionListener cloudConnectionListener;
    private String invalidUsername, emptyUsername, emptyPassword;
    private MaterialDialog.Builder dialogBuilder;
    private boolean edit;   // boolean determining whether be want to edit the connection
    private int type;   // ordinal to {@link OpenMode}
    private String name, password, title;     // user id and title of dialog
    private TextInputLayout userIdTextInputLayout, passwordTextInputLayout;
    private AppCompatEditText userIdEditText, passwordEditText;
    private View rootView;
    private int accentColor;

    public static final String KEY_EDIT = "edit";
    public static final String KEY_TYPE = "type";
    public static final String KEY_NAME = "name";
    public static final String KEY_PASSWORD = "password";
    public static final String TAG_FRAGMENT = "cloud_connect_dialog";

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public interface CloudConnectionListener {

        void addConnection(boolean edit, String userId, String password, OpenMode accountType,
                           String oldUserId, String oldPassword);

        void deleteConnection(String name, OpenMode accountType);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        utilitiesProviderInterface = (UtilitiesProviderInterface) getActivity();
        cloudConnectionListener = (CloudConnectionListener) getActivity();

        accentColor = utilitiesProviderInterface.getColorPreference().getColor(ColorUsage.ACCENT);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        dialogBuilder = new MaterialDialog.Builder(getActivity());

        edit = getArguments().getBoolean(KEY_EDIT, false);
        type = getArguments().getInt(KEY_TYPE);

        switch (OpenMode.getOpenMode(type)) {
            case BOX:
                dialogBuilder.title(R.string.cloud_box);
                break;
            case DROPBOX:
                dialogBuilder.title(R.string.cloud_dropbox);
                break;
            case GDRIVE:
                dialogBuilder.title(R.string.cloud_drive);
                break;
            case ONEDRIVE:
                dialogBuilder.title(R.string.cloud_onedrive);
                break;
        }

        invalidUsername = String.format(getString(R.string.invalid), getString(R.string.cloud_error_email).toLowerCase());
        emptyUsername = String.format(getString(R.string.cantbeempty), getString(R.string.cloud_error_email));
        emptyPassword = String.format(getString(R.string.cantbeempty), getString(R.string.password));

        rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_cloud, null);
        userIdTextInputLayout = (TextInputLayout) rootView.findViewById(R.id.text_input_layout_user_id);
        passwordTextInputLayout = (TextInputLayout) rootView.findViewById(R.id.text_input_layout_password);
        userIdEditText = (AppCompatEditText) rootView.findViewById(R.id.edit_text_cloud_user_id);
        passwordEditText = (AppCompatEditText) rootView.findViewById(R.id.edit_text_cloud_password);

        dialogBuilder.positiveColor(accentColor);
        dialogBuilder.negativeColor(accentColor);
        dialogBuilder.positiveText(R.string.save);
        dialogBuilder.negativeText(R.string.cancel);
        dialogBuilder.theme(utilitiesProviderInterface.getAppTheme().getMaterialDialogTheme());
        dialogBuilder.autoDismiss(false);

        userIdEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if(s.toString().length()==0)
                    userIdTextInputLayout.setError(emptyUsername);
                else if (!validateEmailId(s.toString()))
                    userIdTextInputLayout.setError(invalidUsername);
                else
                    userIdTextInputLayout.setError("");
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if(s.toString().length()==0)
                    passwordTextInputLayout.setError(emptyUsername);
                else
                    passwordTextInputLayout.setError("");
            }
        });

        // if we're here to edit, first set up the fields
        if (edit) {

            // name, and password are supposed to be arguments when we are editing the existing account
            name = getArguments().getString(KEY_NAME);
            password = getArguments().getString(KEY_PASSWORD);

            dialogBuilder.neutralText(R.string.delete);
            dialogBuilder.neutralColor(utilitiesProviderInterface.getColorPreference().getColor(ColorUsage.ACCENT));

            userIdEditText.setText(name);
            passwordEditText.setText(password);
        }

        dialogBuilder.customView(rootView, true);

        EditTextColorStateUtil.setTint(userIdEditText, accentColor);
        EditTextColorStateUtil.setTint(passwordEditText, accentColor);

        dialogBuilder.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                if (userIdEditText.getText().toString().length() == 0 ||
                        passwordEditText.getText().toString().length() == 0 ||
                        !validateEmailId(userIdEditText.getText().toString())) return;

                dismiss();

                cloudConnectionListener.addConnection(edit, userIdEditText.getText().toString(),
                        passwordEditText.getText().toString(), OpenMode.getOpenMode(type),
                        name, password);
            }
        });

        dialogBuilder.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                // cancel key
                dismiss();
            }
        });

        dialogBuilder.onNeutral(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                dismiss();

                // delete key
                cloudConnectionListener.deleteConnection(userIdEditText.getText().toString(),
                        OpenMode.getOpenMode(type));
            }
        });

        return dialogBuilder.build();
    }

    private boolean validateEmailId(String email) {
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);

        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}
