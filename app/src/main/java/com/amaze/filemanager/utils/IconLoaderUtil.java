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
import com.amaze.filemanager.utils.cloud.CloudUtil;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import jcifs.smb.SmbFile;

public class IconLoaderUtil {

    private static HandlerThread workerHandlerThread, iconLoaderHandlerThread;
    private static ConcurrentHashMap<ImageView, IconDataParcelable> requests = new ConcurrentHashMap<>();
    private Context context;
    private static IconLoaderUtil iconLoaderUtil;
    private RecyclerAdapter.OnImageProcessed onImageProcessed;
    private RequestListener<Drawable> requestListener;
    private RecyclerPreloadModelProvider recyclerPreloadModelProvider;

    private static final int SUCCESS = 1, DESTROY = 2, LOAD = 3;
    private static final String WORKER_THREAD_ICON = "icon_worker_thread";

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

    private Handler loaderHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SUCCESS:
                    publishResult((Result) msg.obj);
                    sendEmptyMessageDelayed(DESTROY, 1000);
                    break;
                case DESTROY:
                    cleanThreads();
                    break;
            }
        }

        private void publishResult(Result result) {
            // find the request in the queue
            for (Map.Entry<ImageView, IconDataParcelable> map : requests.entrySet()) {
                if (map.getValue().equals(result.iconDataParcelable)) {
                    map.getKey().setImageDrawable(result.drawable);
                    requests.remove(map);
                    break;
                }
            }
        }
    };

    private Handler workerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            IconDataParcelable iconDataParcelable = (IconDataParcelable) msg.obj;
            Drawable drawable = load(iconDataParcelable);

            Result result = new Result();
            result.drawable = drawable;
            result.iconDataParcelable = iconDataParcelable;
            if (drawable == null) {
                loaderHandler.obtainMessage(DESTROY).sendToTarget();
            } else {
                loaderHandler.obtainMessage(SUCCESS, result).sendToTarget();
            }
        }
    };

    public void loadDrawable(ImageView imageView, IconDataParcelable iconDataParcelable,
                             RecyclerAdapter.OnImageProcessed onImageProcessed,
                             RequestListener<Drawable> requestListener) {
        this.requestListener = requestListener;
        this.onImageProcessed = onImageProcessed;

        new Thread(new Runnable() {
            @Override
            public void run() {

                if (workerHandler == null || workerHandlerThread == null) {
                    workerHandlerThread = new HandlerThread(WORKER_THREAD_ICON);
                    workerHandlerThread.start();
                    workerHandler = new Handler(workerHandlerThread.getLooper());
                }
                requests.put(imageView, iconDataParcelable);
                workerHandler.obtainMessage(LOAD, iconDataParcelable).sendToTarget();
            }
        }).start();
    }

    public void loadDrawable(IconDataParcelable iconDataParcelable, ImageView imageView) {


        if (workerHandler == null || workerHandlerThread == null) {
            workerHandlerThread = new HandlerThread(WORKER_THREAD_ICON);
            workerHandlerThread.start();
            workerHandler = new Handler(workerHandlerThread.getLooper());
        }
        requests.put(imageView, iconDataParcelable);
    }

    private Drawable load(IconDataParcelable iconDataParcelable) {
        FutureTarget<Drawable> drawableFutureTarget;
        if (requestListener != null && onImageProcessed != null) {
            drawableFutureTarget = recyclerPreloadModelProvider
                    .getPreloadRequestBuilder(iconDataParcelable)
                    .listener(requestListener).submit();
        } else {
            drawableFutureTarget = recyclerPreloadModelProvider.getPreloadRequestBuilder(iconDataParcelable).submit();
        }
        try {
            Drawable drawable = drawableFutureTarget.get();
            return drawable;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void cleanThreads() {
        if (workerHandlerThread != null) {

            workerHandlerThread.getLooper().quit();
            workerHandlerThread = null;
            workerHandler = null;
        }
    }

    private static class Result {
        Drawable drawable;
        IconDataParcelable iconDataParcelable;
    }

    public static InputStream getInputStreamForCloud(Context context, IconDataParcelable iconDataParcelable) {
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
                inputStream = cloudStorageDropbox.download(CloudUtil.stripPath(OpenMode.DROPBOX, hybridFile.getPath()));
                break;
            case BOX:
                CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
                inputStream = cloudStorageBox.download(CloudUtil.stripPath(OpenMode.BOX, hybridFile.getPath()));
                break;
            case GDRIVE:
                CloudStorage cloudStorageGDrive = dataUtils.getAccount(OpenMode.GDRIVE);
                inputStream = cloudStorageGDrive.download(CloudUtil.stripPath(OpenMode.GDRIVE, hybridFile.getPath()));
                break;
            case ONEDRIVE:
                CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
                inputStream = cloudStorageOneDrive.download(CloudUtil.stripPath(OpenMode.ONEDRIVE, hybridFile.getPath()));
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
