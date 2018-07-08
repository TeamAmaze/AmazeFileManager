package com.amaze.filemanager.asynchronous.handlers;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.amaze.filemanager.adapters.RecyclerAdapter;
import com.amaze.filemanager.filesystem.CustomFileObserver;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.utils.provider.UtilitiesProvider;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * @author Emmanuel
 *         on 8/11/2017, at 17:37.
 */
public class FileHandler extends Handler {
    private WeakReference<MainFragment> mainFragment;
    private RecyclerView listView;
    private boolean useThumbs;

    public FileHandler(MainFragment mainFragment, RecyclerView listView, boolean useThumbs) {
        super(Looper.getMainLooper());
        this.mainFragment = new WeakReference<>(mainFragment);
        this.listView = listView;
        this.useThumbs = useThumbs;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        final MainFragment main = mainFragment.get();

        if(main == null || main.getActivity() == null) {
            return;
        }

        String path = (String) msg.obj;

        switch (msg.what) {
            case CustomFileObserver.GOBACK:
                main.goBack();
                break;
            case CustomFileObserver.NEW_ITEM:
                HybridFile fileCreated = new HybridFile(main.openMode,
                        main.getCurrentPath() + "/" + path);
                main.getElementsList().add(fileCreated.generateLayoutElement(useThumbs));
                break;
            case CustomFileObserver.DELETED_ITEM:
                for (int i = 0; i < main.getElementsList().size(); i++) {
                    File currentFile = new File(main.getElementsList().get(i).desc);

                    if (currentFile.getName().equals(path)) {
                        main.getElementsList().remove(i);
                        break;
                    }
                }
                break;
            default://Pass along other messages from the UI
                super.handleMessage(msg);
                return;
        }

        if (listView.getVisibility() == View.VISIBLE) {
            if (main.getElementsList().size() == 0) {
                // no item left in list, recreate views
                main.reloadListElements(true, main.results, !main.IS_LIST);
            } else {
                // we already have some elements in list view, invalidate the adapter
                ((RecyclerAdapter) listView.getAdapter()).setItems(listView, main.getElementsList());
            }
        } else {
            // there was no list view, means the directory was empty
            main.loadlist(main.getCurrentPath(), true, main.openMode);
        }

        main.computeScroll();
    }
}
