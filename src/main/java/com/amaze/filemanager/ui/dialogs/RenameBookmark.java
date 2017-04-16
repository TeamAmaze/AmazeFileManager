package com.amaze.filemanager.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;

import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by arpitkh996 on 21-01-2016.
 */
public class RenameBookmark extends DialogFragment {
    private UtilitiesProviderInterface utilsProvider;

    String title, path, user = "", pass = "", ipp = "";
    String fabskin;
    Context c;
    BookmarkCallback bookmarkCallback;
    SharedPreferences Sp;
    int studiomode = 0;

    public static RenameBookmark getInstance(String name, String path, String fabskin) {
        RenameBookmark renameBookmark = new RenameBookmark();
        Bundle bundle = new Bundle();
        bundle.putString("title", name);
        bundle.putString("path", path);
        bundle.putString("fabskin", fabskin);
        renameBookmark.setArguments(bundle);
        return renameBookmark;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utilsProvider = (UtilitiesProviderInterface) getActivity();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        c = getActivity();
        if (getActivity() instanceof BookmarkCallback)
            bookmarkCallback = (BookmarkCallback) getActivity();
        title = getArguments().getString("title");
        path = getArguments().getString("path");
        fabskin = getArguments().getString("fabskin");
        Sp = PreferenceManager.getDefaultSharedPreferences(c);
        studiomode = Sp.getInt("studio", 0);
        if (DataUtils.containsBooks(new String[]{title, path}) != -1 || DataUtils.containsAccounts(new String[]{title, path}) != -1) {
            final MaterialDialog materialDialog;
            String pa = path;
            MaterialDialog.Builder builder = new MaterialDialog.Builder(c);
            builder.title(R.string.renamebookmark);
            builder.positiveColor(Color.parseColor(fabskin));
            builder.negativeColor(Color.parseColor(fabskin));
            builder.neutralColor(Color.parseColor(fabskin));
            builder.positiveText(R.string.save);
            builder.neutralText(R.string.cancel);
            builder.negativeText(R.string.delete);
            builder.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
            builder.autoDismiss(false);
            View v2 = getActivity().getLayoutInflater().inflate(R.layout.rename, null);
            builder.customView(v2, true);
            final TextInputLayout t1 = (TextInputLayout) v2.findViewById(R.id.t1);
            final TextInputLayout t2 = (TextInputLayout) v2.findViewById(R.id.t2);
            final AppCompatEditText con_name = (AppCompatEditText) v2.findViewById(R.id.editText4);
            con_name.setText(title);
            final String s1 = String.format(getString(R.string.cantbeempty), c.getResources().getString(R.string.name));
            final String s2 = String.format(getString(R.string.cantbeempty), c.getResources().getString(R.string.path));
            con_name.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (con_name.getText().toString().length() == 0)
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
                            ipp = a.getHost();
                            pa = "smb://" + ipp + a.getPath();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                ip.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (ip.getText().toString().length() == 0)
                            t2.setError(s1);
                        else t2.setError("");
                    }
                });
            } else t2.setVisibility(View.GONE);
            ip.setText(pa);
            builder.onNeutral(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    dialog.dismiss();
                }
            });
            materialDialog = builder.build();
            materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String t = ip.getText().toString();
                    String name = con_name.getText().toString();
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
                    if ((i = DataUtils.containsBooks(new String[]{title, path})) != -1) {
                        if (!t.equals(title) && t.length() >= 1) {
                            DataUtils.removeBook(i);
                            DataUtils.addBook(new String[]{name, t});
                            DataUtils.sortBook();
                            if (bookmarkCallback != null) {
                                bookmarkCallback.modify(path, title, t, name);
                            }
                        }
                    }
                    materialDialog.dismiss();

                }
            });
            materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int i = -1;
                    if ((i = DataUtils.containsBooks(new String[]{title, path})) != -1) {
                        DataUtils.removeBook(i);
                        if (bookmarkCallback != null) {
                            bookmarkCallback.delete(title, path);
                        }
                    }
                    materialDialog.dismiss();
                }
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
