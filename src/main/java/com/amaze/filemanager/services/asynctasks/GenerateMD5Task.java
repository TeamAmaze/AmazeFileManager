package com.amaze.filemanager.services.asynctasks;

import android.os.AsyncTask;
import android.text.format.Formatter;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.ui.CircleAnimation;
import com.amaze.filemanager.ui.views.SizeDrawable;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.GenericCopyUtil;
import com.amaze.filemanager.utils.color.ColorUsage;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Vishal on 05-02-2015.
 */
public class GenerateMD5Task extends AsyncTask<String, String, String[]> {

    private MaterialDialog a;
    private String name, parent, items, date;
    private HFile f;
    private BaseActivity c;
    private SizeDrawable sizeDrawable;
    private GenerateMD5Task g = this;
    private TextView t5, t6, t7, t8, t9, t10;
    private TextView mNameTitle, mLocationTitle, mDateTitle, mSizeTitle;
    private TextView md5TextView, sha256TextView;
    private LinearLayout mNameLinearLayout, mSizeLinearLayout, mLocationLinearLayout, mDateLinearLayout;
    private LinearLayout mMD5LinearLayout, mSHA256LinearLayout;
    private int accentColor;

    public GenerateMD5Task(MaterialDialog a, HFile f, String name, String parent,
                           String items, String date, final BaseActivity c, final View textView) {
        this.a = a;
        this.c = c;
        this.f = f;
        this.name = name;
        this.parent = parent;
        this.items = items;
        this.date = date;
        this.sizeDrawable = (SizeDrawable) textView.findViewById(R.id.sizedrawable);
        final TextView t1 = (TextView) textView.findViewById(R.id.t1);
        final TextView t2 = (TextView) textView.findViewById(R.id.t2);
        final TextView t3 = (TextView) textView.findViewById(R.id.t3);
        final TextView t4 = (TextView) textView.findViewById(R.id.t4);
        t5 = (TextView) textView.findViewById(R.id.t5);
        t6 = (TextView) textView.findViewById(R.id.t6);
        t7 = (TextView) textView.findViewById(R.id.t7);
        t8 = (TextView) textView.findViewById(R.id.t8);
        t9 = (TextView) textView.findViewById(R.id.t9);
        t10 = (TextView) textView.findViewById(R.id.t10);

        accentColor = c.getColorPreference().getColor(ColorUsage.ACCENT);
        md5TextView = (TextView) textView.findViewById(R.id.text_view_properties_dialog_title_md5);
        md5TextView.setTextColor(accentColor);
        sha256TextView = (TextView) textView.findViewById(R.id.text_view_properties_dialog_title_sha256);
        sha256TextView.setTextColor(accentColor);
        mNameTitle = (TextView) textView.findViewById(R.id.text_view_properties_dialog_title_name);
        mNameTitle.setTextColor(accentColor);
        mDateTitle = (TextView) textView.findViewById(R.id.text_view_properties_dialog_title_date);
        mDateTitle.setTextColor(accentColor);
        mSizeTitle = (TextView) textView.findViewById(R.id.text_view_properties_dialog_title_size);
        mSizeTitle.setTextColor(accentColor);
        mLocationTitle = (TextView) textView.findViewById(R.id.text_view_properties_dialog_title_location);
        mLocationTitle.setTextColor(accentColor);

        mNameLinearLayout = (LinearLayout) textView.findViewById(R.id.linear_layout_properties_dialog_name);
        mLocationLinearLayout = (LinearLayout) textView.findViewById(R.id.linear_layout_properties_dialog_location);
        mSizeLinearLayout = (LinearLayout) textView.findViewById(R.id.linear_layout_properties_dialog_size);
        mDateLinearLayout = (LinearLayout) textView.findViewById(R.id.linear_layout_properties_dialog_date);
        mMD5LinearLayout = (LinearLayout) textView.findViewById(R.id.linear_layout_properties_dialog_md5);
        mSHA256LinearLayout = (LinearLayout) textView.findViewById(R.id.linear_layout_properties_dialog_sha256);

        if (!f.isDirectory(c)) {
            textView.findViewById(R.id.divider).setVisibility(View.GONE);
            textView.findViewById(R.id.dirprops).setVisibility(View.GONE);
        } else {
            md5TextView.setVisibility(View.GONE);
            sha256TextView.setVisibility(View.GONE);

            new AsyncTask<Void, Void, long[]>() {
                @Override
                protected long[] doInBackground(Void... voids) {
                    return Futils.getSpaces(g.f);
                }

                @Override
                protected void onPostExecute(long[] longs) {
                    super.onPostExecute(longs);
                    if (longs[0] != -1 && longs[0] != 0) {
                        float r1 = (longs[0] - longs[1]) * 360 / longs[0];
                        float r2 = (longs[2]) * 360 / longs[0];
                        t1.setText(Formatter.formatFileSize(c, longs[0]));
                        t2.setText(Formatter.formatFileSize(c, longs[1]));
                        t3.setText(Formatter.formatFileSize(c, longs[0] - longs[1] - longs[2]));
                        t4.setText(Formatter.formatFileSize(c, longs[2]));

                        CircleAnimation animation = new CircleAnimation(g.sizeDrawable, r1, r2);
                        animation.setDuration(Math.round(r1 * 5));
                        g.sizeDrawable.startAnimation(animation);
                    } else {
                        textView.findViewById(R.id.divider).setVisibility(View.GONE);
                        textView.findViewById(R.id.dirprops).setVisibility(View.GONE);
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onProgressUpdate(String... ab) {
        if (a != null && a.isShowing()) {
            t7.setText(items);
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        t5.setText(name);
        t6.setText(parent);
        t7.setText(items);
        t8.setText(date);
        a.getActionButton(DialogAction.NEGATIVE).setEnabled(false);

        // setting click listeners for long press
        mNameLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                Futils.copyToClipboard(c, name);
                Toast.makeText(c, c.getResources().getString(R.string.name) + " " +
                        c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        mLocationLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                Futils.copyToClipboard(c, parent);
                Toast.makeText(c, c.getResources().getString(R.string.location) + " " +
                        c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        mSizeLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                Futils.copyToClipboard(c, items);
                Toast.makeText(c, c.getResources().getString(R.string.size) + " " +
                        c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        mDateLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                Futils.copyToClipboard(c, date);
                Toast.makeText(c, c.getResources().getString(R.string.date) + " " +
                        c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    @Override
    protected String[] doInBackground(String... params) {
        String param = params[0];
        if (f.isDirectory(c)) {
            int x = f.listFiles(c, false).size();
            items = x + " " + c.getResources().getString(x == 0 ? R.string.item : R.string.items);
        } else {
            items = Formatter.formatFileSize(c, f.length()) + (" (" + f.length() + " "
                    + c.getResources().getString(R.string.bytes).toLowerCase() + ")");
        }
        publishProgress("");
        String md5 = "";
        String sha256 = "";
        try {
            if (!f.isDirectory(c)) {
                md5 = getMD5Checksum(param);
                sha256 = getSHA256Checksum();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] hashes = new String[] {
                md5,
                sha256
        };
        return hashes;
    }

    @Override
    protected void onPostExecute(final String[] aVoid) {
        super.onPostExecute(aVoid);
        if (a.isShowing()) {

            if (!f.isDirectory(c)) {
                t9.setText(aVoid[0]);
                t10.setText(aVoid[1]);

                mMD5LinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {

                        Futils.copyToClipboard(c, aVoid[0]);
                        Toast.makeText(c, c.getResources().getString(R.string.md5).toUpperCase() + " " +
                                c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                });
                mSHA256LinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {

                        Futils.copyToClipboard(c, aVoid[1]);
                        Toast.makeText(c, c.getResources().getString(R.string.hash_sha256) + " " +
                                c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                });
            } else {
                t9.setVisibility(View.GONE);
                t10.setVisibility(View.GONE);
            }
        }
    }

    // see this How-to for a faster way to convert a byte array to a HEX string

    public String getMD5Checksum(String filename) throws Exception {
        byte[] b = createChecksum();
        String result = "";

        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    private String getSHA256Checksum() throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] input = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
        int length;
        InputStream inputStream = f.getInputStream(c);
        while ((length = inputStream.read(input)) != -1) {
            if (length > 0)
                messageDigest.update(input, 0, length);
        }

        byte[] hash = messageDigest.digest();

        StringBuffer hexString = new StringBuffer();

        for (int i=0; i<hash.length; i++) {
            // convert hash to base 16
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        inputStream.close();
        return hexString.toString();
    }

    public  byte[] createChecksum() throws Exception {
        InputStream fis = f.getInputStream(c);

        byte[] buffer = new byte[8192];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }
}
