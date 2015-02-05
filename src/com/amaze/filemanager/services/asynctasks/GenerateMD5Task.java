package com.amaze.filemanager.services.asynctasks;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Created by Vishal on 05-02-2015.
 */
public class GenerateMD5Task extends AsyncTask<String, Void, String> {

    private MaterialDialog.Builder a;
    private MaterialDialog b;
    private String name, parent, size, items, date;
    private File f;

    public GenerateMD5Task(MaterialDialog.Builder a, File f, String name, String parent,
                           String size, String items, String date) {

        this.a = a;
        this.f = f;
        this.name = name;
        this.parent = parent;
        this.size = size;
        this.items = items;
        this.date = date;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        a.content(name + "\n" + parent + "\n" + size + "\n" + items + "\n"
                + date + "\n" + "md5: generating..");
        Log.d("test", "testing onPreExecute");
        b = a.build();
        b.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
        b.show();
    }

    @Override
    protected String doInBackground(String... params) {
        String param = params[0];
        String md5 = "";
        try {
            md5 = getMD5Checksum(param);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return md5;
    }

    @Override
    protected void onPostExecute(String aVoid) {
        super.onPostExecute(aVoid);
        if (b.isShowing()) {

            if (f.isDirectory())
                aVoid = " null";
            a.content(name + "\n" + parent + "\n" + size + "\n" + items + "\n"
                    + date + "\n" + "md5:" + aVoid);
            b.cancel();
            b = a.build();
            if(f.isDirectory())
                b.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
            b.show();
        }
        Log.d("test", "testing onPostExecute");
    }

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
    public static String getMD5Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";

        for (int i=0; i < b.length; i++) {
            result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }

    public static byte[] createChecksum(String filename) throws Exception {
        InputStream fis =  new FileInputStream(filename);

        byte[] buffer = new byte[1024];
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
