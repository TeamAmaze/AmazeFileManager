package com.amaze.filemanager.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.widget.ImageView;

import com.amaze.filemanager.adapters.RecyclerAdapter;
import com.amaze.filemanager.adapters.data.IconDataParcelable;
import com.amaze.filemanager.adapters.glide.RecyclerPreloadModelProvider;
import com.amaze.filemanager.adapters.holders.ItemViewHolder;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.ssh.SFtpClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.ui.views.RoundedImageView;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.cloudrail.si.interfaces.CloudStorage;

import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.SFTPClient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import jcifs.smb.SmbFile;

public class IconLoaderUtil {

    private static HandlerThread workerHandlerThread, loaderHandlerThread;
    private static ConcurrentHashMap<IconLoader, FutureTarget<Drawable>> requests = new ConcurrentHashMap<>();
    private Context context;
    private static IconLoaderUtil iconLoaderUtil;
    private RecyclerAdapter.OnImageProcessed onImageProcessed;
    private RequestListener<Drawable> requestListener;
    private RecyclerPreloadModelProvider recyclerPreloadModelProvider;
    private Handler workerHandler, loaderHandler;
    private boolean isLoadCancelled = false;

    private static final int SUCCESS = 1, DESTROY = 2, LOAD = 3;
    private static final String WORKER_THREAD_ICON = "icon_thread_worker";
    private static final String LOADER_THREAD_ICON = "icon_thread_loader";

    private IconLoaderUtil(Context context, RecyclerPreloadModelProvider recyclerPreloadModelProvider) {
        this.context = context;
        this.recyclerPreloadModelProvider = recyclerPreloadModelProvider;
    }

    public static IconLoaderUtil getInstance(Context context, RecyclerPreloadModelProvider recyclerPreloadModelProvider) {
        if (iconLoaderUtil != null)
            return iconLoaderUtil;
        iconLoaderUtil = new IconLoaderUtil(context, recyclerPreloadModelProvider);
        return iconLoaderUtil;
    }

