package com.reactnativegooglecastv3;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.text.TextUtils;
import android.util.Log;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

import static com.google.android.gms.cast.framework.CastState.CONNECTED;
import static com.google.android.gms.cast.framework.CastState.CONNECTING;
import static com.reactnativegooglecastv3.GoogleCastPackage.APP_ID;
import static com.reactnativegooglecastv3.GoogleCastPackage.NAMESPACE;
import static com.reactnativegooglecastv3.GoogleCastPackage.TAG;
import static com.reactnativegooglecastv3.GoogleCastPackage.metadata;

public class CastManager {
  static CastManager instance;

  final Context parent;
  final CastContext castContext;
  final SessionManager sessionManager;
  final CastStateListenerImpl castStateListener;
  ReactContext reactContext;
  CastDevice castDevice;
  private MediaRouteSelector selector;
  private MediaRouter mediaRouter;

  private RemoteMediaClient.ProgressListener progressListener =
      new RemoteMediaClient.ProgressListener() {
        @Override public void onProgressUpdated(long progress, long duration) {
          WritableMap map = Arguments.createMap();
          map.putDouble("progress", (double) progress);
          map.putDouble("duration", (double) duration);
          emitEvent("castProgress", map);
        }
      };

  private MediaRouter.Callback mediaRouterCallback = new MediaRouter.Callback() {
    @Override public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
      super.onRouteAdded(router, route);
      emitEvent("mediaRouteChange", getRoutes());
    }

