/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
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

package com.amaze.filemanager.ui.fragments
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build.VERSION_CODES.M
import android.provider.MediaStore
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.R
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.shadows.jcifs.smb.ShadowSmbFile
import com.amaze.filemanager.test.ShadowPasswordUtil
import com.amaze.filemanager.test.ShadowTabHandler
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.fragments.MainFragmentReturnIntentResultsTest.Companion.TEST_AUDIO_PATH
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.fakes.RoboCursor
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSQLiteConnection
import org.robolectric.shadows.ShadowStorageManager
import org.robolectric.shadows.ShadowToast

/**
 * Robolectric tests for the ringtone-picker path in [MainFragment.returnIntentResults].
 *
 * Changes covered (against `upstream/release/4.0`):
 * - When `MainActivity.mRingtonePickerIntent == true` and the selected file IS indexed in
 *   MediaStore, the activity result must be RESULT_OK with an intent whose data URI comes from
 *   MediaStore and carries `canonical=1` and `title=<stem>` query parameters.
 * - When `mRingtonePickerIntent == true` but the file is NOT found in MediaStore, the activity
 *   must show an error [android.widget.Toast] and finish with RESULT_CANCELED.
 *
 * Tests run only on [M] (API 23) so that `Utils.getUriForBaseFile` returns a plain
 * `file://` URI via `Uri.fromFile()` without requiring a configured
 * [androidx.core.content.FileProvider].
 *
 * Because Robolectric 4.9's in-process MediaStore ContentProvider does not support the
 * INSERT → `_data=?` QUERY cycle, the MediaStore query response is simulated by registering a
 * [RoboCursor] on the activity's [android.content.ContentResolver] shadow via
 * [org.robolectric.shadows.ShadowContentResolver.setCursor].
 */
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Suppress("StringLiteralDuplication")
@Config(
    // API 23 (pre-N): Uri.fromFile() path avoids FileProvider setup complexity.
    sdk = [M],
    shadows = [
        ShadowMultiDex::class,
        ShadowStorageManager::class,
        ShadowPasswordUtil::class,
        ShadowSmbFile::class,
        ShadowTabHandler::class,
    ],
)
class MainFragmentReturnIntentResultsTest {
    companion object {
        private const val TEST_AUDIO_PATH = "/storage/emulated/0/Music/ringtone_test.mp3"
        private const val TEST_AUDIO_FILENAME = "ringtone_test.mp3"

        // Expected title = filename stem (everything before the last '.').
        private const val TEST_AUDIO_STEM = "ringtone_test"

        // Fake MediaStore row id planted into the RoboCursor.
        private const val FAKE_MEDIA_ID = 7
    }

    private lateinit var scenario: ActivityScenario<MainActivity>

