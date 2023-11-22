/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.ui.dialogs;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.activities.superclasses.BasicActivity;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.SimpleTextWatcher;
import com.google.android.material.textfield.TextInputLayout;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.preference.PreferenceManager;

public class RenameBookmark extends DialogFragment {

  private String title;
  private String path;
  private BookmarkCallback bookmarkCallback;
  private final DataUtils dataUtils = DataUtils.getInstance();

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

    if (dataUtils.containsBooks(new String[] {title, path}) != -1) {
      final MaterialDialog materialDialog;
      String pa = path;
      MaterialDialog.Builder builder = new MaterialDialog.Builder(c);
      builder.title(R.string.rename_bookmark);
      builder.positiveColor(accentColor);
      builder.negativeColor(accentColor);
      builder.neutralColor(accentColor);
      builder.positiveText(R.string.save);
      builder.neutralText(R.string.cancel);
      builder.negativeText(R.string.delete);
      builder.theme(((BasicActivity) getActivity()).getAppTheme().getMaterialDialogTheme());
      builder.autoDismiss(false);
      View v2 = getActivity().getLayoutInflater().inflate(R.layout.rename, null);
      builder.customView(v2, true);
      final TextInputLayout t1 = v2.findViewById(R.id.t1);
      final TextInputLayout t2 = v2.findViewById(R.id.t2);
      final AppCompatEditText conName = v2.findViewById(R.id.editText4);
      conName.setText(title);
      final String s1 = getString(R.string.cant_be_empty, c.getString(R.string.name));
      final String s2 = getString(R.string.cant_be_empty, c.getString(R.string.path));
      conName.addTextChangedListener(
          new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
              if (conName.getText().toString().length() == 0) t1.setError(s2);
              else t1.setError("");
            }
          });
      final AppCompatEditText ip = v2.findViewById(R.id.editText);
      t2.setVisibility(View.GONE);
      ip.setText(pa);
      builder.onNeutral((dialog, which) -> dialog.dismiss());

      materialDialog = builder.build();
      materialDialog
          .getActionButton(DialogAction.POSITIVE)
          .setOnClickListener(
              v -> {
                String t = ip.getText().toString();
                String name = conName.getText().toString();
                int i = -1;
                if ((i = dataUtils.containsBooks(new String[] {title, path})) != -1) {
                  if (!t.equals(title) && t.length() >= 1) {
                    dataUtils.removeBook(i);
                    dataUtils.addBook(new String[] {name, t});
                    dataUtils.sortBook();
                    if (bookmarkCallback != null) {
                      bookmarkCallback.modify(path, title, t, name);
                    }
                  }
                }
                materialDialog.dismiss();
              });
      materialDialog
          .getActionButton(DialogAction.NEGATIVE)
          .setOnClickListener(
              v -> {
                int i;
                if ((i = dataUtils.containsBooks(new String[] {title, path})) != -1) {
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
