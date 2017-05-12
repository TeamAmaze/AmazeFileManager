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
import com.amaze.filemanager.filesystem.BaseFile;
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

    private MaterialDialog dialog;
    private String name, parent, items, date;
    private BaseFile f;
    private BaseActivity c;
    private GenerateMD5Task g = this;
    private TextView nameText, dirText, itemCountText, creationDateText, md5HashText, sha256Text;
    private LinearLayout mNameLinearLayout, mSizeLinearLayout, mLocationLinearLayout, mDateLinearLayout;
    private LinearLayout mMD5LinearLayout, mSHA256LinearLayout;

    public GenerateMD5Task(MaterialDialog dialog, BaseFile f, String name, String parent,
                           String items, String date, final BaseActivity c, final View textView) {
        this.dialog = dialog;
        this.c = c;
        this.f = f;
        this.name = name;
        this.parent = parent;
        this.items = items;
        this.date = date;
        nameText = (TextView) textView.findViewById(R.id.t5);
        dirText = (TextView) textView.findViewById(R.id.t6);
        itemCountText = (TextView) textView.findViewById(R.id.t7);
        creationDateText = (TextView) textView.findViewById(R.id.t8);
        md5HashText = (TextView) textView.findViewById(R.id.t9);
        sha256Text = (TextView) textView.findViewById(R.id.t10);

        int accentColor = c.getColorPreference().getColor(ColorUsage.ACCENT);
        TextView md5TextView = (TextView) textView.findViewById(R.id.text_view_properties_dialog_title_md5);
        md5TextView.setTextColor(accentColor);
        TextView sha256TextView = (TextView) textView.findViewById(R.id.text_view_properties_dialog_title_sha256);
        sha256TextView.setTextColor(accentColor);
        TextView mNameTitle = (TextView) textView.findViewById(R.id.text_view_properties_dialog_title_name);
        mNameTitle.setTextColor(accentColor);
        TextView mDateTitle = (TextView) textView.findViewById(R.id.text_view_properties_dialog_title_date);
        mDateTitle.setTextColor(accentColor);
        TextView mSizeTitle = (TextView) textView.findViewById(R.id.text_view_properties_dialog_title_size);
        mSizeTitle.setTextColor(accentColor);
        TextView mLocationTitle = (TextView) textView.findViewById(R.id.text_view_properties_dialog_title_location);
        mLocationTitle.setTextColor(accentColor);

        mNameLinearLayout = (LinearLayout) textView.findViewById(R.id.properties_dialog_name);
        mLocationLinearLayout = (LinearLayout) textView.findViewById(R.id.properties_dialog_location);
        mSizeLinearLayout = (LinearLayout) textView.findViewById(R.id.properties_dialog_size);
        mDateLinearLayout = (LinearLayout) textView.findViewById(R.id.properties_dialog_date);
        mMD5LinearLayout = (LinearLayout) textView.findViewById(R.id.properties_dialog_md5);
        mSHA256LinearLayout = (LinearLayout) textView.findViewById(R.id.properties_dialog_sha256);
    }

    @Override
    public void onProgressUpdate(String... ab) {
        if (dialog != null && dialog.isShowing()) {
            itemCountText.setText(items);
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        nameText.setText(name);
        dirText.setText(parent);
        itemCountText.setText(items);
        creationDateText.setText(date);
        dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(false);

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
            items = Formatter.formatFileSize(c, f.length(c)) + (" (" + f.length(c) + " "
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
    protected void onPostExecute(final String[] hashes) {
        super.onPostExecute(hashes);
        if (dialog.isShowing()) {

            if (!f.isDirectory()) {
                md5HashText.setText(hashes[0]);
                sha256Text.setText(hashes[1]);

                mMD5LinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {

                        Futils.copyToClipboard(c, hashes[0]);
                        Toast.makeText(c, c.getResources().getString(R.string.md5).toUpperCase() + " " +
                                c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                });
                mSHA256LinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {

                        Futils.copyToClipboard(c, hashes[1]);
                        Toast.makeText(c, c.getResources().getString(R.string.hash_sha256) + " " +
                                c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                });
            } else {
                mMD5LinearLayout.setVisibility(View.GONE);
                mSHA256LinearLayout.setVisibility(View.GONE);
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
