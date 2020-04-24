package com.malacca.purevideo;

import java.util.Map;
import java.util.HashMap;
import javax.annotation.Nullable;

import android.net.Uri;
import android.text.TextUtils;
import android.content.Context;
import androidx.annotation.NonNull;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;

class PureVideoManager extends SimpleViewManager<PureVideoView> {

    @NonNull
    @Override
    public String getName() {
        return "RCTPureVideo";
    }

    @NonNull
    @Override
    protected PureVideoView createViewInstance(@NonNull ThemedReactContext reactContext) {
        return new PureVideoView(reactContext);
    }

    @Override
    public void onDropViewInstance(PureVideoView videoView) {
        videoView.onHostDestroy();
    }

    // props 接收完毕后 initializePlayer
    @Override
    protected void onAfterUpdateTransaction(@NonNull PureVideoView videoView) {
        super.onAfterUpdateTransaction(videoView);
        videoView.initializePlayer();
    }

    @ReactProp(name = "src")
    public void setSrc(final PureVideoView videoView, ReadableMap src) {
        Context context = videoView.getContext().getApplicationContext();
        String uriString = src.hasKey("uri") ? src.getString("uri") : null;
        if (TextUtils.isEmpty(uriString)) {
            return;
        }
        String extension = src.hasKey("type") ? src.getString("type") : null;
        boolean holdPosition = src.hasKey("holdPosition") && src.getBoolean("holdPosition");
        boolean withValidScheme = uriString.startsWith("http://")
                || uriString.startsWith("https://")
                || uriString.startsWith("content://")
                || uriString.startsWith("file://")
                || uriString.startsWith("asset://");
        // url
        if (withValidScheme) {
            Uri srcUri = Uri.parse(uriString);
            if (srcUri != null) {
                videoView.setSrc(
                        srcUri,
                        extension,
                        src.hasKey("requestHeaders") ? toStringMap(src.getMap("requestHeaders")) : null,
                        holdPosition
                );
            }
            return;
        }
        // RawSrc
        int identifier = context.getResources().getIdentifier(
                uriString,
                "raw",
                context.getPackageName()
        );
        if (identifier == 0) {
            identifier = context.getResources().getIdentifier(
                    uriString,
                    "drawable",
                    context.getPackageName()
            );
        }
        if (identifier > 0) {
            Uri srcUri = RawResourceDataSource.buildRawResourceUri(identifier);
            if (srcUri != null) {
                videoView.setRawSrc(srcUri, extension, holdPosition);
            }
        }
    }

    @ReactProp(name = "resizeMode")
    public void setResizeMode(final PureVideoView videoView, final int mode) {
        videoView.setResizeModeModifier(VideoResizeMode.toResizeMode(mode));
    }

    @ReactProp(name = "paused")
    public void setPaused(final PureVideoView videoView, final boolean paused) {
        videoView.setPausedModifier(paused);
    }

    @ReactProp(name = "muted")
    public void setMuted(final PureVideoView videoView, final boolean muted) {
        videoView.setMutedModifier(muted);
    }

    @ReactProp(name = "volume", defaultFloat = 1.0f)
    public void setVolume(final PureVideoView videoView, final float volume) {
        videoView.setVolumeModifier(volume);
    }

    @ReactProp(name = "seek")
    public void setSeek(final PureVideoView videoView, final float seek) {
        videoView.seekTo(Math.round(seek * 1000f));
    }

    @ReactProp(name = "repeat")
    public void setRepeat(final PureVideoView videoView, final boolean repeat) {
        videoView.setRepeatModifier(repeat);
    }

    @ReactProp(name = "rate")
    public void setRate(final PureVideoView videoView, final float rate) {
        videoView.setRateModifier(rate);
    }

    @ReactProp(name = "minLoadRetryCount")
    public void minLoadRetryCount(final PureVideoView videoView, final int minLoadRetryCount) {
        videoView.setMinLoadRetryCountModifier(minLoadRetryCount);
    }

    @ReactProp(name = "maxBitRate")
    public void setMaxBitRate(final PureVideoView videoView, final int maxBitRate) {
        videoView.setMaxBitRateModifier(maxBitRate);
    }

    @ReactProp(name = "playInBackground")
    public void setPlayInBackground(final PureVideoView videoView, final boolean playInBackground) {
        videoView.setPlayInBackground(playInBackground);
    }

    @ReactProp(name = "disableFocus")
    public void setDisableFocus(final PureVideoView videoView, final boolean disableFocus) {
        videoView.setDisableFocus(disableFocus);
    }

    @ReactProp(name = "progressUpdateInterval", defaultFloat = 250.0f)
    public void setProgressUpdateInterval(final PureVideoView videoView, final float progressUpdateInterval) {
        videoView.setProgressUpdateInterval(progressUpdateInterval);
    }

    @ReactProp(name = "useTextureView", defaultBoolean = true)
    public void setUseTextureView(final PureVideoView videoView, final boolean useTextureView) {
        videoView.setUseTextureView(useTextureView);
    }

