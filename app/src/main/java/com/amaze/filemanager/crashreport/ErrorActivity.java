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

package com.amaze.filemanager.crashreport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import org.acra.ReportField;
import org.acra.data.CrashReportData;
import org.json.JSONObject;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.utils.Utils;
import com.google.android.material.snackbar.Snackbar;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;

/*
 * Created by Christian Schabesberger on 24.10.15.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * ErrorActivity.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class ErrorActivity extends ThemedActivity {
  // LOG TAGS
  public static final String TAG = ErrorActivity.class.toString();
  // BUNDLE TAGS
  public static final String ERROR_INFO = "error_info";
  public static final String ERROR_LIST = "error_list";

  // Error codes
  public static final String ERROR_UI_ERROR = "UI Error";
  public static final String ERROR_USER_REPORT = "User report";
  public static final String ERROR_UNKNOWN = "Unknown";

  public static final String ERROR_GITHUB_ISSUE_URL =
      "https://github.com/TeamAmaze/AmazeFileManager/issues";

  private String[] errorList;
  private ErrorInfo errorInfo;
  private Class returnActivity;
  private String currentTimeStamp;
  private EditText userCommentBox;

  public static void reportError(
      final Context context,
      final List<Throwable> el,
      final View rootView,
      final ErrorInfo errorInfo) {
    if (rootView != null) {
      Snackbar.make(rootView, R.string.error_snackbar_message, 3 * 1000)
          .setActionTextColor(Color.YELLOW)
          .setAction(
              context.getString(R.string.error_snackbar_action).toUpperCase(),
              v -> startErrorActivity(context, errorInfo, el))
          .show();
    } else {
      startErrorActivity(context, errorInfo, el);
    }
  }

  private static void startErrorActivity(
      final Context context, final ErrorInfo errorInfo, final List<Throwable> el) {
    final Intent intent = new Intent(context, ErrorActivity.class);
    intent.putExtra(ERROR_INFO, errorInfo);
    intent.putExtra(ERROR_LIST, elToSl(el));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
  }

  public static void reportError(
      final Context context, final Throwable e, final View rootView, final ErrorInfo errorInfo) {
    List<Throwable> el = null;
    if (e != null) {
      el = new Vector<>();
      el.add(e);
    }
    reportError(context, el, rootView, errorInfo);
  }

  public static void reportError(
      final Context context, final CrashReportData report, final ErrorInfo errorInfo) {
    System.out.println("ErrorActivity reportError");
    final String[] el = new String[] {report.getString(ReportField.STACK_TRACE)};

    final Intent intent = new Intent(context, ErrorActivity.class);
    intent.putExtra(ERROR_INFO, errorInfo);
    intent.putExtra(ERROR_LIST, el);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
  }

  private static String getStackTrace(final Throwable throwable) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw, true);
    throwable.printStackTrace(pw);
    return sw.getBuffer().toString();
  }

  // errorList to StringList
  private static String[] elToSl(final List<Throwable> stackTraces) {
    final String[] out = new String[stackTraces.size()];
    for (int i = 0; i < stackTraces.size(); i++) {
      out[i] = getStackTrace(stackTraces.get(i));
    }
    return out;
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_error);
    final Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    final Intent intent = getIntent();

    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setTitle(R.string.error_report_title);
      actionBar.setDisplayShowTitleEnabled(true);
    }

    final Button reportEmailButton = findViewById(R.id.errorReportEmailButton);
    final Button reportTelegramButton = findViewById(R.id.errorReportTelegramButton);
    final Button copyButton = findViewById(R.id.errorReportCopyButton);
    final Button reportGithubButton = findViewById(R.id.errorReportGitHubButton);

    userCommentBox = findViewById(R.id.errorCommentBox);
    final TextView errorView = findViewById(R.id.errorView);
    final TextView errorMessageView = findViewById(R.id.errorMessageView);

    returnActivity = MainActivity.class;
    errorInfo = intent.getParcelableExtra(ERROR_INFO);
    errorList = intent.getStringArrayExtra(ERROR_LIST);

    // important add guru meditation
    addGuruMeditation();
    currentTimeStamp = getCurrentTimeStamp();

    reportEmailButton.setOnClickListener((View v) -> sendReportEmail());

    reportTelegramButton.setOnClickListener(
        (View v) -> {
          FileUtils.copyToClipboard(this, buildMarkdown());
          Toast.makeText(this, R.string.crash_report_copied, Toast.LENGTH_SHORT).show();
          Utils.openTelegramURL(this);
        });

    copyButton.setOnClickListener(
        (View v) -> {
          FileUtils.copyToClipboard(this, buildMarkdown());
          Toast.makeText(this, R.string.crash_report_copied, Toast.LENGTH_SHORT).show();
        });

    reportGithubButton.setOnClickListener(
        (View v) -> {
          FileUtils.copyToClipboard(this, buildMarkdown());
          Toast.makeText(this, R.string.crash_report_copied, Toast.LENGTH_SHORT).show();
          Utils.openURL(ERROR_GITHUB_ISSUE_URL, this);
        });

    // normal bugreport
    buildInfo(errorInfo);
    if (errorInfo.message != 0) {
      errorMessageView.setText(errorInfo.message);
    } else {
      errorMessageView.setVisibility(View.GONE);
      findViewById(R.id.messageWhatHappenedView).setVisibility(View.GONE);
    }

    errorView.setText(formErrorText(errorList));

    // print stack trace once again for debugging:
    for (final String e : errorList) {
      Log.e(TAG, e);
    }
    initStatusBarResources(findViewById(R.id.parent_view));
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    final MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.error_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    final int id = item.getItemId();
    switch (id) {
      case android.R.id.home:
        goToReturnActivity();
        break;
      case R.id.menu_item_share_error:
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, buildMarkdown());
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
        break;
      default:
        break;
    }
    return false;
  }

  private void sendReportEmail() {
    final Intent i = Utils.buildEmailIntent(buildMarkdown(), Utils.EMAIL_NOREPLY_REPORTS);
    if (i.resolveActivity(getPackageManager()) != null) {
      startActivity(i);
    }
  }

  private String formErrorText(final String[] el) {
    final StringBuilder text = new StringBuilder();
    if (el != null) {
      for (final String e : el) {
        text.append("-------------------------------------\n").append(e);
      }
    }
    text.append("-------------------------------------");
    return text.toString();
  }

  private void goToReturnActivity() {
    final Intent intent = new Intent(this, returnActivity);
    NavUtils.navigateUpTo(this, intent);
    startActivity(intent);
  }

  private void buildInfo(final ErrorInfo info) {
    final TextView infoLabelView = findViewById(R.id.errorInfoLabelsView);
    final TextView infoView = findViewById(R.id.errorInfosView);
    String text = "";

    infoLabelView.setText(getString(R.string.info_labels).replace("\\n", "\n"));

    text +=
        errorInfo.userAction
            + "\n"
            + info.request
            + "\n"
            + currentTimeStamp
            + "\n"
            + getPackageName()
            + "\n"
            + BuildConfig.VERSION_NAME
            + "\n"
            + getOsString()
            + "\n"
            + Build.DEVICE
            + "\n"
            + Build.MODEL
            + "\n"
            + Build.PRODUCT;

    infoView.setText(text);
  }

  private String buildJson() {
    try {
      Map<String, String> jsonMap = new HashMap<>();
      jsonMap.put("user_action", errorInfo.userAction);
      jsonMap.put("request", errorInfo.request);
      jsonMap.put("package", getPackageName());
      jsonMap.put("version", BuildConfig.VERSION_NAME);
      jsonMap.put("os", getOsString());
      jsonMap.put("device", Build.DEVICE);
      jsonMap.put("model", Build.MODEL);
      jsonMap.put("product", Build.PRODUCT);
      jsonMap.put("time", currentTimeStamp);
      jsonMap.put("exceptions", Arrays.asList(errorList).toString());
      jsonMap.put("user_comment", userCommentBox.getText().toString());
      return new JSONObject(jsonMap).toString();
    } catch (final Throwable e) {
      Log.e(TAG, "Could not build json");
      e.printStackTrace();
    }

    return "";
  }

  private String buildMarkdown() {
    try {
      final StringBuilder htmlErrorReport = new StringBuilder();

      String userComment = "";
      if (!TextUtils.isEmpty(userCommentBox.getText())) {
        userComment = userCommentBox.getText().toString();
      }

      // basic error info
      htmlErrorReport
          .append(
              String.format("## Issue explanation (write below this line)\n\n%s\n\n", userComment))
          .append("## Exception")
          .append("\n* __App Name:__ ")
          .append(getString(R.string.app_name))
          .append("\n* __Package:__ ")
          .append(BuildConfig.APPLICATION_ID)
          .append("\n* __Version:__ ")
          .append(BuildConfig.VERSION_NAME)
          .append("\n* __User Action:__ ")
          .append(errorInfo.userAction)
          .append("\n* __Request:__ ")
          .append(errorInfo.request)
          .append("\n* __OS:__ ")
          .append(getOsString())
          .append("\n* __Device:__ ")
          .append(Build.DEVICE)
          .append("\n* __Model:__ ")
          .append(Build.MODEL)
          .append("\n* __Product:__ ")
          .append(Build.PRODUCT)
          .append("\n");

      // Collapse all logs to a single paragraph when there are more than one
      // to keep the GitHub issue clean.
      if (errorList.length > 1) {
        htmlErrorReport
            .append("<details><summary><b>Exceptions (")
            .append(errorList.length)
            .append(")</b></summary><p>\n");
      }

      // add the logs
      for (int i = 0; i < errorList.length; i++) {
        htmlErrorReport.append("<details><summary><b>Crash log ");
        if (errorList.length > 1) {
          htmlErrorReport.append(i + 1);
        }
        htmlErrorReport
            .append("</b>")
            .append("</summary><p>\n")
            .append("\n```\n")
            .append(errorList[i])
            .append("\n```\n")
            .append("</details>\n");
      }

      // make sure to close everything
      if (errorList.length > 1) {
        htmlErrorReport.append("</p></details>\n");
      }
      htmlErrorReport.append("<hr>\n");
      return htmlErrorReport.toString();
    } catch (final Throwable e) {
      Log.e(TAG, "Error while erroring: Could not build markdown");
      e.printStackTrace();
      return "";
    }
  }

  private String getOsString() {
    final String osBase =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Build.VERSION.BASE_OS : "Android";
    return System.getProperty("os.name")
        + " "
        + (osBase.isEmpty() ? "Android" : osBase)
        + " "
        + Build.VERSION.RELEASE
        + " - "
        + Build.VERSION.SDK_INT;
  }

  private void addGuruMeditation() {
    // just an easter egg
    final TextView sorryView = findViewById(R.id.errorSorryView);
    String text = sorryView.getText().toString();
    text += "\n" + getString(R.string.guru_meditation);
    sorryView.setText(text);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    goToReturnActivity();
  }

  public String getCurrentTimeStamp() {
    final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    return df.format(new Date());
  }

  public static class ErrorInfo implements Parcelable {
    public static final Parcelable.Creator<ErrorInfo> CREATOR =
        new Parcelable.Creator<ErrorInfo>() {
          @Override
          public ErrorInfo createFromParcel(final Parcel source) {
            return new ErrorInfo(source);
          }

          @Override
          public ErrorInfo[] newArray(final int size) {
            return new ErrorInfo[size];
          }
        };

    private final String userAction;
    private final String request;
    @StringRes public final int message;

    private ErrorInfo(final String userAction, final String request, @StringRes final int message) {
      this.userAction = userAction;
      this.request = request;
      this.message = message;
    }

    protected ErrorInfo(final Parcel in) {
      this.userAction = in.readString();
      this.request = in.readString();
      this.message = in.readInt();
    }

    public static ErrorInfo make(
        final String userAction, final String request, @StringRes final int message) {
      return new ErrorInfo(userAction, request, message);
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
      dest.writeString(this.userAction);
      dest.writeString(this.request);
      dest.writeInt(this.message);
    }
  }
}
