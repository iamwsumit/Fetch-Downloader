package com.sumit1334.fetchdownloader;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.PermissionResultHandler;

import com.tonyodev.fetch2.AbstractFetchListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2core.DownloadBlock;
import com.tonyodev.fetch2core.Func;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

public class FetchDownloader extends AndroidNonvisibleComponent implements Component, OnDestroyListener {
    private final Context context;
    private final HashMap<String, Download> downloads = new HashMap<>();
    private final HashMap<Download, String> ids = new HashMap<>();
    private final String TAG = "Fetch";
    private Fetch fetch;
    private NetworkType networkType = NetworkType.ALL;

    private String id = "";
    private int lastId = 0;

    private final AbstractFetchListener listener = new AbstractFetchListener() {
        @Override
        public void onCancelled(@NotNull Download download) {
            if (lastId == download.getId()) {
                lastId = 0;
                return;
            }
            lastId = download.getId();
            downloads.remove(ids.get(download));
            ids.remove(download);
            DownloadCancelled(ids.get(download));
        }

        @Override
        public void onCompleted(@NotNull Download download) {
            if (lastId == download.getId()) {
                lastId = 0;
                return;
            }
            lastId = download.getId();
            DownloadCompleted(ids.get(download));
            downloads.remove(ids.get(download));
            ids.remove(download);
        }

        @Override
        public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
            if (lastId == download.getId()) {
                lastId = 0;
                return;
            }
            lastId = download.getId();
            downloads.remove(ids.get(download));
            ids.remove(download);
            ErrorOccurred(ids.get(download), error.toString());
        }

        @Override
        public void onPaused(@NotNull Download download) {
            if (lastId == download.getId()) {
                lastId = 0;
                return;
            }
            lastId = download.getId();
            DownloadPaused(ids.get(download));
        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
            OnProgress(ids.get(download), download.getProgress(), etaInMilliSeconds, downloadedBytesPerSecond);
        }

