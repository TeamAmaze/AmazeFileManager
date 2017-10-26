package com.amaze.filemanager.activities;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.superclasses.BasicActivity;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.util.Random;

/**
 * Created by vishal on 27/7/16.
 */
public class AboutActivity extends BasicActivity implements View.OnClickListener {

    private static final int HEADER_HEIGHT = 1024;
    private static final int HEADER_WIDTH = 500;

    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private TextView mTitleTextView;
    private int mCount=0;
    private Snackbar snackbar;
    private SharedPreferences mSharedPref;
    private View mAuthorsDivider;

    private static final String KEY_PREF_STUDIO = "studio";
    private static final String URL_AUTHOR_1_G_PLUS = "https://plus.google.com/u/0/110424067388738907251/";
    private static final String URL_AUTHOR_1_PAYPAL = "arpitkh96@gmail.com";
    private static final String URL_AUTHOR_2_G_PLUS = "https://plus.google.com/+VishalNehra/";
    private static final String URL_AUTHOR_2_PAYPAL = "https://www.paypal.me/vishalnehra";
    private static final String URL_DEVELOPER1_GITHUB = "https://github.com/EmmanuelMess";
    private static final String URL_DEVELOPER1_BITCOIN = "bitcoin:12SRnoDQvDD8aoCy1SVSn6KSdhQFvRf955?amount=0.0005";
    private static final String URL_REPO_CHANGELOG = "https://github.com/TeamAmaze/AmazeFileManager/commits/master";
    private static final String URL_REPO_ISSUES = "https://github.com/TeamAmaze/AmazeFileManager/issues";
    private static final String URL_REPO_TRANSLATE = "https://www.transifex.com/amaze/amaze-file-manager-1/";
    private static final String URL_REPO_G_PLUS_COMMUNITY = "https://plus.google.com/communities/113997576965363268101";
    private static final String URL_REPO_XDA = "http://forum.xda-developers.com/android/apps-games/app-amaze-file-managermaterial-theme-t2937314";
    private static final String URL_REPO_RATE = "market://details?id=com.amaze.filemanager";
    private static final String TAG_CLIPBOARD_DONATE = "donate_id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getAppTheme().equals(AppTheme.DARK)) {
            setTheme(R.style.aboutDark);
        } else {
            setTheme(R.style.aboutLight);
        }

        setContentView(R.layout.activity_about);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        mAppBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_layout);
        mTitleTextView = (TextView) findViewById(R.id.text_view_title);
        ImageView mLicensesIcon = (ImageView) findViewById(R.id.image_view_license);;
        mAuthorsDivider = findViewById(R.id.view_divider_authors);

        mAppBarLayout.setLayoutParams(calculateHeaderViewParams());

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(getResources().getDrawable(R.drawable.md_nav_back));
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        switchIcons();

        // license icon easter
        Random random = new Random();
        if (random.nextInt(2) == 0) {
            mLicensesIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_apple_ios_grey600_24dp));
        }

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.about_header);

        // It will generate colors based on the image in an AsyncTask.
        Palette.from(bitmap).generate(palette -> {
            int mutedColor = palette.getMutedColor(Utils.getColor(AboutActivity.this, R.color.primary_blue));
            int darkMutedColor = palette.getDarkMutedColor(Utils.getColor(AboutActivity.this, R.color.primary_blue));
            mCollapsingToolbarLayout.setContentScrimColor(mutedColor);
            mCollapsingToolbarLayout.setStatusBarScrimColor(darkMutedColor);
        });

        mAppBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            mTitleTextView.setAlpha(Math.abs(verticalOffset / (float) appBarLayout.getTotalScrollRange()));
        });
    }

    /**
     * Calculates aspect ratio for the Amaze header
     * @return the layout params with new set of width and height attribute
     */
    private CoordinatorLayout.LayoutParams calculateHeaderViewParams() {

        // calculating cardview height as per the youtube video thumb aspect ratio
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) mAppBarLayout.getLayoutParams();
        float vidAspectRatio = (float) HEADER_WIDTH / (float) HEADER_HEIGHT;
        Log.d(getClass().getSimpleName(), vidAspectRatio + "");
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        float reqHeightAsPerAspectRatio = (float) screenWidth *vidAspectRatio;
        Log.d(getClass().getSimpleName(), reqHeightAsPerAspectRatio + "");


        Log.d(getClass().getSimpleName(), "new width: " + screenWidth + " and height: " + reqHeightAsPerAspectRatio);

        layoutParams.width = screenWidth;
        layoutParams.height = (int) reqHeightAsPerAspectRatio;
        return layoutParams;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Method switches icon resources as per current theme
     */
    private void switchIcons() {
        if (getAppTheme().equals(AppTheme.DARK)) {
            // dark theme
            mAuthorsDivider.setBackgroundColor(Utils.getColor(this, R.color.divider_dark_card));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.relative_layout_version:
                mCount++;
                if (mCount >= 5) {
                    String text = getResources().getString(R.string.easter_egg_title) + " : " + mCount;

                    if(snackbar != null && snackbar.isShown()) {
                        snackbar.setText(text);
                    } else {
                        snackbar = Snackbar.make(v, text, Snackbar.LENGTH_SHORT);
                    }

                    snackbar.show();
                    mSharedPref.edit().putInt(KEY_PREF_STUDIO, Integer.parseInt(Integer.toString(mCount) + "000")).apply();
                } else {
                    mSharedPref.edit().putInt(KEY_PREF_STUDIO, 0).apply();
                }
                break;

            case R.id.relative_layout_issues:
                openURL(URL_REPO_ISSUES);
                break;

            case R.id.relative_layout_changelog:
                openURL(URL_REPO_CHANGELOG);
                break;

            case R.id.relative_layout_licenses:
                Dialog dialog = new Dialog(this, android.R.style.Theme_Holo_Light);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                final View dialog_view = getLayoutInflater().inflate(R.layout.open_source_licenses, null);
                WebView wv = (WebView) dialog_view.findViewById(R.id.webView1);
                dialog.setContentView(dialog_view);
                wv.loadData(PreferenceUtils.LICENCE_TERMS, "text/html", null);
                dialog.show();
                break;

            case R.id.text_view_author_1_g_plus:
                openURL(URL_AUTHOR_1_G_PLUS);
                break;

            case R.id.text_view_author_1_donate:
                ClipboardManager clipManager1 = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip1 = ClipData.newPlainText(TAG_CLIPBOARD_DONATE, URL_AUTHOR_1_PAYPAL);
                clipManager1.setPrimaryClip(clip1);
                Snackbar.make(v, R.string.paypal_copy_message, Snackbar.LENGTH_LONG).show();
                break;

            case R.id.text_view_author_2_g_plus:
                openURL(URL_AUTHOR_2_G_PLUS);
                break;

            case R.id.text_view_author_2_donate:
                openURL(URL_AUTHOR_2_PAYPAL);
                break;

            case R.id.text_view_developer_1_github:
                openURL(URL_DEVELOPER1_GITHUB);
                break;

            case R.id.text_view_developer_1_donate:
                try {
                    openURL(URL_DEVELOPER1_BITCOIN);
                } catch (ActivityNotFoundException e) {
                    Snackbar.make(v, R.string.nobitcoinapp, Snackbar.LENGTH_LONG).show();
                }
                break;

            case R.id.relative_layout_translate:
                openURL(URL_REPO_TRANSLATE);
                break;

            case R.id.relative_layout_g_plus_community:
                openURL(URL_REPO_G_PLUS_COMMUNITY);
                break;

            case R.id.relative_layout_xda:
                openURL(URL_REPO_XDA);
                break;

            case R.id.relative_layout_rate:
                openURL(URL_REPO_RATE);
                break;
        }
    }

    private void openURL(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

}
