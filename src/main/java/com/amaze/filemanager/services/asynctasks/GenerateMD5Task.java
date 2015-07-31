package com.amaze.filemanager.services.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.CircleAnimation;
import com.amaze.filemanager.ui.views.SizeDrawable;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.HFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Created by Vishal on 05-02-2015.
 */
public class GenerateMD5Task extends AsyncTask<String, String, String> {

    private MaterialDialog a;
    private String name, parent, size, items, date;
    private HFile f;
    Context c;
    String md5 = "";
    TextView textView, textView1;
    SizeDrawable sizeDrawable;
    GenerateMD5Task g = this;

    public GenerateMD5Task(MaterialDialog a, HFile f, String name, String parent,
                           String size, String items, String date, Context c, TextView textView) {

        this.a = a;
        this.c = c;
        this.f = f;
        this.name = name;
        this.parent = parent;
        this.size = size;
        this.items = items;
        this.date = date;
        this.textView = textView;
    }

    public GenerateMD5Task(MaterialDialog a, HFile f, String name, String parent,
                           String size, String items, String date, Context c, TextView textView,
                           SizeDrawable sizeDrawable, TextView textView1) {
        this.a = a;
        this.c = c;
        this.f = f;
        this.name = name;
        this.parent = parent;
        this.size = size;
        this.items = items;
        this.date = date;
        this.textView = textView;
        this.textView1 = textView1;
        this.sizeDrawable = sizeDrawable;
        new AsyncTask<Void, Void, long[]>() {
            @Override
            protected long[] doInBackground(Void... voids) {
                Futils futils = new Futils();
                long[] longs = futils.getSpaces(g.f.getPath());
                return longs;
            }

            @Override
            protected void onPostExecute(long[] longs) {
                super.onPostExecute(longs);
                Futils futils = new Futils();
                if (longs[0] != -1 && longs[0]!=0 && g.textView1 != null) {
                    float r1 = (longs[0] - longs[1]) * 360 / longs[0];
                    float r2=(longs[2]) * 360 / longs[0];
                    g.textView1.setText("Total "+futils.readableFileSize(longs[0]) + "\n" +"Free "+
                            futils
                            .readableFileSize
                                    (longs[1]) + "\n" +"Used "+ futils.readableFileSize(longs[0] -
                                    longs[1])+"\n"+"Folder Size "+futils.readableFileSize
                            (longs[2]));

                    CircleAnimation animation = new CircleAnimation(g.sizeDrawable, r1,r2);
                    animation.setDuration(Math.round(r1 * 5));
                    g.sizeDrawable.startAnimation(animation);
                } else {
                    g.sizeDrawable.setVisibility(View.GONE);
                    g.textView1.setVisibility(View.GONE);
                }

            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onProgressUpdate(String... ab) {
        if (a != null && a.isShowing()) {
            textView.setText(name + "\n" + parent + "\n" + size +"\n" + date);
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        textView.setText(name + "\n" + parent + "\n" + size + "\n"
                + date);
        a.getActionButton(DialogAction.NEGATIVE).setEnabled(false);

    }

    @Override
    protected String doInBackground(String... params) {
        String param = params[0];
        Futils futils = new Futils();
        if (f.isDirectory()) {
            items =  futils.getString(c, R.string.totalitems) + f.listFiles(false).size();
        } else {
            items = "";
            size = futils.getString(c, R.string.size) + futils.readableFileSize(f.length());
        }
        publishProgress("");
        String md5 = "";
        try {
            if (!f.isDirectory()) md5 = getMD5Checksum(param);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return md5;
    }

    @Override
    protected void onPostExecute(String aVoid) {
        super.onPostExecute(aVoid);
        if (a.isShowing()) {
            md5 = aVoid;
            if (f.isDirectory())
                textView.setText(name + "\n" + parent  + "\n" + items + "\n"
                        + date );
            else
                textView.setText(name + "\n" + parent + "\n" + size  + "\n"
                        + date + "\n" + "md5:" + aVoid);
            if (f.isDirectory())
                a.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
            else {
                a.getActionButton(DialogAction.NEGATIVE).setEnabled(true);
                a.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            new Futils().copyToClipboard(c, md5);
                            Toast.makeText(c, c.getResources().getString(R.string.pathcopied), Toast.LENGTH_SHORT).show();
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
    public static String getMD5Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";

        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static byte[] createChecksum(String filename) throws Exception {
        InputStream fis = new FileInputStream(filename);

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