    @Override public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
      super.onRouteRemoved(router, route);
      emitEvent("mediaRouteChange", getRoutes());
    }

    @Override public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
      super.onRouteChanged(router, route);
      emitEvent("mediaRouteChange", getRoutes());
    }
  };

  private RemoteMediaClient.Callback remoteMediaClientCallback = new RemoteMediaClient.Callback() {
    @Override public void onStatusUpdated() {
      super.onStatusUpdated();
     updatePlayerStatus();
    }

    @Override public void onMetadataUpdated() {
      super.onMetadataUpdated();
      updateMetadata();
    }

    @Override public void onQueueStatusUpdated() {
      super.onQueueStatusUpdated();
      updateQueueStatus();
    }

    @Override public void onPreloadStatusUpdated() {
      super.onPreloadStatusUpdated();
    }

    @Override public void onSendingRemoteMediaRequest() {
      super.onSendingRemoteMediaRequest();
    }

    @Override public void onAdBreakStatusUpdated() {
      super.onAdBreakStatusUpdated();
    }
  };



  CastManager(Context parent) {
    this.parent = parent;
    CastContext castContext = null;
    SessionManager sessionManager = null;
    CastStateListenerImpl castStateListener = null;
    if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(parent)
        == ConnectionResult.SUCCESS) {
      try {
        castContext = CastContext.getSharedInstance(parent); // possible RuntimeException from this
        sessionManager = castContext.getSessionManager();
        castStateListener = new CastStateListenerImpl();
        castContext.addCastStateListener(castStateListener);
        sessionManager.addSessionManagerListener(new SessionManagerListenerImpl(),
            CastSession.class);
      } catch (RuntimeException re) {
        Log.w(TAG, "RuntimeException in CastManager.<init>. Cannot cast.", re);
      }
    } else {
      Log.w(TAG, "Google Play services not installed on device. Cannot cast.");
    }
    this.castContext = castContext;
    this.sessionManager = sessionManager;
    this.castStateListener = castStateListener;
    selector = new MediaRouteSelector.Builder().addControlCategory(
        CastMediaControlIntent.categoryForCast(GoogleCastPackage.metadata(APP_ID, "", parent)))
        .build();
    mediaRouter = MediaRouter.getInstance(parent);
  }

  public static void init(Context ctx) {
    instance = new CastManager(ctx);
  }

  private void emitEvent(String eventName, Object payload) {
    if (reactContext != null) {
      reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
          .emit(eventName, payload);
    }
  }

  private RemoteMediaClient getMediaClient() {
    return this.sessionManager != null && this.sessionManager.getCurrentCastSession() != null
        ? this.sessionManager.getCurrentCastSession().getRemoteMediaClient() : null;
  }

  private void updateQueueStatus(){
    if (getMediaClient() != null && getMediaClient().getMediaStatus() != null) {
      int currentItemId = getMediaClient().getMediaStatus().getCurrentItemId();
      //Integer currentItemIndex = getMediaClient().getMediaStatus().getIndexById(currentItemId);
      WritableMap map = Arguments.createMap();
      WritableMap queueMap = Arguments.createMap();
      queueMap.putInt("total", getMediaClient().getMediaStatus().getQueueItemCount());
      queueMap.putInt("index",
          getMediaClient().getMediaStatus().getQueueItemCount() == 1 ? 0 : currentItemId);
      map.putMap("queue", queueMap);
      emitEvent("googleCastMessage", map);
    }
  }

  private void updateMetadata(){
    if (getMediaClient() != null
        && getMediaClient().getMediaInfo() != null
        && getMediaClient().getMediaInfo().getMetadata() != null) {
      MediaMetadata mediaMetadata = getMediaClient().getMediaInfo().getMetadata();
      WritableMap map = Arguments.createMap();
      map.putString("metadata", mediaMetadata.toJson().toString());
      emitEvent("googleCastMessage", map);
    }
  }

  private void updatePlayerStatus() {
    if (getMediaClient() != null) {
      WritableMap map = Arguments.createMap();
      WritableMap playerMap = Arguments.createMap();
      playerMap.putInt("state", getMediaClient().getPlayerState());
      if (getMediaClient().getPlayerState() == MediaStatus.PLAYER_STATE_IDLE) {
        playerMap.putInt("detail", getMediaClient().getIdleReason());
      }
      map.putMap("player", playerMap);
      emitEvent("googleCastMessage", map);
    }
  }

  public void togglePlayPause() {
    if (getMediaClient() != null) {
      getMediaClient().togglePlayback();
    }
  }

  public void connectToDevice(String deviceId) {
    List<MediaRouter.RouteInfo> routeInfos = mediaRouter.getRoutes();
    MediaRouter.RouteInfo selectRoute = null;
    for (MediaRouter.RouteInfo info : routeInfos) {
      if (info.getId().equals(deviceId)) {
        selectRoute = info;
        break;
      }
    }
    if (selectRoute != null) {
      mediaRouter.selectRoute(selectRoute);
    }
  }

  public void seek(double position) {
    if (getMediaClient() != null) {
      getMediaClient().seek((long) position, RemoteMediaClient.RESUME_STATE_UNCHANGED);
    }
  }

  public void startScan() {
    mediaRouter.addCallback(selector, mediaRouterCallback,
        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
  }

  public void stopScan() {
    mediaRouter.removeCallback(mediaRouterCallback);
  }

  public void queueNext() {
    if (getMediaClient() != null) {
      getMediaClient().queueNext(null);
    }
  }

  public void queuePrevious() {
    if (getMediaClient() != null) {
      getMediaClient().queuePrev(null);
    }
  }

  public void getCurrentCastState(Promise promise) {
    if (castContext != null) {
      promise.resolve(castContext.getCastState());
    } else {
      promise.resolve(false);
    }
  }

  public void triggerUpdateCurrentState(){
    updatePlayerStatus();
    updateQueueStatus();
    updateMetadata();
  }

  public void getCurrentCastPlayerState(Promise promise) {
    if (getMediaClient() != null) {
      WritableMap map = Arguments.createMap();
      WritableMap playerMap = Arguments.createMap();
      playerMap.putInt("state", getMediaClient().getPlayerState());
      map.putMap("player", playerMap);
      promise.resolve(map);
    } else {
      promise.resolve(false);
    }
  }

  public void addCastProgressListener() {
    if (getMediaClient() != null) {
      getMediaClient().addProgressListener(progressListener, 500);
    }
  }

  public void removeCastProgressListener() {
    if (getMediaClient() != null) {
      getMediaClient().removeProgressListener(progressListener);
    }
  }

  private WritableArray getRoutes() {
    WritableArray writableArray = Arguments.createArray();
    if (mediaRouter != null) {
      List<MediaRouter.RouteInfo> routeInfos = mediaRouter.getRoutes();
      for (MediaRouter.RouteInfo info : routeInfos) {
        if (!info.isDefault() && !info.isBluetooth()) {
          WritableMap map = Arguments.createMap();
          map.putString("id", info.getId());
          map.putString("name", info.getName());
          writableArray.pushMap(map);
        }
      }
    }
    return writableArray;
  }

  private void resolveResultCallback(PendingResult<RemoteMediaClient.MediaChannelResult> request,
      final Promise promise) {
    request.setResultCallback(new ResultCallbacks<RemoteMediaClient.MediaChannelResult>() {
      @Override
      public void onSuccess(@NonNull RemoteMediaClient.MediaChannelResult mediaChannelResult) {
        promise.resolve(true);
      }

      @Override public void onFailure(@NonNull Status status) {
        promise.resolve(false);
      }
    });
  }

  public void load(String url, String title, String subtitle, String imageUri, int duration,
      String customDataString, final Promise promise) {
    Video video = new Video(url, title, subtitle, imageUri, duration);

    try {
      JSONObject customData = null;
      if (!TextUtils.isEmpty(customDataString)) {
        customData = new JSONObject(customDataString);
      }
      List<MediaQueueItem> queueItems = new ArrayList<>();
      MediaQueueItem queueItem = new MediaQueueItem.Builder(buildMediaInfo(video)).setAutoplay(true)
          .setPreloadTime(5)
          .setCustomData(customData)
          .build();
      queueItems.add(queueItem);
      if (getMediaClient() != null) {

        if (getMediaClient().getMediaStatus() != null
            && getMediaClient().getMediaStatus().getQueueItemCount() > 0) {
          resolveResultCallback(
              getMediaClient().queueInsertAndPlayItem(queueItem, MediaQueueItem.INVALID_ITEM_ID,
                  duration * 1000, customData), promise);
        } else {
          resolveResultCallback(
              getMediaClient().queueLoad(queueItems.toArray(new MediaQueueItem[0]), 0,
                  MediaStatus.REPEAT_MODE_REPEAT_OFF, duration * 1000, customData), promise);
        }
      }
    } catch (JSONException error) {
      promise.reject(error.getMessage(), error);
    }
  }

  public void getQueueItemByIndex(int index, Promise promise) {
    if (index != -1 && getMediaClient() != null && getMediaClient().getMediaStatus() != null) {
      MediaQueueItem queueItem = getMediaClient().getMediaStatus().getItemByIndex(index);
      if (queueItem != null) {
        WritableMap item = MapperUtils.mediaQueueItemToMap(queueItem);
        Log.d("QueueItem",item.toString());
        promise.resolve(item);
      } else {
        promise.resolve(
            constructErrorMap("where: getQueueItemByIndex, cause: MediaQueueItem is null"));
      }
    } else {
      promise.reject(new Throwable("Invalid index or null ref on remote media client"));
    }
  }

  private MediaMetadata buildMetadata(Video video) {
    MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
    movieMetadata.putString(MediaMetadata.KEY_TITLE, video.getTitle());
    movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, video.getSubtitle());
    movieMetadata.addImage(new WebImage(Uri.parse(video.getImageUri())));
    return movieMetadata;
  }

  private MediaInfo buildMediaInfo(Video video) {
    return new MediaInfo.Builder(video.getUrl()).setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
        .setContentType("application/x-mpegurl")
        .setMetadata(buildMetadata(video))
        .build();
  }

  public void sendMessage(String namespace, String message) {
    CastSession session = sessionManager.getCurrentCastSession();
    if (session == null) return;
    try {
      session.sendMessage(namespace, message);
    } catch (RuntimeException re) {
      Log.w(TAG, "RuntimeException in CastManager.sendMessage.", re);
    }
  }

  private WritableMap constructErrorMap(String msg) {
    WritableMap map = Arguments.createMap();
    map.putString("msg", msg);
    return map;
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
    @Override public void onCastStateChanged(int state) {
      Log.d(TAG, "onCastStateChanged: " + state);
      if (state == CONNECTING || state == CONNECTED) {
        castDevice = sessionManager.getCurrentCastSession().getCastDevice();
        if (state == CONNECTED && getMediaClient() != null) {
          getMediaClient().registerCallback(remoteMediaClientCallback);
        }
      } else {
        castDevice = null;
        if (getMediaClient() != null) {
          getMediaClient().unregisterCallback(remoteMediaClientCallback);
        }
      }
      if (reactContext != null) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("googleCastStateChanged", state);
      }
    }
  }

  private class SessionManagerListenerImpl extends SessionManagerListenerBase {
    @Override public void onSessionStarted(CastSession session, String sessionId) {
      setMessageReceivedCallbacks(session);
    }

    @Override public void onSessionResumed(CastSession session, boolean wasSuspended) {
      setMessageReceivedCallbacks(session);
    }

    private void setMessageReceivedCallbacks(CastSession session) {
      try {
        if (reactContext != null) {
          session.setMessageReceivedCallbacks(metadata(NAMESPACE, "", reactContext),
              new CastMessageReceivedCallback());
        }
      } catch (IOException e) {
        Log.e(TAG, "Cast channel creation failed: ", e);
      }
    }
  }

  private class CastMessageReceivedCallback implements Cast.MessageReceivedCallback {
    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
      Log.d(TAG, "onMessageReceived: " + namespace + " / " + message);
      if (reactContext == null) return;
      WritableMap map = Arguments.createMap();
      map.putString("namespace", namespace);
      map.putString("message", message);
      emitEvent("googleCastMessage", map);
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

    public String getSubtitle() {
      return subtitle;
    }

    public long getDuration() {
      return duration;
    }
  }
}
