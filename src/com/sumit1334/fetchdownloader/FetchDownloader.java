package com.sumit1334.fetchdownloader;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.PropertyTypeConstants;
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class FetchDownloader extends AndroidNonvisibleComponent implements Component, OnDestroyListener {
    private final Context context;
    private final String TAG = "Fetch";
    private final HashMap<String, Integer> intIds = new HashMap<>();
    private final HashMap<Integer, String> stringIds = new HashMap<>();
    private Fetch fetch;
    private NetworkType networkType = NetworkType.ALL;
    private boolean convertBytes = false;

    private final AbstractFetchListener listener = new AbstractFetchListener() {
        @Override
        public void onCancelled(@NotNull Download download) {
            String id = stringIds.get(download.getId());
            stringIds.remove(download.getId());
            intIds.remove(id);
            DownloadCancelled(id);
        }

        @Override
        public void onCompleted(@NotNull Download download) {
            String id = stringIds.get(download.getId());
            stringIds.remove(download.getId());
            intIds.remove(id);
            DownloadCompleted(id);
        }

        @Override
        public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
            String id = stringIds.get(download.getId());
            stringIds.remove(download.getId());
            intIds.remove(id);
            ErrorOccurred(id, error.toString());
        }

        @Override
        public void onPaused(@NotNull Download download) {
            String id = stringIds.get(download.getId());
            DownloadPaused(id);
        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
            ProgressChanged(stringIds.get(download.getId()), download.getProgress(), etaInMilliSeconds, downloadedBytesPerSecond, download.getDownloaded(), download.getTotal());
        }

        @Override
        public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
        }

        @Override
        public void onResumed(@NotNull Download download) {
            DownloadResumed(stringIds.get(download.getId()));
        }

        @Override
        public void onStarted(@NotNull Download download, @NotNull List<? extends DownloadBlock> downloadBlocks, int totalBlocks) {
            DownloadStarted(stringIds.get(download.getId()));
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

    @SimpleEvent(description = "[DEPRECATED] Use ProgressChanged instead")
    public void OnProgress(String id, int progress, long eta, long speed) {
        EventDispatcher.dispatchEvent(this, "OnProgress", id, progress == -1 ? 0 : progress, eta == -1 ? 0 : eta, speed);
    }

    @SimpleEvent
    public void ProgressChanged(String id, int progress, long eta, long speed, long downloaded, long totalSize) {
        Object[] args = new Object[6];
        args[0] = id;
        args[1] = progress == -1 ? 0 : progress;
        args[2] = eta == -1 ? 0 : eta;
        if (convertBytes) {
            args[3] = ConvertBytes(speed);
            args[4] = ConvertBytes(downloaded);
            args[5] = ConvertBytes(totalSize);
        } else {
            args[3] = speed;
            args[4] = downloaded;
            args[5] = totalSize;
        }
        EventDispatcher.dispatchEvent(this, "ProgressChanged", args);
    }

    @SimpleEvent
    public void DownloadCompleted(String id) {
        EventDispatcher.dispatchEvent(this, "DownloadCompleted", id);
    }

    @SimpleFunction(description = "Initialize the downloader. \n You must use this block first to initialize the downloader than any other block")
    public void Initialize(int downloadConcurrentLimit) {
        try {
            FetchConfiguration.Builder configuration = new FetchConfiguration.Builder(context)
                    .enableAutoStart(false)
                    .setNamespace("DEFAULT")
                    .setDownloadConcurrentLimit(downloadConcurrentLimit);
            this.fetch = Fetch.Impl.getInstance(configuration.build());
            this.fetch.deleteAllWithStatus(Status.DOWNLOADING);
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
    public boolean PermissionGranted() {
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permission != PackageManager.PERMISSION_DENIED;
    }

    @SimpleProperty(description = "Specifies the network type that you want to run your downloads on")
    public void NetworkType(int type) {
        this.networkType = getNetworkType(type);
    }

    @SimpleFunction
    public void DownloadFile(final String id, final String path, final String url) {
        try {
            Request request = new Request(url, path);
            request.setAutoRetryMaxAttempts(0);
            request.setNetworkType(networkType);
            if (fetch != null) {
                fetch.enqueue(request, new Func<Request>() {
                    @Override
                    public void call(@NotNull Request request) {
                        intIds.put(id, request.getId());
                        stringIds.put(request.getId(), id);
                        DownloadQueued(id);
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
        if (this.intIds.containsKey(id))
            fetch.pause(intIds.get(id));
        else
            throw new IllegalStateException("Id does not exist");
    }

    @SimpleFunction
    public void Resume(String id) {
        if (this.intIds.containsKey(id))
            fetch.resume(intIds.get(id));
        else
            throw new IllegalStateException("Id does not exist");
    }

    @SimpleFunction
    public void Cancel(String id) {
        if (this.intIds.containsKey(id)) {
            fetch.delete(intIds.get(id));
            stringIds.remove(intIds.get(id));
            intIds.remove(id);
        } else
            throw new IllegalStateException("Id does not exist");
    }

    @SimpleFunction
    public String GetASDPath() {
        return context.getExternalFilesDir(null).getPath();
    }

    @SimpleFunction(description = "This block cancel all the downloads")
    public void CancelAllDownloads() {
        this.stringIds.clear();
        this.intIds.clear();
        this.fetch.removeAll();
    }

    @SimpleFunction(description = "This block delete all the downloaded files from cache or from the file manager")
    public void DeleteAllDownloads() {
        stringIds.clear();
        intIds.clear();
        this.fetch.deleteAll();
    }

    @SimpleFunction(description = "Converts the given bytes into MBs/KBs and GBs.\n This block converts the bytes to suitable format")
    public String ConvertBytes(long bytes) {
        double mb = 1024 * 1024;
        double gb = 1024 * 1024 * 1024;
        if (bytes < mb)
            return String.format(Locale.ENGLISH, "%.2fKB", bytes / (1024.00));
        else if (bytes < gb)
            return String.format(Locale.ENGLISH, "%.2fMB", bytes / mb);
        return String.format(Locale.ENGLISH, "%.2fGB", bytes / gb);
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

    @SimpleProperty(description = "If set to true then extension will convert the bytes into suitable units itself")
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    public void AutoConvertBytes(boolean is) {
        this.convertBytes = is;
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
            fetch.close();
    }
}