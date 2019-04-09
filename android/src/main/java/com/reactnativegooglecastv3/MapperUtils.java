package com.reactnativegooglecastv3;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import org.json.JSONException;
import org.json.JSONObject;


class MapperUtils {

  static WritableMap mediaQueueItemToMap(@NonNull MediaQueueItem mediaQueueItem) {
    WritableMap map = Arguments.createMap();
    if (mediaQueueItem.getMedia() != null && mediaQueueItem.getMedia().getMetadata() != null) {
      WritableMap mediaMap = Arguments.createMap();
      mediaMap.putString("title",
          mediaQueueItem.getMedia().getMetadata().getString(MediaMetadata.KEY_TITLE));
      mediaMap.putString("subtitle",
          mediaQueueItem.getMedia().getMetadata().getString(MediaMetadata.KEY_SUBTITLE));
      if (mediaQueueItem.getMedia().getMetadata().getImages() != null
          && mediaQueueItem.getMedia().getMetadata().getImages().size() > 0) {
        mediaMap.putString("thumb",
            mediaQueueItem.getMedia().getMetadata().getImages().get(0).getUrl().toString());
      }
      map.putMap("media", mediaMap);
    }
    if (mediaQueueItem.getCustomData() != null) {
      try {
        Bundle bundle = BundleJSONConverter.convertToBundle(mediaQueueItem.getCustomData());
        WritableMap customDataMap = Arguments.fromBundle(bundle);
        map.putMap("custom_data",customDataMap);
      } catch (JSONException exp) {
        Log.d("mediaQueueItemToMap", exp.getMessage());
      }
    }
    map.putInt("id", mediaQueueItem.getItemId());
    map.putInt("duration", (int) mediaQueueItem.getPlaybackDuration());
    map.putDouble("start", (int) mediaQueueItem.getStartTime());
    return map;
  }
}
