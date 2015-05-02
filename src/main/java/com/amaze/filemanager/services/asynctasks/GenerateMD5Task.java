package com.amaze.filemanager.services.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.Futils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Created by Vishal on 05-02-2015.
 */
public class GenerateMD5Task extends AsyncTask<String, String, String> {

    private MaterialDialog a;
    private MaterialDialog b;
    private String name, parent, size, items, date;
    private File f;
    Context c;
    String md5="";
    public GenerateMD5Task(MaterialDialog a, File f, String name, String parent,
                           String size, String items, String date,Context c) {

        this.a = a;
        this.c=c;
        this.f = f;
        this.name = name;
        this.parent = parent;
        this.size = size;
        this.items = items;
        this.date = date;
    }
    @Override
    public  void onProgressUpdate(String... ab){
        if(a!=null && a.isShowing()){
            a.setContent(name + "\n" + parent + "\n" + size + "\n" + items + "\n"
                    + date + "\n" + "md5: generating..");
        }
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        a.setContent(name + "\n" + parent + "\n" + size + "\n" + items + "\n"
                + date + "\n" + "md5: generating..");
        Log.d("test", "testing onPreExecute");
        a.getActionButton(DialogAction.NEGATIVE).setEnabled(false);

    }

    @Override
    protected String doInBackground(String... params) {
        String param = params[0];
        Futils futils=new Futils();
        if (f.isDirectory()) {
            size = futils.getString(c, R.string.size) + futils.readableFileSize(futils.folderSize(f));
            items = futils.getString(c, R.string.totalitems) + futils.count(f,c.getResources(),true);
        } else if (f.isFile()) {
            items = "";
            size =futils. getString(c, R.string.size) + futils.getSize(f);
        }publishProgress("");
        String md5 = "";
        try {
            if(!f.isDirectory())md5 = getMD5Checksum(param);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return md5;
    }

    @Override
    protected void onPostExecute(String aVoid) {
        super.onPostExecute(aVoid);
        if (a.isShowing()) {

            if (f.isDirectory())
                aVoid = " null";
            md5=aVoid;
            a.setContent(name + "\n" + parent + "\n" + size + "\n" + items + "\n"
                    + date + "\n" + "md5:" + aVoid);
            if(f.isDirectory())
                a.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
            else{a.getActionButton(DialogAction.NEGATIVE).setEnabled(true);
                a.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            new Futils().copyToClipboard(c,md5);
                            Toast.makeText(c, c.getResources().getString(R.string.pathcopied), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
           // a.show();
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
