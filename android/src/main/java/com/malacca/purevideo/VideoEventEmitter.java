package com.malacca.purevideo;

import android.view.View;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.id3.Id3Frame;
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame;

class VideoEventEmitter {
    private static final String EVENT_BANDWIDTH = "onBandwidthUpdate";
    private static final String EVENT_AUDIO_BECOMING_NOISY = "onAudioBecomingNoisy";

    private static final String EVENT_IDLE = "onIdle";
    private static final String EVENT_BUFFER = "onBuffer";
    private static final String EVENT_READY = "onReady";
    private static final String EVENT_PROGRESS = "onProgress";
    private static final String EVENT_END = "onEnd";

    private static final String EVENT_LOAD_START = "onLoadStart";
    private static final String EVENT_LOAD = "onLoad";
    private static final String EVENT_SIZE_CHANGE = "onSizeChange";
    private static final String EVENT_TIMED_METADATA = "onTimedMetadata";
    private static final String EVENT_PAUSED = "onPaused";
    private static final String EVENT_SEEK = "onSeek";
    private static final String EVENT_RATE_CHANGE = "onRateChange";
    private static final String EVENT_AUDIO_FOCUS_CHANGE = "onAudioFocusChanged";
    private static final String EVENT_ERROR = "onError";

    static final String[] Events = {
            EVENT_BANDWIDTH,
            EVENT_AUDIO_BECOMING_NOISY,

            EVENT_IDLE,
            EVENT_BUFFER,
            EVENT_READY,
            EVENT_PROGRESS,
            EVENT_END,

            EVENT_LOAD_START,
            EVENT_LOAD,
            EVENT_SIZE_CHANGE,
            EVENT_TIMED_METADATA,
            EVENT_PAUSED,
            EVENT_SEEK,
            EVENT_RATE_CHANGE,
            EVENT_AUDIO_FOCUS_CHANGE,
            EVENT_ERROR,
    };

    private final RCTEventEmitter eventEmitter;
    private int viewId = View.NO_ID;
    private ReadableMap emitListeners;

    VideoEventEmitter(ReactContext reactContext) {
        eventEmitter = reactContext.getJSModule(RCTEventEmitter.class);
    }

    void setViewId(int id) {
        viewId = id;
    }

    void setEmitListeners(ReadableMap listeners) {
        emitListeners = listeners;
    }

    // 音频输出设备发生变化
    void audioBecomingNoisy() {
        receiveEvent(EVENT_AUDIO_BECOMING_NOISY, null);
    }

    // 网络变化
    void bandwidthReport(int elapsedMs, long bytes, long bitrate) {
        WritableMap map = Arguments.createMap();
        map.putInt("elapsedMs", elapsedMs);  // 距上次通知的间隔市场(毫秒)
        map.putDouble("bytes", bytes);       // 距上次通知, 新加载字节数
        map.putDouble("bitrate", bitrate);   // 带宽
        receiveEvent(EVENT_BANDWIDTH, map);
    }

    // 播放器空闲
    void idle() {
        receiveEvent(EVENT_IDLE, null);
    }

    // 加载状态
    void buffering(boolean buffering) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("buffering", buffering);
        receiveEvent(EVENT_BUFFER, map);
    }

    // 缓冲可播放了
    void ready() {
        receiveEvent(EVENT_READY, null);
    }

    // 播放结束
    void end() {
        receiveEvent(EVENT_END, null);
    }

    // 请求 video source 开始
    void loadStart() {
        receiveEvent(EVENT_LOAD_START, null);
    }

    // 加载成功, 在首次 ready 时调用
    void load(double duration, double currentPosition, int videoWidth, int videoHeight,
              WritableArray videoTracks, WritableArray audioTracks, WritableArray textTracks) {
        WritableMap event = Arguments.createMap();
        event.putDouble("duration", duration / 1000D);
        event.putDouble("currentTime", currentPosition / 1000D);
        event.putInt("width", videoWidth);
        event.putInt("height", videoHeight);
        event.putArray("videoTracks", videoTracks);
        event.putArray("audioTracks", audioTracks);
        event.putArray("textTracks", textTracks);
//        event.putBoolean("canPlayFastForward", true);
//        event.putBoolean("canPlaySlowForward", true);
//        event.putBoolean("canPlaySlowReverse", true);
//        event.putBoolean("canPlayReverse", true);
//        event.putBoolean("canStepBackward", true);
//        event.putBoolean("canStepForward", true);
        receiveEvent(EVENT_LOAD, event);
    }

    // 视频尺寸发生变化, 一般用不动, 主要是内部处理 resizeMode 需要监听, 顺道传递
    void videoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        WritableMap map = Arguments.createMap();
        map.putInt("width", width);
        map.putInt("height", height);
        map.putInt("unappliedRotationDegrees", unappliedRotationDegrees);
        map.putDouble("pixelWidthHeightRatio", pixelWidthHeightRatio);
        receiveEvent(EVENT_SIZE_CHANGE, map);
    }

    // 获取到视频元数据
    void timedMetadata(Metadata metadata) {
        WritableArray metadataArray = Arguments.createArray();
        for (int i = 0; i < metadata.length(); i++) {
            Id3Frame frame = (Id3Frame) metadata.get(i);
            String value = "";
            if (frame instanceof TextInformationFrame) {
                TextInformationFrame txxxFrame = (TextInformationFrame) frame;
                value = txxxFrame.value;
            }
            String identifier = frame.id;
            WritableMap map = Arguments.createMap();
            map.putString("identifier", identifier);
            map.putString("value", value);
            metadataArray.pushMap(map);
        }
        WritableMap event = Arguments.createMap();
        event.putArray("metadata", metadataArray);
        receiveEvent(EVENT_TIMED_METADATA, event);
    }

    // 暂停/播放
    void pausedChange(boolean paused) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("paused", paused);
        receiveEvent(EVENT_PAUSED, map);
    }

    // 播放进度实时通知
    void progressChanged(double currentPosition, double bufferedDuration, double seekableDuration) {
        WritableMap event = Arguments.createMap();
        event.putDouble("currentTime", currentPosition / 1000D);
        event.putDouble("playableDuration", bufferedDuration / 1000D);
        event.putDouble("seekableDuration", seekableDuration / 1000D);
        receiveEvent(EVENT_PROGRESS, event);
    }

    // 播放进度跳跃设定
    void seek(long currentPosition, long seekTime) {
        WritableMap event = Arguments.createMap();
        event.putDouble("currentTime", currentPosition / 1000D);
        event.putDouble("seekTime", seekTime / 1000D);
        receiveEvent(EVENT_SEEK, event);
    }

    // 播放速度发生变化
    void playbackRateChange(float rate) {
        WritableMap map = Arguments.createMap();
        map.putDouble("rate", rate);
        receiveEvent(EVENT_RATE_CHANGE, map);
    }

    // 声音焦点状态发生变化 (被其他软件占用 或 重新占用)
    void audioFocusChanged(boolean focused) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("focused", focused);
        receiveEvent(EVENT_AUDIO_FOCUS_CHANGE, map);
    }

    // 发生错误
    void error(String errorString, Throwable exception) {
        WritableMap error = Arguments.createMap();
        error.putString("errorString", errorString);
        error.putString("errorException", exception.getMessage());
        WritableMap event = Arguments.createMap();
        event.putMap("error", error);
        receiveEvent(EVENT_ERROR, event);
    }

    private void receiveEvent(String type, WritableMap event) {
        if (emitListeners != null && emitListeners.hasKey(type)) {
            eventEmitter.receiveEvent(viewId, type, event);
        }
    }
}
