package com.amaze.filemanager.activities;

import android.app.Dialog;
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
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.PreferenceUtils;
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
    private Toolbar mToolbar;
    private TextView mTitleTextView, mAuthor1Gplus, mAuthor1Donate, mAuthor2Gplus, mAuthor2Donate;
    private RelativeLayout mVersion, mChangelog, mLicenses, mIssues, mTranslate, mGplusCommunity;
    private RelativeLayout mXda, mRate;
    private ImageView mLicensesIcon;
    private int mCount=0;
    private Toast mToast;
    private SharedPreferences mSharedPref;
    private View mAuthorsDivider;

    private static final String KEY_PREF_STUDIO = "studio";
    private static final String URL_AUTHOR_1_G_PLUS = "https://plus.google.com/u/0/110424067388738907251/";
    private static final String URL_AUTHOR_1_PAYPAL = "arpitkh96@gmail.com";
    private static final String URL_AUTHOR_2_G_PLUS = "https://plus.google.com/+VishalNehra/";
    private static final String URL_AUTHOR_2_PAYPAL = "vishalmeham2@gmail.com";
    private static final String URL_REPO_CHANGELOG = "https://github.com/arpitkh96/AmazeFileManager/commits/master";
    private static final String URL_REPO_ISSUES = "https://github.com/arpitkh96/AmazeFileManager/issues";
    private static final String URL_REPO_TRANSLATE = "https://www.transifex.com/amaze/amaze-file-manager-1/";
    private static final String URL_REPO_G_PLUS_COMMUNITY = "https://plus.google.com/communities/113997576965363268101";
    private static final String URL_REPO_XDA = "http://forum.xda-developers.com/android/apps-games/app-amaze-file-managermaterial-theme-t2937314";
    private static final String URL_REPO_RATE = "market://details?id=com.amaze.filemanager";
    private static final String TAG_CLIPBOARD_DONATE = "donate_id";
    private static final String URL_DONATE_2 = "https://www.paypal.me/vishalnehra";

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
        mVersion = (RelativeLayout) findViewById(R.id.relative_layout_version);
        mChangelog = (RelativeLayout) findViewById(R.id.relative_layout_changelog);
        mLicenses = (RelativeLayout) findViewById(R.id.relative_layout_licenses);
        mIssues = (RelativeLayout) findViewById(R.id.relative_layout_issues);
        mTranslate = (RelativeLayout) findViewById(R.id.relative_layout_translate);
        mAuthor1Gplus = (TextView) findViewById(R.id.text_view_author_1_g_plus);
        mAuthor1Donate = (TextView) findViewById(R.id.text_view_author_1_donate);
        mAuthor2Gplus = (TextView) findViewById(R.id.text_view_author_2_g_plus);
        mAuthor2Donate = (TextView) findViewById(R.id.text_view_author_2_donate);
        mAuthorsDivider = findViewById(R.id.view_divider_authors);
        mGplusCommunity = (RelativeLayout) findViewById(R.id.relative_layout_g_plus_community);
        mXda = (RelativeLayout) findViewById(R.id.relative_layout_xda);
        mRate = (RelativeLayout) findViewById(R.id.relative_layout_rate);
        mLicensesIcon = (ImageView) findViewById(R.id.image_view_license);

        mVersion.setOnClickListener(this);
        mChangelog.setOnClickListener(this);
        mLicenses.setOnClickListener(this);
        mAuthor1Gplus.setOnClickListener(this);
        mAuthor1Donate.setOnClickListener(this);
        mAuthor2Gplus.setOnClickListener(this);
        mAuthor2Donate.setOnClickListener(this);
        mIssues.setOnClickListener(this);
        mTranslate.setOnClickListener(this);
        mGplusCommunity.setOnClickListener(this);
        mXda.setOnClickListener(this);
        mRate.setOnClickListener(this);

        mAppBarLayout.setLayoutParams(calculateHeaderViewParams());

        mToolbar = (Toolbar)findViewById(R.id.toolBar);
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
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @SuppressWarnings("ResourceType")
            @Override
            public void onGenerated(Palette palette) {

                int mutedColor = palette.getMutedColor(getResources().getColor(R.color.primary_blue));
                int darkMutedColor = palette.getDarkMutedColor(getResources().getColor(R.color.primary_blue));
                mCollapsingToolbarLayout.setContentScrimColor(mutedColor);
                mCollapsingToolbarLayout.setStatusBarScrimColor(darkMutedColor);
            }
        });

        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

                mTitleTextView.setAlpha(Math.abs(verticalOffset / (float)
                        appBarLayout.getTotalScrollRange()));
            }
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
            mAuthorsDivider.setBackgroundColor(getResources().getColor(R.color.divider_dark_card));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.relative_layout_version:
                mCount++;
                if (mCount >= 5) {
                    if (mToast!=null)
                        mToast.cancel();
                    mToast = Toast.makeText(this, getResources().getString(R.string.easter_egg_title) +
                                    " : " + mCount, Toast.LENGTH_SHORT);
                    mToast.show();

                    mSharedPref.edit().putInt(KEY_PREF_STUDIO, Integer.parseInt(Integer.toString(mCount) + "000")).apply();
                } else {
                    mSharedPref.edit().putInt(KEY_PREF_STUDIO, 0).apply();
                }
                break;

            case R.id.relative_layout_issues:
                Intent issuesIntent = new Intent(Intent.ACTION_VIEW);
                issuesIntent.setData(Uri.parse(URL_REPO_ISSUES));
                startActivity(issuesIntent);
                break;

            case R.id.relative_layout_changelog:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(URL_REPO_CHANGELOG));
                startActivity(intent);
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
                Intent author1GplusIntent = new Intent(Intent.ACTION_VIEW);
                author1GplusIntent.setData(Uri.parse(URL_AUTHOR_1_G_PLUS));
                startActivity(author1GplusIntent);
                break;

            case R.id.text_view_author_1_donate:
                ClipboardManager clipManager1 = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip1 = ClipData.newPlainText(TAG_CLIPBOARD_DONATE, URL_AUTHOR_1_PAYPAL);
                clipManager1.setPrimaryClip(clip1);
                Toast.makeText(this, R.string.paypal_copy_message, Toast.LENGTH_LONG).show();
                break;

            case R.id.text_view_author_2_g_plus:
                Intent author2GplusIntent = new Intent(Intent.ACTION_VIEW);
                author2GplusIntent.setData(Uri.parse(URL_AUTHOR_2_G_PLUS));
                startActivity(author2GplusIntent);
                break;

            case R.id.text_view_author_2_donate:

                Intent donate2Intent = new Intent(Intent.ACTION_VIEW);
                donate2Intent.setData(Uri.parse(URL_DONATE_2));
                startActivity(donate2Intent);
                break;

            case R.id.relative_layout_translate:
                Intent translateIntent = new Intent(Intent.ACTION_VIEW);
                translateIntent.setData(Uri.parse(URL_REPO_TRANSLATE));
                startActivity(translateIntent);
                break;

            case R.id.relative_layout_g_plus_community:
                Intent communityIntent = new Intent(Intent.ACTION_VIEW);
                communityIntent.setData(Uri.parse(URL_REPO_G_PLUS_COMMUNITY));
                startActivity(communityIntent);
                break;

            case R.id.relative_layout_xda:
                Intent xdaIntent = new Intent(Intent.ACTION_VIEW);
                xdaIntent.setData(Uri.parse(URL_REPO_XDA));
                startActivity(xdaIntent);
                break;

            case R.id.relative_layout_rate:
                Intent rateIntent = new Intent(Intent.ACTION_VIEW);
                rateIntent.setData(Uri.parse(URL_REPO_RATE));
                startActivity(rateIntent);
                break;
        }
    }
}