        @Override
        public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
            downloads.put(id, download);
            ids.put(download, id);
            if (lastId == download.getId()) {
                lastId = 0;
                return;
            }
            lastId = download.getId();
            DownloadQueued(ids.get(download));
        }

        @Override
        public void onResumed(@NotNull Download download) {
            if (lastId == download.getId()) {
                lastId = 0;
                return;
            }
            lastId = download.getId();
            DownloadResumed(ids.get(download));
        }

        @Override
        public void onStarted(@NotNull Download download, @NotNull List<? extends DownloadBlock> downloadBlocks, int totalBlocks) {
            if (lastId == download.getId()) {
                lastId = 0;
                return;
            }
            lastId = download.getId();
            DownloadStarted(ids.get(download));
        }
    };

    public FetchDownloader(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
        container.$form().registerForOnDestroy(this);
        Log.i(TAG, "Extension Initialized");
    }

    @SimpleEvent
    public void ErrorOccurred(String id, String error) {
        EventDispatcher.dispatchEvent(this, "ErrorOccurred", id, error);
    }

    @SimpleEvent
    public void DownloadQueued(String id) {
        EventDispatcher.dispatchEvent(this, "DownloadQueued", id);
    }

    @SimpleEvent
    public void DownloadStarted(String id) {
        EventDispatcher.dispatchEvent(this, "DownloadStarted", id);
    }

    @SimpleEvent
    public void DownloadPaused(String id) {
        EventDispatcher.dispatchEvent(this, "DownloadPaused", id);
    }

    @SimpleEvent
    public void DownloadResumed(String id) {
        EventDispatcher.dispatchEvent(this, "DownloadResumed", id);
    }

    @SimpleEvent
    public void DownloadCancelled(String id) {
        EventDispatcher.dispatchEvent(this, "DownloadCancelled", id);
    }

    @SimpleEvent
    public void OnProgress(String id, int progress, long eta, long speed) {
        EventDispatcher.dispatchEvent(this, "OnProgress", id, progress == -1 ? 0 : progress, eta == -1 ? 0 : eta, speed);
    }

    @SimpleEvent
    public void DownloadCompleted(String id) {
        EventDispatcher.dispatchEvent(this, "DownloadCompleted", id);
    }

    @SimpleFunction(description = "Initialize the downloader. \n You must use this block first to initialize the downloader than any other block")
    public void Initialize(int downloadConcurrentLimit) {
        try {
            FetchConfiguration.Builder configuration = new FetchConfiguration.Builder(context)
                    .setDownloadConcurrentLimit(downloadConcurrentLimit);
            this.fetch = Fetch.Impl.getInstance(configuration.build());
            this.fetch.addListener(listener);
            Log.i(TAG, "Initialize: Downloader Initialized");
        } catch (Exception e) {
            ErrorOccurred("Initialize", e.getMessage());
        }
    }

    @SimpleFunction(description = "Ask the user WRITE_EXTERNAL_STORAGE permission")
    public void AskPermission() {
        this.form.askPermission("android.permission.WRITE_EXTERNAL_STORAGE", new PermissionResultHandler() {
            @Override
            public void HandlePermissionResponse(String permission, boolean granted) {
                Log.i(TAG, "Permission granted : " + granted);
            }
        });
    }

    @SimpleFunction(description = "Returns true if we have write external storage permission granted")
    public boolean HavePermission() {
        PackageManager pm = context.getPackageManager();
        int permission = pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, context.getPackageName());
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    @SimpleProperty(description = "Specifies the network type that you want to run your downloads on")
    public void NetworkType(int type) {
        this.networkType = getNetworkType(type);
    }

    @SimpleFunction
    public void DownloadFile(String id, String path, String url) {
        try {
            Request request = new Request(url, path);
            request.setAutoRetryMaxAttempts(0);
            this.id = id;
            request.setNetworkType(networkType);
            if (fetch != null) {
                fetch.enqueue(request, new Func<Request>() {
                    @Override
                    public void call(@NotNull Request request) {
                    }
                }, new Func<Error>() {
                    @Override
                    public void call(@NotNull Error error) {
                        ErrorOccurred(id, error.toString());
                    }
                });
            } else
                throw new NullPointerException("Please initialize the downloader first");
        } catch (Exception e) {
            ErrorOccurred("Download File", e.getMessage());
        }
    }

    @SimpleFunction
    public void Pause(String id) {
        if (this.downloads.containsKey(id))
            fetch.pause(downloads.get(id).getId());
        else
            throw new IllegalStateException("Id does not exist");
    }

    @SimpleFunction
    public void Resume(String id) {
        if (this.downloads.containsKey(id))
            fetch.resume(downloads.get(id).getId());
        else
            throw new IllegalStateException("Id does not exist");
    }

    @SimpleFunction
    public void Cancel(String id) {
        if (this.downloads.containsKey(id)) {
            fetch.delete(downloads.get(id).getId());
            ids.remove(downloads.get(id));
            downloads.remove(id);
        } else
            throw new IllegalStateException("Id does not exist");
    }

    @SimpleFunction(description = "Returns the total file size that will be downloaded, in bytes")
    public long GetFileSize(String id) {
        if (this.downloads.containsKey(id)) {
            long total = downloads.get(id).getTotal();
            return total == -1 ? 0 : total;
        }
        throw new IllegalStateException("Id does not exist");
    }

    @SimpleFunction(description = "Returns the progress of the download between 0 and 100")
    public long GetProgress(String id) {
        if (this.downloads.containsKey(id)) {
            int progress = downloads.get(id).getProgress();
            return progress == -1 ? 0 : progress;
        }
        throw new IllegalStateException("Id does not exist");
    }

    @SimpleFunction(description = "Returns the downloaded size of the file in bytes")
    public long GetDownloadedSize(String id) {
        if (this.downloads.containsKey(id))
            return downloads.get(id).getDownloaded();
        throw new IllegalStateException("Id does not exist");
    }

    @SimpleFunction
    public String GetASDPath() {
        return context.getExternalFilesDir(null).getPath();
    }

    @SimpleFunction(description = "This block cancel all the downloads")
    public void CancelAllDownloads() {
        for (Download download : ids.keySet()) {
            fetch.delete(download.getId());
        }
        this.fetch.removeAll();
    }

    @SimpleFunction(description = "This block delete all the downloaded files from cache or from the file manager")
    public void DeleteAllDownloads() {
        this.downloads.clear();
        this.ids.clear();
        this.fetch.deleteAll();
    }

    @SimpleFunction(description = "Converts the given bytes into MBs/KBs and GBs.\n This block converts the bytes to suitable format")
    public String ConvertBytes(long bytes) {
        double kb = (double) bytes / (double) 1000;
        double mb = kb / (double) 1000;
        final DecimalFormat decimalFormat = new DecimalFormat(".##");
        if (mb >= 1) {
            return decimalFormat.format(mb) + " MB";
        } else if (kb >= 1) {
            return decimalFormat.format(kb) + " KB";
        } else {
            return bytes + "";
        }
    }

    private NetworkType getNetworkType(int i) {
        switch (i) {
            case 0:
                return NetworkType.WIFI_ONLY;
            case 1:
                return NetworkType.ALL;
            case 2:
                return NetworkType.UNMETERED;
            case 3:
                return NetworkType.GLOBAL_OFF;
        }
        return NetworkType.ALL;
    }

    @SimpleProperty
    public int WifiOnly() {
        return 0;
    }

    @SimpleProperty
    public int All() {
        return 1;
    }

    @SimpleProperty
    public int UnMetered() {
        return 2;
    }

    @SimpleProperty
    public int GlobalOff() {
        return 3;
    }

    @Override
    public void onDestroy() {
        if (fetch != null) {
            for (Download download : ids.keySet()) {
                if (download.getStatus() != Status.COMPLETED)
                    fetch.delete(download.getId());
            }
            downloads.clear();
            ids.clear();
            fetch.close();
        }
    }
}