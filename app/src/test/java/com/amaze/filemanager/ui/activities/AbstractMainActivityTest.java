package com.amaze.filemanager.ui.activities;

import android.os.Build;
import android.os.storage.StorageManager;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amaze.filemanager.shadows.ShadowMultiDex;
import com.amaze.filemanager.test.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowStorageManager;

import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import static android.os.Build.VERSION_CODES.N;
import static androidx.test.core.app.ActivityScenario.launch;
import static org.robolectric.Shadows.shadowOf;

/*
 * Need to make LooperMode PAUSED and flush the main looper before activity can show up.
 * @see {@link LooperMode.Mode.PAUSED}
 * @see {@link <a href="https://stackoverflow.com/questions/55679636/robolectric-throws-fragmentmanager-is-already-executing-transactions">StackOverflow discussion</a>}
 */
@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(AndroidJUnit4.class)
@Config(
        shadows = {
                ShadowMultiDex.class,
                ShadowStorageManager.class})
public abstract class AbstractMainActivityTest {

    protected ActivityScenario<MainActivity> scenario;

    @BeforeClass
    public static void setUpBeforeClass() {
        RxJavaPlugins.reset();
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        if (Build.VERSION.SDK_INT >= N) TestUtils.initializeInternalStorage();

        ShadowLooper.idleMainLooper();
        scenario = launch(MainActivity.class);
        scenario.moveToState(Lifecycle.State.STARTED);
    }

    @After
    public void tearDown() {

        scenario.moveToState(Lifecycle.State.DESTROYED);
        scenario.close();

        if (Build.VERSION.SDK_INT >= N)
            shadowOf(ApplicationProvider.getApplicationContext().getSystemService(StorageManager.class))
                    .resetStorageVolumeList();
    }
}