    private Handler.Callback loaderHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            publishResult((Result) msg.obj, msg.what);
            return false;
        }

        private void publishResult(Result result, int what) {
            synchronized (requests) {
                // find the request in the queue
                for (Map.Entry<IconLoader, FutureTarget<Drawable>> map : requests.entrySet()) {
                    if (map.getKey().iconDataParcelable.equals(result.iconDataParcelable)) {
                        if (what == SUCCESS) {
                            map.getKey().iconView.setImageDrawable(result.drawable);
                        } else if (what == DESTROY) {
                            map.getKey().iconView.setImageDrawable(null);
                        }
                        requests.remove(map);
                        break;
                    }
                }
            }
        }
    };

    private Handler.Callback workerHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            IconLoader iconLoader = (IconLoader) msg.obj;
            Drawable drawable = load(iconLoader);

            Result result = new Result();
            result.drawable = drawable;
            result.iconDataParcelable = iconLoader.iconDataParcelable;
            if (drawable == null) {
                loaderHandler.obtainMessage(DESTROY, result).sendToTarget();
            } else {
                loaderHandler.obtainMessage(SUCCESS, result).sendToTarget();
            }
            return false;
        }
    };

    public void loadDrawable(IconLoader iconLoader, RecyclerAdapter.OnImageProcessed onImageProcessed,
                             RequestListener<Drawable> requestListener) {
        this.requestListener = requestListener;
        this.onImageProcessed = onImageProcessed;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (workerHandler == null || workerHandlerThread == null) {
                    workerHandlerThread = new HandlerThread(WORKER_THREAD_ICON);
                    workerHandlerThread.start();
                    workerHandler = new Handler(workerHandlerThread.getLooper(), workerHandlerCallback);
                }
                workerHandler.obtainMessage(LOAD, iconLoader).sendToTarget();
            }
        }).start();

        if (loaderHandler == null || loaderHandlerThread == null) {
            loaderHandlerThread = new HandlerThread(LOADER_THREAD_ICON);
            loaderHandlerThread.start();
            loaderHandler = new Handler(context.getMainLooper(), loaderHandlerCallback);
        }
    }

    private Drawable load(IconLoader iconLoader) {
        FutureTarget<Drawable> drawableFutureTarget;
        if (requestListener != null && onImageProcessed != null) {
            drawableFutureTarget = recyclerPreloadModelProvider
                    .getPreloadRequestBuilder(iconLoader.iconDataParcelable)
                    .listener(requestListener).submit();
        } else {
            drawableFutureTarget = recyclerPreloadModelProvider.getPreloadRequestBuilder(iconLoader.iconDataParcelable).submit();
        }
        requests.put(iconLoader, drawableFutureTarget);
        try {/*
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true) {
                        if (isLoadCancelled) {
                            drawableFutureTarget.cancel(true);
                            break;
                        }
                    }
                }
            }).start();*/
            Drawable drawable = drawableFutureTarget.get();
            return drawable;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        } catch (CancellationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void cleanThreads() {
        if (workerHandlerThread != null) {
            requests.clear();
            requests = null;
            workerHandlerThread.getLooper().quit();
            workerHandlerThread = null;
            workerHandler = null;
        }
    }

    public void pauseLoad(ImageView imageView) {
        isLoadCancelled = true;
        synchronized (requests) {
            // clear all bitmaps
            for (Map.Entry<IconLoader, FutureTarget<Drawable>> value : requests.entrySet()) {
                ImageView imageViewTemp = value.getKey().iconView;
                if (imageViewTemp.equals(imageView)) {
                    imageViewTemp.setImageDrawable(null);
                    Glide.with(context).clear(imageView);
                    FutureTarget<Drawable> futureTarget = value.getValue();
                    if (futureTarget.getRequest() != null && !futureTarget.getRequest().isCancelled()) {
                        futureTarget.getRequest().pause();
                    }
                    //futureTarget.cancel(true);
                    //requests.remove(value);
                    break;
                }
            }
        }
    }

    public void resumesLoad(ImageView imageView) {
        isLoadCancelled = true;
        synchronized (requests) {
            // clear all bitmaps
            for (Map.Entry<IconLoader, FutureTarget<Drawable>> value : requests.entrySet()) {
                ImageView imageViewTemp = value.getKey().iconView;
                if (imageViewTemp.equals(imageView)) {
                    imageViewTemp.setImageDrawable(null);
                    Glide.with(context).clear(imageView);
                    FutureTarget<Drawable> futureTarget = value.getValue();
                    if (futureTarget.getRequest() != null && !futureTarget.getRequest().isCancelled()
                            && futureTarget.getRequest().isPaused()) {
                        futureTarget.getRequest().begin();
                    }
                    //futureTarget.cancel(true);
                    //requests.remove(value);
                    break;
                }
            }
        }
    }

    private static class Result {
        Drawable drawable;
        IconDataParcelable iconDataParcelable;
    }

    public static class IconLoader {
        public ImageView iconView;
        public IconDataParcelable iconDataParcelable;

        public IconLoader(ImageView imageView, IconDataParcelable iconDataParcelable) {
            iconView = imageView;
            this.iconDataParcelable = iconDataParcelable;
        }
    }

    public static InputStream getThumbnailInputStreamForCloud(Context context, IconDataParcelable iconDataParcelable) {
        InputStream inputStream;
        HybridFile hybridFile = new HybridFile(OpenMode.UNKNOWN, iconDataParcelable.path);
        hybridFile.generateMode(context);
        DataUtils dataUtils = DataUtils.getInstance();

        switch (hybridFile.getMode()) {
            case SFTP:
                inputStream = SshClientUtils.execute(new SFtpClientTemplate(hybridFile.getPath(), false) {
                    @Override
                    public InputStream execute(final SFTPClient client) throws IOException {
                        final RemoteFile rf = client.open(SshClientUtils.extractRemotePathFrom(hybridFile.getPath()));
                        return rf. new RemoteFileInputStream(){
                            @Override
                            public void close() throws IOException {
                                try
                                {
                                    super.close();
                                }
                                finally
                                {
                                    rf.close();
                                    client.close();
                                }
                            }
                        };
                    }
                });
                break;
            case SMB:
                try {
                    inputStream = new SmbFile(hybridFile.getPath()).getInputStream();
                } catch (IOException e) {
                    inputStream = null;
                    e.printStackTrace();
                }
                break;
            case OTG:
                ContentResolver contentResolver = context.getContentResolver();
                DocumentFile documentSourceFile = OTGUtil.getDocumentFile(hybridFile.getPath(),
                        context, false);
                try {
                    inputStream = contentResolver.openInputStream(documentSourceFile.getUri());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    inputStream = null;
                }
                break;
            case DROPBOX:
                CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
                inputStream = cloudStorageDropbox.getThumbnail(CloudUtil.stripPath(OpenMode.DROPBOX, hybridFile.getPath()));
                break;
            case BOX:
                CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
                inputStream = cloudStorageBox.getThumbnail(CloudUtil.stripPath(OpenMode.BOX, hybridFile.getPath()));
                break;
            case GDRIVE:
                CloudStorage cloudStorageGDrive = dataUtils.getAccount(OpenMode.GDRIVE);
                inputStream = cloudStorageGDrive.getThumbnail(CloudUtil.stripPath(OpenMode.GDRIVE, hybridFile.getPath()));
                break;
            case ONEDRIVE:
                CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
                inputStream = cloudStorageOneDrive.getThumbnail(CloudUtil.stripPath(OpenMode.ONEDRIVE, hybridFile.getPath()));
                break;
            default:
                try {
                    inputStream = new FileInputStream(hybridFile.getPath());
                } catch (FileNotFoundException e) {
                    inputStream = null;
                    e.printStackTrace();
                }
                break;
        }
        return inputStream;
    }
}
