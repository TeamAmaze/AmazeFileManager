/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
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

package com.amaze.filemanager.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.preference_fragments.ColorPref;
import com.amaze.filemanager.fragments.preference_fragments.Preffrag;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import org.sufficientlysecure.donations.DonationsFragment;

import java.util.Arrays;
import java.util.Calendar;

public class Preferences extends AppCompatActivity  implements ActivityCompat.OnRequestPermissionsResultCallback  {
    int theme, skinStatusBar;
    String skin, fabSkin;
    int select=0;
    public int changed=0;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences Sp = PreferenceManager.getDefaultSharedPreferences(this);
        fabSkin = PreferenceUtils.getAccentString(Sp);

        int th = Integer.parseInt(Sp.getString("theme", "0"));

        theme = th==2 ? PreferenceUtils.hourOfDay() : th;

        // setting accent theme
        if (Build.VERSION.SDK_INT >= 21) {

            switch (fabSkin) {
                case "#F44336":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_red);
                    else
                        setTheme(R.style.pref_accent_dark_red);
                    break;

                case "#e91e63":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_pink);
                    else
                        setTheme(R.style.pref_accent_dark_pink);
                    break;

                case "#9c27b0":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_purple);
                    else
                        setTheme(R.style.pref_accent_dark_purple);
                    break;

                case "#673ab7":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_deep_purple);
                    else
                        setTheme(R.style.pref_accent_dark_deep_purple);
                    break;

                case "#3f51b5":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_indigo);
                    else
                        setTheme(R.style.pref_accent_dark_indigo);
                    break;

                case "#2196F3":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_blue);
                    else
                        setTheme(R.style.pref_accent_dark_blue);
                    break;

                case "#03A9F4":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_light_blue);
                    else
                        setTheme(R.style.pref_accent_dark_light_blue);
                    break;

                case "#00BCD4":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_cyan);
                    else
                        setTheme(R.style.pref_accent_dark_cyan);
                    break;

                case "#009688":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_teal);
                    else
                        setTheme(R.style.pref_accent_dark_teal);
                    break;

                case "#4CAF50":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_green);
                    else
                        setTheme(R.style.pref_accent_dark_green);
                    break;

                case "#8bc34a":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_light_green);
                    else
                        setTheme(R.style.pref_accent_dark_light_green);
                    break;

                case "#FFC107":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_amber);
                    else
                        setTheme(R.style.pref_accent_dark_amber);
                    break;

                case "#FF9800":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_orange);
                    else
                        setTheme(R.style.pref_accent_dark_orange);
                    break;

                case "#FF5722":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_deep_orange);
                    else
                        setTheme(R.style.pref_accent_dark_deep_orange);
                    break;

                case "#795548":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_brown);
                    else
                        setTheme(R.style.pref_accent_dark_brown);
                    break;

                case "#212121":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_black);
                    else
                        setTheme(R.style.pref_accent_dark_black);
                    break;

                case "#607d8b":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_blue_grey);
                    else
                        setTheme(R.style.pref_accent_dark_blue_grey);
                    break;

                case "#004d40":
                    if (theme==0)
                        setTheme(R.style.pref_accent_light_super_su);
                    else
                        setTheme(R.style.pref_accent_dark_super_su);
                    break;
            }
        } else {
            if (theme==1) {
                setTheme(R.style.appCompatDark);
            } else {
                setTheme(R.style.appCompatLight);
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.prefsfrag);
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
        skin = PreferenceUtils.getPrimaryColorString(Sp);
        if (Build.VERSION.SDK_INT>=21) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription("Amaze", ((BitmapDrawable)getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap(), Color.parseColor(skin));
            ((Activity)this).setTaskDescription(taskDescription);
        }
        skinStatusBar = PreferenceUtils.getStatusColor(skin);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_HOME_AS_UP| android.support.v7.app.ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(skin)));
        int sdk=Build.VERSION.SDK_INT;

        if(sdk==20 || sdk==19) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(Color.parseColor(skin));

            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) findViewById(R.id.preferences).getLayoutParams();
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            p.setMargins(0, config.getStatusBarHeight(), 0, 0);
        }else if(Build.VERSION.SDK_INT>=21){
            boolean colourednavigation=Sp.getBoolean("colorednavigation",true);
            Window window =getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor((PreferenceUtils.getStatusColor(skin)));
            if(colourednavigation)
                window.setNavigationBarColor((PreferenceUtils.getStatusColor(skin)));

        }
        selectItem(0);
    }

    @Override
    public void onBackPressed() {
        if(select==1 && changed==1)
            restartPC(this);
        else if(select==1 || select==2){selectItem(0);}
        else{
            Intent in = new Intent(Preferences.this, MainActivity.class);
            in.setAction(Intent.ACTION_MAIN);
            final int enter_anim = android.R.anim.fade_in;
            final int exit_anim = android.R.anim.fade_out;
            Activity activity = this;
            activity.overridePendingTransition(enter_anim, exit_anim);
            activity.finish();
            activity.overridePendingTransition(enter_anim, exit_anim);
            activity.startActivity(in);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Navigate "up" the demo structure to the launchpad activity.
                if(select==1 && changed==1)
                    restartPC(this);
                else if(select==1 ){selectItem(0);}
                else{
                Intent in = new Intent(Preferences.this, MainActivity.class);
                in.setAction(Intent.ACTION_MAIN);
                final int enter_anim = android.R.anim.fade_in;
                final int exit_anim = android.R.anim.fade_out;
                Activity activity = this;
                activity.overridePendingTransition(enter_anim, exit_anim);
                    activity.finish();
                activity.overridePendingTransition(enter_anim, exit_anim);
                activity.startActivity(in);
            }return true;

        }
        return true;
    }
    private static final String GOOGLE_PUBKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAo9hApxv/pAZAUQshPiQEX2L6ZPoifEUw9fuisAxZFOHpW83mcRbWDmcqdouc1JqHak0/J0tZEBMc4SqSngE+xK3NxS2Mf4uwXPhD40bC1InAKtGNOJllGXKS8RRmk2FDD33ZHrdFUcJuKL6EIXHl1bwFIrd9rvr5VRt1mvXGj+iSdZe1WQpLex/f/s+eEe1B046Z/U6YNoPvP7xFezbZr3F1kRsx4WD5fcrTdptn38BXcwabJ1T/c2fLuGjUCZbycrggqJS47zEJ+SJhJpQUJWabq0sEYAHlyVN0CR0AVTd4/+y4+hFuPaYkhT4u/H5Uvd78u0VQdljzDs4w8mS++QIDAQAB";
    private static final String[] GOOGLE_CATALOG = new String[]{"ntpsync.donation","ntpsync.donation.2", "ntpsync.donation.13"};
    public void donate(){
        try {
            getFragmentManager().beginTransaction().remove(p).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String[] s=new String[]{"Minimal Donation","Medium Donation","High Donation"};
        DonationsFragment donationsFragment = DonationsFragment.newInstance(BuildConfig.DEBUG, true, GOOGLE_PUBKEY, GOOGLE_CATALOG,
                s, false, null, null,
                null, false, null, null, false, null);
        android.support.v4.app.FragmentTransaction transaction =getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.prefsfragment, donationsFragment);
        transaction.commit();
        getSupportActionBar().setTitle(R.string.donate);

    }
    public  void restartPC(final Activity activity) {
        if (activity == null)
            return;
        final int enter_anim = android.R.anim.fade_in;
        final int exit_anim = android.R.anim.fade_out;
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.finish();
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.startActivity(activity.getIntent());
    }
    Preffrag p;
    public void selectItem(int i){
        switch (i){
            case 0:
                p=new Preffrag();
                FragmentTransaction transaction =getFragmentManager().beginTransaction();
                transaction.replace(R.id.prefsfragment,p );
                transaction.commit();
                select=0;
                getSupportActionBar().setTitle(R.string.setting);
                break;
            case 1:
                FragmentTransaction transaction1 =getFragmentManager().beginTransaction();
                transaction1.replace(R.id.prefsfragment, new ColorPref());
                transaction1.commit();
                select=1;
                getSupportActionBar().setTitle(R.string.color_title);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 66) {
                p.invalidateGplus();
            }

        }
}