    @ReactProp(name = "textTracks")
    public void setPropTextTracks(final PureVideoView videoView,
                                  @Nullable ReadableArray textTracks) {
        videoView.setTextTracks(textTracks);
    }

    @ReactProp(name = "selectedVideoTrack")
    public void setSelectedVideoTrack(final PureVideoView videoView,
                                      @Nullable ReadableMap selectedVideoTrack) {
        String typeString = null;
        Dynamic value = null;
        if (selectedVideoTrack != null) {
            typeString = selectedVideoTrack.hasKey("type")
                    ? selectedVideoTrack.getString("type") : null;
            value = selectedVideoTrack.hasKey("value")
                    ? selectedVideoTrack.getDynamic("value") : null;
        }
        videoView.setSelectedVideoTrack(typeString, value);
    }

    @ReactProp(name = "selectedAudioTrack")
    public void setSelectedAudioTrack(final PureVideoView videoView,
                                      @Nullable ReadableMap selectedAudioTrack) {
        String typeString = null;
        Dynamic value = null;
        if (selectedAudioTrack != null) {
            typeString = selectedAudioTrack.hasKey("type")
                    ? selectedAudioTrack.getString("type") : null;
            value = selectedAudioTrack.hasKey("value")
                    ? selectedAudioTrack.getDynamic("value") : null;
        }
        videoView.setSelectedAudioTrack(typeString, value);
    }

    @ReactProp(name = "selectedTextTrack")
    public void setSelectedTextTrack(final PureVideoView videoView,
                                     @Nullable ReadableMap selectedTextTrack) {
        String typeString = null;
        Dynamic value = null;
        if (selectedTextTrack != null) {
            typeString = selectedTextTrack.hasKey("type")
                    ? selectedTextTrack.getString("type") : null;
            value = selectedTextTrack.hasKey("value")
                    ? selectedTextTrack.getDynamic("value") : null;
        }
        videoView.setSelectedTextTrack(typeString, value);
    }

    @ReactProp(name = "textTrackStyle")
    public void setTextTrackStyle(final PureVideoView videoView,
                                     @Nullable ReadableMap textTrackStyle) {
        videoView.setTextTrackStyle(textTrackStyle);
    }

    @ReactProp(name = "bufferConfig")
    public void setBufferConfig(final PureVideoView videoView, @Nullable ReadableMap bufferConfig) {
        if (bufferConfig == null) {
            return;
        }
        int minBufferMs = DefaultLoadControl.DEFAULT_MIN_BUFFER_MS;
        int maxBufferMs = DefaultLoadControl.DEFAULT_MAX_BUFFER_MS;
        int bufferForPlaybackMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS;
        int bufferForPlaybackAfterRebufferMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS;
        if (bufferConfig.hasKey("minBufferMs")) {
            minBufferMs = bufferConfig.getInt("minBufferMs");
        }
        if (bufferConfig.hasKey("maxBufferMs")) {
            maxBufferMs = bufferConfig.getInt("maxBufferMs");
        }
        if (bufferConfig.hasKey("bufferForPlaybackMs")) {
            bufferForPlaybackMs = bufferConfig.getInt("bufferForPlaybackMs");
        }
        if (bufferConfig.hasKey("bufferForPlaybackAfterRebufferMs")) {
            bufferForPlaybackAfterRebufferMs = bufferConfig.getInt("bufferForPlaybackAfterRebufferMs");
        }
        videoView.setBufferConfig(minBufferMs, maxBufferMs, bufferForPlaybackMs, bufferForPlaybackAfterRebufferMs);
    }

    @ReactProp(name = "listeners")
    public void setListeners(final PureVideoView videoView, ReadableMap listeners) {
        videoView.setListeners(listeners);
    }

    // 事件注册
    @Override
    public @Nullable Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
        for (String event : VideoEventEmitter.Events) {
            builder.put(event, MapBuilder.of("registrationName", event));
        }
        return builder.build();
    }

    // 导出可用事件
    @Override
    public @Nullable Map<String, Object> getExportedViewConstants() {
        WritableArray events = Arguments.createArray();
        for (String event : VideoEventEmitter.Events) {
            events.pushString(event);
        }
        return MapBuilder.<String, Object>of(
                "events", events
        );
    }

    /**
     * toStringMap converts a {@link ReadableMap} into a HashMap.
     * @param readableMap The ReadableMap to be conveted.
     * @return A HashMap containing the data that was in the ReadableMap.
     * Adapted from https://github.com/artemyarulin/react-native-eval/blob/master/android/src/main/java/com/evaluator/react/ConversionUtil.java
     */
    private static Map<String, String> toStringMap(@Nullable ReadableMap readableMap) {
        if (readableMap == null) {
            return null;
        }
        com.facebook.react.bridge.ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        if (!iterator.hasNextKey()) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            result.put(key, readableMap.getString(key));
        }
        return result;
    }
}
