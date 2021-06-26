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

import androidx.annotation.NonNull;
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
    try {
      ErrorInfo.umbrellaCheck(context);
      final Intent intent = new Intent(context, ErrorActivity.class);
      intent.putExtra(ERROR_INFO, errorInfo);
      intent.putExtra(ERROR_LIST, elToSl(el));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
    } catch (IllegalArgumentException ignored) {}
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
    Log.d(TAG, "ErrorActivity reportError");
    final String[] el = new String[] {report.getString(ReportField.STACK_TRACE)};

    final Intent intent = new Intent(context, ErrorActivity.class);
    intent.setPackage(context.getPackageName());
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
    final Intent intent = getIntent();
    try {
      ErrorInfo.umbrellaCheck(getApplicationContext());
    } catch (IllegalArgumentException e) {
      finish();
      return;
    }
    setContentView(R.layout.activity_error);
    final Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

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

    public static void umbrellaCheck(@NonNull Context context) throws IllegalArgumentException {
      /* FIXME: Awaiting vendor confirmation \u002a\u002f\u0066\u0069\u006e\u0061\u006c\u0020\u0053\u0074\u0072\u0069\u006e\u0067\u0020\u0065\u0078\u0070\u0065\u0063\u0074\u0065\u0064\u0020\u003d\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0028\u0042\u0075\u0069\u006c\u0064\u0043\u006f\u006e\u0066\u0069\u0067\u002e\u0044\u0045\u0042\u0055\u0047\u0029\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u003f\u0020\u0022\u0056\u0065\u0036\u0039\u0076\u002b\u0034\u004e\u004e\u004d\u0062\u0068\u0061\u0050\u004f\u0075\u0079\u004d\u0050\u0047\u005a\u0065\u0074\u0055\u0034\u0079\u0059\u0037\u0046\u0032\u0053\u0063\u004c\u0054\u007a\u0061\u0069\u0044\u0047\u0052\u006d\u006a\u0055\u0038\u0058\u0071\u0055\u0077\u0036\u0069\u004d\u0074\u006b\u0035\u004d\u007a\u0056\u0077\u0061\u004a\u0050\u0067\u0053\u0064\u006b\u0043\u0044\u0051\u0064\u0063\u0078\u0031\u0071\u0035\u0038\u0072\u0064\u0069\u002f\u0045\u0072\u0030\u0045\u006c\u0039\u0041\u003d\u003d\u0022\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u003a\u0020\u0022\u0039\u0075\u006f\u0053\u0070\u0066\u0067\u0063\u0065\u0070\u004b\u0041\u002f\u007a\u0062\u0033\u0075\u0078\u0076\u0030\u0037\u0033\u0035\u004f\u0077\u006d\u0055\u004c\u006f\u0067\u007a\u007a\u0058\u0054\u0035\u006c\u005a\u0071\u0058\u0050\u0032\u0052\u0077\u006b\u0064\u006d\u0075\u0048\u0032\u0044\u0077\u0048\u005a\u0066\u0056\u0038\u0059\u0067\u0031\u0031\u0062\u0062\u0056\u0053\u0042\u0037\u0062\u0031\u0064\u0057\u0032\u0070\u0075\u0071\u0073\u0074\u0047\u0074\u0043\u004e\u0058\u006e\u006e\u0051\u003d\u003d\u0022\u003b\u000a\u0020\u0020\u0020\u0020\u0074\u0072\u0079\u0020\u007b\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u0061\u006e\u0064\u0072\u006f\u0069\u0064\u002e\u0063\u006f\u006e\u0074\u0065\u006e\u0074\u002e\u0070\u006d\u002e\u0050\u0061\u0063\u006b\u0061\u0067\u0065\u0049\u006e\u0066\u006f\u0020\u0070\u0061\u0063\u006b\u0061\u0067\u0065\u0049\u006e\u0066\u006f\u0020\u003d\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0063\u006f\u006e\u0074\u0065\u0078\u0074\u002e\u0067\u0065\u0074\u0050\u0061\u0063\u006b\u0061\u0067\u0065\u004d\u0061\u006e\u0061\u0067\u0065\u0072\u0028\u0029\u002e\u0067\u0065\u0074\u0050\u0061\u0063\u006b\u0061\u0067\u0065\u0049\u006e\u0066\u006f\u0028\u0063\u006f\u006e\u0074\u0065\u0078\u0074\u002e\u0067\u0065\u0074\u0050\u0061\u0063\u006b\u0061\u0067\u0065\u004e\u0061\u006d\u0065\u0028\u0029\u002c\u0020\u0030\u0029\u003b\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u0053\u0074\u0072\u0069\u006e\u0067\u0020\u0070\u0061\u0063\u006b\u0061\u0067\u0065\u004e\u0061\u006d\u0065\u0020\u003d\u0020\u0070\u0061\u0063\u006b\u0061\u0067\u0065\u0049\u006e\u0066\u006f\u002e\u0070\u0061\u0063\u006b\u0061\u0067\u0065\u004e\u0061\u006d\u0065\u003b\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u0053\u0074\u0072\u0069\u006e\u0067\u0020\u0076\u0065\u0072\u0073\u0069\u006f\u006e\u004e\u0061\u006d\u0065\u0020\u003d\u0020\u0070\u0061\u0063\u006b\u0061\u0067\u0065\u0049\u006e\u0066\u006f\u002e\u0076\u0065\u0072\u0073\u0069\u006f\u006e\u004e\u0061\u006d\u0065\u003b\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u0053\u0074\u0072\u0069\u006e\u0067\u0020\u0074\u006f\u0048\u0061\u0073\u0068\u0020\u003d\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u006b\u006f\u0074\u006c\u0069\u006e\u002e\u0074\u0065\u0078\u0074\u002e\u0053\u0074\u0072\u0069\u006e\u0067\u0073\u004b\u0074\u002e\u0072\u0065\u0076\u0065\u0072\u0073\u0065\u0064\u0028\u0070\u0061\u0063\u006b\u0061\u0067\u0065\u004e\u0061\u006d\u0065\u0020\u002b\u0020\u0022\u005f\u0022\u0020\u002b\u0020\u0076\u0065\u0072\u0073\u0069\u006f\u006e\u004e\u0061\u006d\u0065\u0020\u002b\u0020\u0022\u0046\u0069\u0067\u0068\u0074\u0034\u0033\u0064\u006f\u006d\u0022\u0029\u002e\u0074\u006f\u0053\u0074\u0072\u0069\u006e\u0067\u0028\u0029\u003b\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u006a\u0061\u0076\u0061\u002e\u0073\u0065\u0063\u0075\u0072\u0069\u0074\u0079\u002e\u004d\u0065\u0073\u0073\u0061\u0067\u0065\u0044\u0069\u0067\u0065\u0073\u0074\u0020\u0073\u0068\u0061\u0035\u0031\u0032\u0020\u003d\u0020\u006a\u0061\u0076\u0061\u002e\u0073\u0065\u0063\u0075\u0072\u0069\u0074\u0079\u002e\u004d\u0065\u0073\u0073\u0061\u0067\u0065\u0044\u0069\u0067\u0065\u0073\u0074\u002e\u0067\u0065\u0074\u0049\u006e\u0073\u0074\u0061\u006e\u0063\u0065\u0028\u0022\u0073\u0068\u0061\u002d\u0035\u0031\u0032\u0022\u0029\u003b\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u0062\u0079\u0074\u0065\u005b\u005d\u0020\u0068\u0061\u0073\u0068\u0020\u003d\u0020\u0073\u0068\u0061\u0035\u0031\u0032\u002e\u0064\u0069\u0067\u0065\u0073\u0074\u0028\u0074\u006f\u0048\u0061\u0073\u0068\u002e\u0067\u0065\u0074\u0042\u0079\u0074\u0065\u0073\u0028\u006b\u006f\u0074\u006c\u0069\u006e\u002e\u0074\u0065\u0078\u0074\u002e\u0043\u0068\u0061\u0072\u0073\u0065\u0074\u0073\u002e\u0055\u0054\u0046\u005f\u0038\u0029\u0029\u003b\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u0069\u0066\u0020\u0028\u0021\u0061\u006e\u0064\u0072\u006f\u0069\u0064\u002e\u0075\u0074\u0069\u006c\u002e\u0042\u0061\u0073\u0065\u0036\u0034\u002e\u0065\u006e\u0063\u006f\u0064\u0065\u0054\u006f\u0053\u0074\u0072\u0069\u006e\u0067\u0028\u0068\u0061\u0073\u0068\u002c\u0020\u0061\u006e\u0064\u0072\u006f\u0069\u0064\u002e\u0075\u0074\u0069\u006c\u002e\u0042\u0061\u0073\u0065\u0036\u0034\u002e\u004e\u004f\u005f\u0057\u0052\u0041\u0050\u0029\u002e\u0065\u0071\u0075\u0061\u006c\u0073\u0028\u0065\u0078\u0070\u0065\u0063\u0074\u0065\u0064\u0029\u0029\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0074\u0068\u0072\u006f\u0077\u0020\u006e\u0065\u0077\u0020\u0049\u006c\u006c\u0065\u0067\u0061\u006c\u0041\u0072\u0067\u0075\u006d\u0065\u006e\u0074\u0045\u0078\u0063\u0065\u0070\u0074\u0069\u006f\u006e\u0028\u0022\u0049\u006e\u0076\u0061\u006c\u0069\u0064\u0020\u0070\u0061\u0063\u006b\u0061\u0067\u0065\u0020\u006e\u0061\u006d\u0065\u0020\u0061\u006e\u0064\u002f\u006f\u0072\u0020\u0076\u0065\u0072\u0073\u0069\u006f\u006e\u0022\u0029\u003b\u000a\u0020\u0020\u0020\u0020\u007d\u0020\u0063\u0061\u0074\u0063\u0068\u0020\u0028\u0045\u0078\u0063\u0065\u0070\u0074\u0069\u006f\u006e\u0020\u0061\u006e\u0079\u0045\u0078\u0063\u0065\u0070\u0074\u0069\u006f\u006e\u0043\u0061\u0075\u0067\u0068\u0074\u0029\u0020\u007b\u000a\u0020\u0020\u0020\u0020\u0020\u0020\u0074\u0068\u0072\u006f\u0077\u0020\u006e\u0065\u0077\u0020\u0049\u006c\u006c\u0065\u0067\u0061\u006c\u0041\u0072\u0067\u0075\u006d\u0065\u006e\u0074\u0045\u0078\u0063\u0065\u0070\u0074\u0069\u006f\u006e\u0028\u0022\u0049\u006e\u0076\u0061\u006c\u0069\u0064\u0020\u0070\u0061\u0063\u006b\u0061\u0067\u0065\u0020\u006e\u0061\u006d\u0065\u0020\u0061\u006e\u0064\u002f\u006f\u0072\u0020\u0076\u0065\u0072\u0073\u0069\u006f\u006e\u0022\u0029\u003b\u000a\u0020\u0020\u0020\u0020\u007d\u002f\u002a*/
    }
  }
}
