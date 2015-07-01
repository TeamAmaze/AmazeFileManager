package com.amaze.filemanager.utils.share;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.widget.GridView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Arpit on 01-07-2015.
 */
public class ShareTask extends AsyncTask<String,String,Void> {
    Activity contextc;
    ArrayList<Uri> arrayList;
    ArrayList<Intent> targetShareIntents=new ArrayList<Intent>();
    ArrayList<String> arrayList1=new ArrayList<>();
    ArrayList<Drawable> arrayList2=new ArrayList<>();
    public ShareTask(Activity context,ArrayList<Uri> arrayList){
        this.contextc=context;
        this.arrayList=arrayList;
    }
    @Override
    protected Void doInBackground(String... strings) {
        String mime=strings[0];
        Intent shareIntent=new Intent();
        boolean bluetooth_present=false;
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setType(mime);
        PackageManager packageManager=contextc.getPackageManager();
        List<ResolveInfo> resInfos=packageManager.queryIntentActivities(shareIntent, 0);
        if(!resInfos.isEmpty()) {
            for (ResolveInfo resInfo : resInfos) {
                String packageName = resInfo.activityInfo.packageName;
                arrayList2.add(resInfo.loadIcon(packageManager));
                arrayList1.add(resInfo.loadLabel(packageManager).toString());
                if(packageName.contains("android.bluetooth"))bluetooth_present=true;
                Intent intent = new Intent();
                System.out.println(resInfo.activityInfo.packageName+"\t"+resInfo.activityInfo.name);
                intent.setComponent(new ComponentName(packageName, resInfo.activityInfo.name));
                intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                intent.setType(mime);
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayList);
                intent.setPackage(packageName);
                targetShareIntents.add(intent);

            }   }
        if(!bluetooth_present){
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity"));
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.setType(mime);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayList);
            intent.setPackage("com.android.bluetooth");
            targetShareIntents.add(intent);
            arrayList1.add("Bluetooth");
            arrayList2.add(contextc.getResources().getDrawable(R.drawable.ic_settings_bluetooth_white_36dp));
        }     return null;
    }public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = contextc.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }
    @Override
    public void onPostExecute(Void v){
        if (!targetShareIntents.isEmpty()) {

            MaterialDialog.Builder builder=new MaterialDialog.Builder(contextc);
            builder.title(R.string.share);
            GridView view=(GridView)contextc.getLayoutInflater().inflate(R.layout.dialog_grid,null);
            view.setColumnWidth(dpToPx(72));
            view.setVerticalSpacing(dpToPx(20));
            ShareAdapter shareAdapter=new ShareAdapter(contextc,targetShareIntents,arrayList1,arrayList2);
            view.setAdapter(shareAdapter);
            builder.customView(view,false);
            MaterialDialog b=builder.build();
            shareAdapter.updateMatDialog(b);
            b.show();
        } else {
            Toast.makeText(contextc, R.string.noappfound, Toast.LENGTH_SHORT).show();
        }
    }
}
