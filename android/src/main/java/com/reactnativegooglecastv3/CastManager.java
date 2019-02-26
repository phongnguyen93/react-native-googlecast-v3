package com.reactnativegooglecastv3;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import android.widget.Toast;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaLoadOptions;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import java.io.IOException;

import static com.google.android.gms.cast.framework.CastState.CONNECTED;
import static com.google.android.gms.cast.framework.CastState.CONNECTING;
import static com.reactnativegooglecastv3.GoogleCastPackage.NAMESPACE;
import static com.reactnativegooglecastv3.GoogleCastPackage.TAG;
import static com.reactnativegooglecastv3.GoogleCastPackage.metadata;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.common.images.WebImage;
import java.util.ArrayList;
import java.util.List;

public class CastManager {
    static CastManager instance;

    final Context parent;
    final CastContext castContext;
    final SessionManager sessionManager;
    final CastStateListenerImpl castStateListener;
    ReactContext reactContext;
    CastDevice castDevice;

    CastManager(Context parent) {
        this.parent = parent;
        CastContext castContext = null;
        SessionManager sessionManager = null;
        CastStateListenerImpl castStateListener = null;
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(parent) == ConnectionResult.SUCCESS) {
            try {
                castContext = CastContext.getSharedInstance(parent); // possible RuntimeException from this
                sessionManager = castContext.getSessionManager();
                castStateListener = new CastStateListenerImpl();
                castContext.addCastStateListener(castStateListener);
                sessionManager.addSessionManagerListener(new SessionManagerListenerImpl(), CastSession.class);
            } catch (RuntimeException re) {
                Log.w(TAG, "RuntimeException in CastManager.<init>. Cannot cast.", re);
            }
        } else {
            Log.w(TAG, "Google Play services not installed on device. Cannot cast.");
        }
        this.castContext = castContext;
        this.sessionManager = sessionManager;
        this.castStateListener = castStateListener;
    }

    public static void init(Context ctx) {
        instance = new CastManager(ctx);
    }

    public void load(String url, String title,String subtitle, String imageUri, int duration, final Promise promise) {
        Video video = new Video(url, title,subtitle, imageUri, duration);
        List<MediaQueueItem> queueItems = new ArrayList<>();
        MediaQueueItem queueItem = new MediaQueueItem.Builder(buildMediaInfo(video)).setAutoplay(true).setPreloadTime(5)
                .build();
        queueItems.add(queueItem);
        sessionManager.getCurrentCastSession().getRemoteMediaClient().queueLoad(
                queueItems.toArray(new MediaQueueItem[queueItems.size()]), 0, MediaStatus.REPEAT_MODE_REPEAT_OFF,
                duration * 1000, null).setResultCallback(
            new ResultCallbacks<RemoteMediaClient.MediaChannelResult>() {
                @Override public void onSuccess(
                    @NonNull RemoteMediaClient.MediaChannelResult mediaChannelResult) {
                     promise.resolve(true);
                }

                @Override public void onFailure(@NonNull Status status) {
                    promise.resolve(false);
                }
            });
    }

    private MediaMetadata buildMetadata(Video video) {
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, video.getTitle());
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE,video.getSubtitle());
        movieMetadata.addImage(new WebImage(Uri.parse(video.getImageUri())));
        return movieMetadata;
    }

    private MediaInfo buildMediaInfo(Video video) {
        return new MediaInfo.Builder(video.getUrl()).setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("application/x-mpegurl").setMetadata(buildMetadata(video)).build();
    }

    public void sendMessage(String namespace, String message) {
        CastSession session = sessionManager.getCurrentCastSession();
        if (session == null)
            return;
        try {
            session.sendMessage(namespace, message);
        } catch (RuntimeException re) {
            Log.w(TAG, "RuntimeException in CastManager.sendMessage.", re);
        }
    }

    public void disconnect() {
        try {
            sessionManager.endCurrentSession(true);
        } catch (RuntimeException re) {
            Log.w(TAG, "RuntimeException in CastManager.disconnect.", re);
        }
    }

    public void triggerStateChange() {
        this.castStateListener.onCastStateChanged(castContext.getCastState());
    }

    private class CastStateListenerImpl implements CastStateListener {
        @Override
        public void onCastStateChanged(int state) {
            Log.d(TAG, "onCastStateChanged: " + state);
            if (state == CONNECTING || state == CONNECTED) {
                castDevice = sessionManager.getCurrentCastSession().getCastDevice();
            } else {
                castDevice = null;
            }
            if (reactContext != null)
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("googleCastStateChanged", state);
        }
    }

    private class SessionManagerListenerImpl extends SessionManagerListenerBase {
        @Override
        public void onSessionStarted(CastSession session, String sessionId) {
            setMessageReceivedCallbacks(session);
        }

        @Override
        public void onSessionResumed(CastSession session, boolean wasSuspended) {
            setMessageReceivedCallbacks(session);
        }

        private void setMessageReceivedCallbacks(CastSession session) {
            try {
                if (reactContext != null)
                    session.setMessageReceivedCallbacks(metadata(NAMESPACE, "", reactContext),
                            new CastMessageReceivedCallback());
            } catch (IOException e) {
                Log.e(TAG, "Cast channel creation failed: ", e);
            }
        }
    }

    private class CastMessageReceivedCallback implements Cast.MessageReceivedCallback {
        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
            Log.d(TAG, "onMessageReceived: " + namespace + " / " + message);
            if (reactContext == null)
                return;
            WritableMap map = Arguments.createMap();
            map.putString("namespace", namespace);
            map.putString("message", message);
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("googleCastMessage",
                    map);

        }
    }

    public class Video {
        private String url, title, subtitle, imageUri;
        private long duration;

        public Video(String url, String title, String subtitle, String imageUri, long duration) {
            this.url = url;
            this.title = title;
            this.imageUri = imageUri;
            this.duration = duration;
            this.subtitle = subtitle;

        }

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }

        public String getImageUri() {
            return imageUri;
        }

        public String getSubtitle() {return subtitle;}

        public long getDuration() {
            return duration;
        }
    }

}
