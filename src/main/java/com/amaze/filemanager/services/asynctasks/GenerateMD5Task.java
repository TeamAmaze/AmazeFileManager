package com.amaze.filemanager.services.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.ui.CircleAnimation;
import com.amaze.filemanager.ui.views.SizeDrawable;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.GenericCopyUtil;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Vishal on 05-02-2015.
 */
public class GenerateMD5Task extends AsyncTask<String, String, String[]> {

    private MaterialDialog a;
    private String name, parent, size, items, date;
    private HFile f;
    Context c;
    String sizeString;
    View textView;
    SizeDrawable sizeDrawable;
    GenerateMD5Task g = this;
    TextView t5, t6, t7, t8, t9, t10;
    TextView md5TextView, sha256TextView;

    public GenerateMD5Task(MaterialDialog a, HFile f, String name, String parent,
                           String size, String items, String date, final Context c, final View textView) {
        this.a = a;
        this.c = c;
        this.f = f;
        this.name = name;
        this.parent = parent;
        this.size = size;
        this.items = items;
        this.date = date;
        this.textView = textView;
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
        md5TextView = (TextView) textView.findViewById(R.id.md5);
        sha256TextView = (TextView) textView.findViewById(R.id.sha256);
        if (!f.isDirectory()) {
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

    }

    @Override
    protected String[] doInBackground(String... params) {
        String param = params[0];
        if (f.isDirectory()) {
            int x = f.listFiles(false).size();
            items = x + " " + c.getResources().getString(x == 0 ? R.string.item : R.string.items);
        } else {
            items = Formatter.formatFileSize(c, f.length());
        }
        publishProgress("");
        String md5 = "";
        String sha256 = "";
        try {
            if (!f.isDirectory()) {
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

            if (!f.isDirectory()) {
                t9.setText(aVoid[0]);
                t10.setText(aVoid[1]);
            } else {
                t9.setVisibility(View.GONE);
                t10.setVisibility(View.GONE);
            }
            if (f.isDirectory())
                a.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
            else {
                a.getActionButton(DialogAction.NEGATIVE).setEnabled(true);
                a.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Futils.copyToClipboard(c, aVoid[1]);
                            Toast.makeText(c, c.getResources().getString(R.string.hash_sha256_copied), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

        // see this How-to for a faster way to convert
        // a byte array to a HEX string

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
        InputStream inputStream = f.getInputStream();
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
        InputStream fis = f.getInputStream();

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
