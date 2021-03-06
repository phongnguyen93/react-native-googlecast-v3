package com.reactnativegooglecastv3;

import android.os.Handler;
import android.util.Log;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastState;
import java.util.HashMap;
import java.util.Map;

import static com.reactnativegooglecastv3.GoogleCastPackage.APP_ID;
import static com.reactnativegooglecastv3.GoogleCastPackage.NAMESPACE;
import static com.reactnativegooglecastv3.GoogleCastPackage.TAG;

public class CastModule extends ReactContextBaseJavaModule {
  final ReactApplicationContext reactContext;
  final Handler handler;

  public CastModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    handler = new Handler(reactContext.getMainLooper());
  }

  @Override public void initialize() {
    super.initialize();
    CastManager.instance.reactContext = reactContext;
  }

  @ReactMethod @SuppressWarnings("unused")
  public void send(final String namespace, final String message) {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.sendMessage(namespace, message);
      }
    });
  }

  @ReactMethod @SuppressWarnings("unused") public void togglePlayPause() {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.togglePlayPause();
      }
    });
  }

  @ReactMethod public void triggerUpdateCurrentState() {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.triggerUpdateCurrentState();
      }
    });
  }

  @ReactMethod @SuppressWarnings("unused") public void disconnect() {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.disconnect();
      }
    });
  }

  @ReactMethod public void connectToDevice(final String deviceId) {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.connectToDevice(deviceId);
      }
    });
  }

  @ReactMethod public void load(final String url, final String title, final String subtitle,
      final String imageUri, final Integer duration, final String customDataString,
      final Promise promise) {
    handler.post(new Runnable() {
      @Override public void run() {
        try {
          CastManager.instance.load(url, title, subtitle, imageUri, duration, customDataString,
              promise);
        } catch (Exception e) {
          promise.reject(e);
        }
      }
    });
  }

  @ReactMethod public void addCastProgressListener() {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.addCastProgressListener();
      }
    });
  }

  @ReactMethod public void removeCastProgressListener() {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.removeCastProgressListener();
      }
    });
  }

  @ReactMethod public void getCurrentCastState(final Promise promise) {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.getCurrentCastState(promise);
      }
    });
  }

  @ReactMethod public void getCurrentCastPlayerState(final Promise promise) {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.getCurrentCastPlayerState(promise);
      }
    });
  }

  @ReactMethod public void seek(final double position) {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.seek(position);
      }
    });
  }

  @ReactMethod public void queueNext() {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.queueNext();
      }
    });
  }

  @ReactMethod public void queuePrevious() {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.queuePrevious();
      }
    });
  }

  @ReactMethod public void startScan() {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.startScan();
      }
    });
  }

  @ReactMethod public void stopScan() {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.stopScan();
      }
    });
  }

  @ReactMethod public void getQueueItemByIndex(final int index, final Promise promise) {
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.getQueueItemByIndex(index, promise);
      }
    });
  }

  @ReactMethod @SuppressWarnings("unused") public void getCurrentDevice(Promise promise) {
    CastDevice d = CastManager.instance.castDevice;
    Log.d(TAG, "getCurrentDevice: " + d);
    if (d == null) {
      promise.resolve(null);
    } else {
      WritableMap map = Arguments.createMap();
      map.putString("id", d.getDeviceId());
      map.putString("version", d.getDeviceVersion());
      map.putString("name", d.getFriendlyName());
      map.putString("model", d.getModelName());
      promise.resolve(map);
    }
  }

  @ReactMethod @SuppressWarnings("unused") public void triggerStateChange() {
    if (CastManager.instance.castContext == null) return;
    handler.post(new Runnable() {
      @Override public void run() {
        CastManager.instance.triggerStateChange();
      }
    });
  }

  @Override public String getName() {
    return "GoogleCastV3";
  }

  @Override public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("appId", GoogleCastPackage.metadata(APP_ID, "", reactContext));
    constants.put("namespace", GoogleCastPackage.metadata(NAMESPACE, "", reactContext));
    constants.put("NO_DEVICES_AVAILABLE", CastState.NO_DEVICES_AVAILABLE);
    constants.put("NOT_CONNECTED", CastState.NOT_CONNECTED);
    constants.put("CONNECTING", CastState.CONNECTING);
    constants.put("CONNECTED", CastState.CONNECTED);
    constants.put("PLAYER_STATE_UNKNOWN", MediaStatus.PLAYER_STATE_UNKNOWN);
    constants.put("PLAYER_STATE_BUFFERING", MediaStatus.PLAYER_STATE_BUFFERING);
    constants.put("PLAYER_STATE_IDLE", MediaStatus.PLAYER_STATE_IDLE);
    constants.put("PLAYER_STATE_PAUSED", MediaStatus.PLAYER_STATE_PAUSED);
    constants.put("PLAYER_STATE_PLAYING", MediaStatus.PLAYER_STATE_PLAYING);
    constants.put("IDLE_REASON_NONE", MediaStatus.IDLE_REASON_NONE);
    constants.put("IDLE_REASON_CANCELED", MediaStatus.IDLE_REASON_CANCELED);
    constants.put("IDLE_REASON_ERROR", MediaStatus.IDLE_REASON_ERROR);
    constants.put("IDLE_REASON_FINISHED", MediaStatus.IDLE_REASON_FINISHED);
    constants.put("IDLE_REASON_INTERRUPTED", MediaStatus.IDLE_REASON_INTERRUPTED);
    return constants;
  }

  @Override public void onCatalystInstanceDestroy() {
    CastManager.instance.reactContext = null;
  }
}
