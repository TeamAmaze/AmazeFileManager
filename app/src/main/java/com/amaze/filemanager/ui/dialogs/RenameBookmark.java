package com.amaze.filemanager.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.SimpleTextWatcher;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;

import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by arpitkh996 on 21-01-2016.
 */
public class RenameBookmark extends DialogFragment {

    private String title;
    private String path;
    private String user = "";
    private String pass = "";
    private BookmarkCallback bookmarkCallback;
    private int studiomode = 0;
    private DataUtils dataUtils = DataUtils.getInstance();

    public static RenameBookmark getInstance(String name, String path, int accentColor) {
        RenameBookmark renameBookmark = new RenameBookmark();
        Bundle bundle = new Bundle();
        bundle.putString("title", name);
        bundle.putString("path", path);
        bundle.putInt("accentColor", accentColor);

        renameBookmark.setArguments(bundle);
        return renameBookmark;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context c = getActivity();
        if (getActivity() instanceof BookmarkCallback)
            bookmarkCallback = (BookmarkCallback) getActivity();
        title = getArguments().getString("title");
        path = getArguments().getString("path");
        int accentColor = getArguments().getInt("accentColor");
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);

        studiomode = sp.getInt("studio", 0);
        if (dataUtils.containsBooks(new String[]{title, path}) != -1) {
            final MaterialDialog materialDialog;
            String pa = path;
            MaterialDialog.Builder builder = new MaterialDialog.Builder(c);
            builder.title(R.string.renamebookmark);
            builder.positiveColor(accentColor);
            builder.negativeColor(accentColor);
            builder.neutralColor(accentColor);
            builder.positiveText(R.string.save);
            builder.neutralText(R.string.cancel);
            builder.negativeText(R.string.delete);
            builder.theme(((UtilitiesProviderInterface) getActivity()).getAppTheme().getMaterialDialogTheme());
            builder.autoDismiss(false);
            View v2 = getActivity().getLayoutInflater().inflate(R.layout.rename, null);
            builder.customView(v2, true);
            final TextInputLayout t1 = (TextInputLayout) v2.findViewById(R.id.t1);
            final TextInputLayout t2 = (TextInputLayout) v2.findViewById(R.id.t2);
            final AppCompatEditText conName = (AppCompatEditText) v2.findViewById(R.id.editText4);
            conName.setText(title);
            final String s1 = String.format(getString(R.string.cantbeempty), c.getResources().getString(R.string.name));
            final String s2 = String.format(getString(R.string.cantbeempty), c.getResources().getString(R.string.path));
            conName.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (conName.getText().toString().length() == 0)
                        t1.setError(s2);
                    else t1.setError("");
                }
            });
            final AppCompatEditText ip = (AppCompatEditText) v2.findViewById(R.id.editText);
            if (studiomode != 0) {
                if (path.startsWith("smb:/")) {
                    try {
                        jcifs.Config.registerSmbURLHandler();
                        URL a = new URL(path);
                        String userinfo = a.getUserInfo();
                        if (userinfo != null) {
                            String inf = URLDecoder.decode(userinfo, "UTF-8");
                            user = inf.substring(0, inf.indexOf(":"));
                            pass = inf.substring(inf.indexOf(":") + 1, inf.length());
                            String ipp = a.getHost();
                            pa = "smb://" + ipp + a.getPath();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                ip.addTextChangedListener(new SimpleTextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        if (ip.getText().toString().length() == 0)
                            t2.setError(s1);
                        else t2.setError("");
                    }
                });
            } else t2.setVisibility(View.GONE);
            ip.setText(pa);
            builder.onNeutral((dialog, which) -> dialog.dismiss());

            materialDialog = builder.build();
            materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(v -> {
                String t = ip.getText().toString();
                String name = conName.getText().toString();
                if (studiomode != 0 && t.startsWith("smb://")) {
                    try {
                        URL a = new URL(t);
                        String userinfo = a.getUserInfo();
                        if (userinfo == null && user.length() > 0) {
                            t = "smb://" + ((URLEncoder.encode(user, "UTF-8") + ":" + URLEncoder.encode(pass, "UTF-8") + "@")) + a.getHost() + a.getPath();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                int i = -1;
                if ((i = dataUtils.containsBooks(new String[]{title, path})) != -1) {
                    if (!t.equals(title) && t.length() >= 1) {
                        dataUtils.removeBook(i);
                        dataUtils.addBook(new String[]{name, t});
                        dataUtils.sortBook();
                        if (bookmarkCallback != null) {
                            bookmarkCallback.modify(path, title, t, name);
                        }
                    }
                }
                materialDialog.dismiss();

            });
            materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(v -> {
                int i;
                if ((i = dataUtils.containsBooks(new String[]{title, path})) != -1) {
                    dataUtils.removeBook(i);
                    if (bookmarkCallback != null) {
                        bookmarkCallback.delete(title, path);
                    }
                }
                materialDialog.dismiss();
            });
            return materialDialog;
        }
        return null;
    }

    public interface BookmarkCallback {
        void delete(String title, String path);

        void modify(String oldpath, String oldname, String newpath, String newname);
    }

}