    /**
     * Setup before tests.
     */
    @Before
    fun setUp() {
        RxJavaPlugins.reset()
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.reset()
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }
        ShadowSQLiteConnection.reset()
    }

    /**
     * After test clean up.
     */
    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
        ShadowSQLiteConnection.reset()
        RxAndroidPlugins.reset()
        RxJavaPlugins.reset()
    }

    /** Builds an [Intent] that starts [MainActivity] as a ringtone picker. */
    private fun ringtonePickerIntent(context: android.content.Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = RingtoneManager.ACTION_RINGTONE_PICKER
        }

    /**
     * Registers a single-row [RoboCursor] against [MediaStore.Audio.Media.EXTERNAL_CONTENT_URI]
     * on the given [MainActivity]'s ContentResolver shadow so that
     * [com.amaze.filemanager.filesystem.MediaStoreHack.getUriForMusicMediaFrom] returns a
     * non-null URI when queried for [TEST_AUDIO_PATH].
     *
     * This works around Robolectric 4.9's lack of MediaStore INSERT→QUERY support.
     */
    private fun registerFakeMediaStoreCursor(activity: MainActivity) {
        val cursor =
            RoboCursor().apply {
                setColumnNames(listOf(MediaStore.Audio.Media._ID))
                setResults(arrayOf<Array<Any>>(arrayOf(FAKE_MEDIA_ID)))
            }
        shadowOf(activity.contentResolver)
            .setCursor(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor)
    }

    /** Extracts the first [MainFragment] from the currently active [TabFragment]. */
    private fun MainActivity.firstMainFragment(): MainFragment? = getTabFragment()?.getFragmentAtIndex(0) as? MainFragment

    /** Creates a minimal [HybridFileParcelable] for [TEST_AUDIO_PATH]. */
    private fun audioFileParcelable(): HybridFileParcelable = HybridFileParcelable(TEST_AUDIO_PATH).also { it.name = TEST_AUDIO_FILENAME }

    /**
     * Happy path: the selected audio file IS indexed in MediaStore.
     *
     * Expected behaviour:
     * - `Activity.setResult(RESULT_OK, intent)` is called.
     * - `intent.data` is a `content://` URI under [MediaStore.Audio.Media.EXTERNAL_CONTENT_URI].
     * - The URI carries `canonical=1` and `title=<stem>` query parameters.
     * - [RingtoneManager.EXTRA_RINGTONE_PICKED_URI] extra equals `intent.data`.
     */
    @Test
    fun testReturnIntentResultsForRingtonePickerWhenFileFoundInMediaStore() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        scenario = ActivityScenario.launch(ringtonePickerIntent(ctx))
        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            assertTrue(
                "mRingtonePickerIntent must be true after launching with ACTION_RINGTONE_PICKER",
                activity.mRingtonePickerIntent,
            )
            // Simulate MediaStore having the file indexed via a fake cursor.
            registerFakeMediaStoreCursor(activity)
            val mainFragment = activity.firstMainFragment()
            assertNotNull("MainFragment must be attached to the activity", mainFragment)
            mainFragment!!.returnIntentResults(arrayOf(audioFileParcelable()))
            ShadowLooper.idleMainLooper()
            val shadow = shadowOf(activity)
            assertEquals(
                "Activity result code must be RESULT_OK",
                Activity.RESULT_OK,
                shadow.resultCode,
            )
            val resultIntent = shadow.resultIntent
            assertNotNull("Result intent must not be null", resultIntent)
            val resultData: Uri? = resultIntent.data
            assertNotNull("Result intent data URI must not be null", resultData)
            // URI must be a MediaStore content:// URI.
            assertEquals("Result URI scheme must be 'content'", "content", resultData!!.scheme)
            assertTrue(
                "Result URI must be under MediaStore.Audio.Media.EXTERNAL_CONTENT_URI",
                resultData.toString()
                    .startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString()),
            )
            // The fake cursor id must appear as the base path segment (before query params).
            val baseUri = resultData.buildUpon().clearQuery().build()
            assertEquals(
                "Base URI last path segment must match the fake media id",
                FAKE_MEDIA_ID.toString(),
                baseUri.lastPathSegment,
            )
            // Query parameters appended in returnIntentResults.
            assertEquals(
                "canonical query param must be '1'",
                "1",
                resultData.getQueryParameter("canonical"),
            )
            assertEquals(
                "title query param must equal the filename stem",
                TEST_AUDIO_STEM,
                resultData.getQueryParameter("title"),
            )
            // EXTRA_RINGTONE_PICKED_URI must equal the data URI.
            @Suppress("DEPRECATION")
            val pickedUri: Uri? =
                resultIntent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            assertEquals(
                "EXTRA_RINGTONE_PICKED_URI must equal intent data",
                resultData,
                pickedUri,
            )
        }
    }

    /**
     * Error path: the selected audio file is NOT indexed in MediaStore.
     *
     * Expected behaviour:
     * - A [android.widget.Toast] with text [R.string.error_mediastore_query_uri] is shown.
     * - `Activity.setResult(RESULT_CANCELED)` is called.
     */
    @Test
    fun testReturnIntentResultsForRingtonePickerWhenFileNotFoundInMediaStore() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        scenario = ActivityScenario.launch(ringtonePickerIntent(ctx))
        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            assertTrue(
                "mRingtonePickerIntent must be true after launching with ACTION_RINGTONE_PICKER",
                activity.mRingtonePickerIntent,
            )
            // Intentionally do NOT register any cursor →
            // ShadowContentResolver returns null for the MediaStore query.
            val mainFragment = activity.firstMainFragment()
            assertNotNull("MainFragment must be attached to the activity", mainFragment)
            mainFragment!!.returnIntentResults(arrayOf(audioFileParcelable()))
            ShadowLooper.idleMainLooper()
            // Error toast must be displayed.
            val latestToast = ShadowToast.getLatestToast()
            assertNotNull(
                "An error Toast should have been shown when MediaStore lookup fails",
                latestToast,
            )
            assertEquals(
                "Toast must show the MediaStore error string",
                ctx.getString(R.string.error_mediastore_query_uri),
                ShadowToast.getTextOfLatestToast(),
            )
            // Activity must finish with RESULT_CANCELED.
            val shadow = shadowOf(activity)
            assertEquals(
                "Activity result code must be RESULT_CANCELED",
                Activity.RESULT_CANCELED,
                shadow.resultCode,
            )
        }
    }

    /**
     * Sanity check: when `mRingtonePickerIntent == false` (normal file-pickup mode),
     * [MainFragment.returnIntentResults] must NOT query MediaStore and must set a plain
     * `file://` data URI on the result intent with RESULT_OK.
     *
     * This is a regression guard for the unchanged `else`-branch in the original code.
     */
    @Test
    fun testReturnIntentResultsForNonRingtonePickerUsesFileUri() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Launch without ACTION_RINGTONE_PICKER — mRingtonePickerIntent stays false.
        val plainPickerIntent =
            Intent(ctx, MainActivity::class.java).apply {
                action = Intent.ACTION_GET_CONTENT
            }
        scenario = ActivityScenario.launch(plainPickerIntent)
        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            // Set mReturnIntent manually (normally set by the GET_CONTENT handler).
            activity.mReturnIntent = true
            val mainFragment = activity.firstMainFragment()
            assertNotNull("MainFragment must be attached to the activity", mainFragment)
            mainFragment!!.returnIntentResults(arrayOf(audioFileParcelable()))
            ShadowLooper.idleMainLooper()
            val shadow = shadowOf(activity)
            assertEquals(
                "Activity result code must be RESULT_OK for normal pick",
                Activity.RESULT_OK,
                shadow.resultCode,
            )
            val resultData: Uri? = shadow.resultIntent?.data
            assertNotNull("Result URI must not be null for normal pick", resultData)
            // On API 23, Utils.getUriForBaseFile returns a file:// URI.
            assertEquals(
                "Normal pick result must use file:// scheme on API 23",
                "file",
                resultData!!.scheme,
            )
        }
    }
}
