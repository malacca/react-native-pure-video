package com.malacca.purevideo;

import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ArrayList;
import java.net.CookiePolicy;
import java.net.CookieManager;
import java.net.CookieHandler;

import android.net.Uri;
import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.SurfaceView;
import android.view.TextureView;
import android.text.TextUtils;
import android.graphics.Color;
import android.content.Context;
import android.media.AudioManager;
import android.widget.FrameLayout;
import android.annotation.SuppressLint;

import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.ThemedReactContext;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.video.VideoListener;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy;
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy;

import com.malacca.purevideo.receiver.BecomingNoisyListener;
import com.malacca.purevideo.receiver.AudioBecomingNoisyReceiver;

@SuppressLint("ViewConstructor")
class PureVideoView extends FrameLayout implements
        LifecycleEventListener,
        VideoListener,
        Player.EventListener,
        MetadataOutput,
        TextOutput,
        BecomingNoisyListener,
        BandwidthMeter.EventListener
{
    private static final String TAG = "ReactExoplayerView";
    private static final CookieManager DEFAULT_COOKIE_MANAGER;
    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }
    private static final int SHOW_PROGRESS = 1;
    private final ThemedReactContext reactContext;
    private final VideoEventEmitter eventEmitter;
    private final VideoAspectFrameLayout aspectLayout;
    private final ViewGroup.LayoutParams subLayoutParams;

    // player initialized
    private boolean initialized;
    private AudioManager audioManager;
    private DefaultBandwidthMeter bandwidthMeter;
    private AudioBecomingNoisyReceiver audioBecomingNoisyReceiver;

    // player resource
    private DefaultTrackSelector trackSelector;
    private DataSource.Factory mediaDataSourceFactory;
    private SimpleExoPlayer exoPlayer;
    private View surfaceView;
    private VideoSubtitleView subtitleLayout;

    // player current status
    private int resumeWindow;
    private long resumePosition;
    private boolean playerNeedsSource;
    private boolean loadVideoStarted;
    private boolean isBuffering;
    private boolean isInBackground;
    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    private int audioIoFocusStatus = AudioManager.AUDIOFOCUS_LOSS;
    private boolean isPlayedBeforeLossTransient;

    // React Props
    private Uri srcUri;
    private String srcExtension;
    private Map<String, String> requestHeaders;
    private boolean isPaused;
    private boolean muted;
    private float audioVolume = 1f;
    private long seekTime = C.TIME_UNSET;
    private boolean repeat;
    private float rate = 1f;
    private int minLoadRetryCount = 3;
    private int maxBitRate = 0;
    private boolean playInBackground;
    private boolean disableFocus;
    private float mProgressUpdateInterval = 250.0f;
    private boolean useTextureView = true;

    private ReadableArray textTracks;
    private String videoTrackType;
    private Dynamic videoTrackValue;
    private String audioTrackType;
    private Dynamic audioTrackValue;
    private String textTrackType;
    private Dynamic textTrackValue;

    private int textForegroundColor = Color.WHITE;
    private int textBackgroundColor = Color.TRANSPARENT;
    private @CaptionStyleCompat.EdgeType int textEdgeType = CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW;
    private int textEdgeColor = Color.BLACK;
    private float textPaddingFraction = VideoSubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION;
    private float textSizeFraction = VideoSubtitleView.DEFAULT_TEXT_SIZE_FRACTION;
    private boolean disableFontScale = false;
    private boolean textIgnorePadding = false;

    private int minBufferMs = DefaultLoadControl.DEFAULT_MIN_BUFFER_MS;
    private int maxBufferMs = DefaultLoadControl.DEFAULT_MAX_BUFFER_MS;
    private int bufferForPlaybackMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS;
    private int bufferForPlaybackAfterRebufferMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS;

    public PureVideoView(ThemedReactContext context) {
        super(context);
        reactContext = context;
        eventEmitter = new VideoEventEmitter(context);
        aspectLayout = new VideoAspectFrameLayout(context);
        LayoutParams aspectRatioParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        aspectRatioParams.gravity = Gravity.CENTER;
        aspectLayout.setLayoutParams(aspectRatioParams);
        addViewInLayout(aspectLayout, 0, aspectRatioParams);
        subLayoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        eventEmitter.setViewId(id);
    }

    // 初始化播放器资源, 在 react props 准备完毕后执行(仅执行一次)
    public void initializePlayer() {
        if (initialized) {
            return;
        }
        initialized = true;
        audioManager = (AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE);
        audioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver(reactContext);
        bandwidthMeter = new DefaultBandwidthMeter.Builder(reactContext).build();
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }
        audioBecomingNoisyReceiver.setListener(this);
        bandwidthMeter.addEventListener(new Handler(), this);
        reactContext.addLifecycleEventListener(this);
        // 初始化
        clearResumePosition();
        createPlayer();
        updateSurfaceView();
        updateVideoSource();
    }

    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
    }

    private void updateResumePosition() {
        if (exoPlayer == null) {
            return;
        }
        resumeWindow = exoPlayer.getCurrentWindowIndex();
        resumePosition = exoPlayer.isCurrentWindowSeekable() ? Math.max(0, exoPlayer.getCurrentPosition())
                : C.TIME_UNSET;
    }

    // BecomingNoisyListener(audioBecomingNoisyReceiver.setListener) 实现
    @Override
    public void onAudioBecomingNoisy() {
        eventEmitter.audioBecomingNoisy();
    }

    //BandwidthMeter.EventListener(bandwidthMeter.addEventListener) 实现
    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrate) {
        eventEmitter.bandwidthReport(elapsedMs, bytes, bitrate);
    }

    // LifecycleEventListener(reactContext.addLifecycleEventListener) 实现
    @Override
    public void onHostPause() {
        isInBackground = true;
        if (!playInBackground) {
            setPlayWhenReady(false, false);
        }
    }

    @Override
    public void onHostResume() {
        if (!playInBackground || !isInBackground) {
            setPlayWhenReady(!isPaused, false);
        }
        isInBackground = false;
    }

    @Override
    public void onHostDestroy() {
        releasePlayer();
        audioBecomingNoisyReceiver.removeListener();
        bandwidthMeter.removeEventListener(this);
        reactContext.removeLifecycleEventListener(this);
    }

    // 创建播放器
    private void createPlayer() {
        if (exoPlayer != null) {
            return;
        }
        // TODO: drmSessionManager 在这了 from: https://github.com/react-native-community/react-native-video/pull/1445
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(getContext())
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);

        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
        trackSelector = new DefaultTrackSelector(reactContext, videoTrackSelectionFactory);
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setMaxVideoBitrate(maxBitRate == 0 ? Integer.MAX_VALUE : maxBitRate));

        DefaultAllocator allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
        DefaultLoadControl.Builder defaultLoadControlBuilder = new DefaultLoadControl.Builder();
        defaultLoadControlBuilder.setAllocator(allocator);
        defaultLoadControlBuilder.setBufferDurationsMs(minBufferMs, maxBufferMs, bufferForPlaybackMs, bufferForPlaybackAfterRebufferMs);
        defaultLoadControlBuilder.setTargetBufferBytes(-1);
        defaultLoadControlBuilder.setPrioritizeTimeOverSizeThresholds(true);
        DefaultLoadControl defaultLoadControl = defaultLoadControlBuilder.createDefaultLoadControl();

        exoPlayer = new SimpleExoPlayer.Builder(reactContext, renderersFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(defaultLoadControl)
                .setBandwidthMeter(bandwidthMeter)
                .build();
        exoPlayer.setPlaybackParameters(new PlaybackParameters(rate, 1f));
        // add listener
        exoPlayer.addVideoListener(this);
        exoPlayer.addMetadataOutput(this);
        exoPlayer.addTextOutput(this);
        exoPlayer.addListener(this);
        setRepeatModifier(repeat);
        setMutedModifier(muted);
        setPlayWhenReady(!isPaused, false);
    }

    // 注销播放器监听 并 释放播放器
    private void releasePlayer() {
        onStopPlayback();
        clearProgressMessageHandler();
        if (exoPlayer != null) {
            exoPlayer.removeVideoListener(this);
            exoPlayer.removeMetadataOutput(this);
            exoPlayer.removeTextOutput(this);
            exoPlayer.removeListener(this);
            exoPlayer.release();
            trackSelector = null;
            exoPlayer = null;
        }
    }

    // 重建播放器
    private void reCreatePlayer() {
        playerNeedsSource = true;
        updateResumePosition();
        releasePlayer();
        createPlayer();
        updateVideoSource();
    }

    // VideoListener(exoPlayer.addVideoListener) 实现
    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        if (aspectLayout.setAspectRatio(width, height, pixelWidthHeightRatio)) {
            runRelayout();
        }
        eventEmitter.videoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
    }

    // MetadataOutput(exoPlayer.addMetadataOutput) 实现
    @Override
    public void onMetadata(Metadata metadata) {
        eventEmitter.timedMetadata(metadata);
    }

    // TextOutput(exoPlayer.addTextOutput) 实现
    @Override
    public void onCues(List<Cue> cues) {
        if (subtitleLayout != null) {
            subtitleLayout.onCues(cues);
        }
    }

    // Player.EventListener(exoPlayer.addListener) 实现
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        StringBuilder state = new StringBuilder("onStateChanged: playWhenReady=" + playWhenReady + ", playbackState=");
        switch (playbackState) {
            case Player.STATE_IDLE:
                state.append("idle");
                clearProgressMessageHandler();
                eventEmitter.idle();
                break;
            case Player.STATE_BUFFERING:
                state.append("buffering");
                clearProgressMessageHandler();
                onBuffering(true);
                break;
            case Player.STATE_READY:
                state.append("ready");
                eventEmitter.ready();
                onBuffering(false);
                startProgressHandler();
                videoLoaded();
                break;
            case Player.STATE_ENDED:
                state.append("ended");
                onStopPlayback();
                eventEmitter.end();
                break;
            default:
                state.append("unknown");
                break;
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, state.toString());
        }
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        if (playerNeedsSource) {
            // This will only occur if the user has performed a seek whilst in the error state. Update the
            // resume position so that if the user then retries, playback will resume from the position to
            // which they seeked.
            updateResumePosition();
        }
        // When repeat is turned on, reaching the end of the video will not cause a state change
        // so we need to explicitly detect it.
        if (reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION
                && exoPlayer.getRepeatMode() == Player.REPEAT_MODE_ONE) {
            eventEmitter.end();
        }
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters params) {
        eventEmitter.playbackRateChange(params.speed);
    }

    @Override
    public void onSeekProcessed() {
        eventEmitter.seek(exoPlayer.getCurrentPosition(), seekTime);
        seekTime = C.TIME_UNSET;
    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {
        String errorString = null;
        Exception ex = e;
        if (e.type == ExoPlaybackException.TYPE_RENDERER) {
            Exception cause = e.getRendererException();
            if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                // Special case for decoder initialization failures.
                MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
                        (MediaCodecRenderer.DecoderInitializationException) cause;
                if (decoderInitializationException.codecInfo == null) {
                    if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                        errorString = "Unable to query device decoders";
                    } else if (decoderInitializationException.secureDecoderRequired) {
                        errorString = "This device does not provide a secure decoder for " + decoderInitializationException.mimeType;
                    } else {
                        errorString = "This device does not provide a decoder for " + decoderInitializationException.mimeType;
                    }
                } else {
                    errorString = "Unable to instantiate decoder " + decoderInitializationException.codecInfo.name;
                }
            }
        } else if (e.type == ExoPlaybackException.TYPE_SOURCE) {
            ex = e.getSourceException();
            errorString = "Unrecognized media format";
        }
        if (errorString != null) {
            eventEmitter.error(errorString, ex);
        }
        if (isBehindLiveWindow(e)) {
            // todo: hls BehindLiveWindowException, 尝试拯救一下
            // todo: 该错误没重现, ?这里是否有必要移除当前进度
            clearResumePosition();
            playerNeedsSource = true;
            updateVideoSource();
        } else {
            updateResumePosition();
        }
    }

    private static boolean isBehindLiveWindow(ExoPlaybackException e) {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) {
            return false;
        }
        Throwable cause = e.getSourceException();
        while (cause != null) {
            if (cause instanceof BehindLiveWindowException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void onBuffering(boolean buffering) {
        if (isBuffering == buffering) {
            return;
        }
        isBuffering = buffering;
        eventEmitter.buffering(buffering);
    }

    private void videoLoaded() {
        if (!loadVideoStarted) {
            return;
        }
        loadVideoStarted = false;
        setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
        setSelectedTrack(C.TRACK_TYPE_AUDIO, audioTrackType, audioTrackValue);
        setSelectedTrack(C.TRACK_TYPE_TEXT, textTrackType, textTrackValue);
        Format videoFormat = exoPlayer.getVideoFormat();
        int width = videoFormat != null ? videoFormat.width : 0;
        int height = videoFormat != null ? videoFormat.height : 0;
        //exoPlayer.get
        eventEmitter.load(
                srcUri.toString(), exoPlayer.getDuration(), exoPlayer.getCurrentPosition(),
                width, height, getTrackInfo(C.TRACK_TYPE_VIDEO), getTrackInfo(C.TRACK_TYPE_AUDIO),
                getTrackInfo(C.TRACK_TYPE_TEXT)
        );
    }

    private void startProgressHandler() {
        progressHandler.sendEmptyMessage(SHOW_PROGRESS);
    }

    private void clearProgressMessageHandler() {
        progressHandler.removeMessages(SHOW_PROGRESS);
    }

    @SuppressLint("HandlerLeak")
    private final Handler progressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SHOW_PROGRESS
                    && exoPlayer != null
                    && exoPlayer.getPlaybackState() == Player.STATE_READY
                    && exoPlayer.getPlayWhenReady()
            ) {
                long pos = exoPlayer.getCurrentPosition();
                long bufferedDuration = exoPlayer.getBufferedPercentage() * exoPlayer.getDuration() / 100;
                eventEmitter.progressChanged(pos, bufferedDuration, exoPlayer.getDuration());
                msg = obtainMessage(SHOW_PROGRESS);
                sendMessageDelayed(msg, Math.round(mProgressUpdateInterval));
            }
        }
    };

    // 更新 exoPlayer video 视图
    private void updateSurfaceView() {
        if (surfaceView != null) {
            aspectLayout.removeView(surfaceView);
        }
        if (useTextureView) {
            surfaceView = new TextureView(reactContext);
            exoPlayer.setVideoTextureView((TextureView) surfaceView);
        } else {
            surfaceView = new SurfaceView(reactContext);
            exoPlayer.setVideoSurfaceView((SurfaceView) surfaceView);
        }
        surfaceView.setLayoutParams(subLayoutParams);
        aspectLayout.addView(surfaceView, 0, subLayoutParams);
        runRelayout();
    }

    // 更新 exoPlayer 字幕视图
    private void createSubtitleLayout() {
        if (subtitleLayout != null) {
            return;
        }
        subtitleLayout = new VideoSubtitleView(reactContext);
        subtitleLayout.setLayoutParams(subLayoutParams);
        setSubtitleLayoutStyle();
        aspectLayout.addView(subtitleLayout, 1, subLayoutParams);
        runRelayout();
    }

    private void setSubtitleLayoutStyle() {
        if (subtitleLayout == null) {
            return;
        }
        subtitleLayout.setFractionalTextSize(textSizeFraction, textIgnorePadding, disableFontScale);
        subtitleLayout.setStyle(new CaptionStyleCompat(
                textForegroundColor, // 前景色
                textBackgroundColor, // 背景色
                Color.TRANSPARENT,  // 整体色
                textEdgeType,       // 效果
                textEdgeColor,      // 效果色
                null
        ));
        subtitleLayout.setBottomPaddingFraction(textPaddingFraction);
    }

    // 更新 video source url, 以下关联参数发生变化, 需要重新调用该函数
    // reactProps: srcUri / srcExtension/ minLoadRetryCount / textTracks
    private void updateVideoSource() {
        try {
            resetVideoSource();
        } catch (Throwable ex) {
            eventEmitter.error("update video source failed", ex);
        }
    }

    private void resetVideoSource() {
        if (!playerNeedsSource || srcUri == null) {
            return;
        }
        ArrayList<MediaSource> mediaSourceList = buildTextSources();
        MediaSource videoSource = buildMediaSource(srcUri, srcExtension);
        MediaSource mediaSource;
        if (mediaSourceList.size() == 0) {
            mediaSource = videoSource;
        } else {
            mediaSourceList.add(0, videoSource);
            MediaSource[] textSourceArray = mediaSourceList.toArray(
                    new MediaSource[0]
            );
            mediaSource = new MergingMediaSource(textSourceArray);
        }
        boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
        if (haveResumePosition) {
            exoPlayer.seekTo(resumeWindow, resumePosition);
        }
        exoPlayer.prepare(mediaSource, !haveResumePosition, false);
        eventEmitter.loadStart();
        playerNeedsSource = false;
        loadVideoStarted = true;
    }

    private ArrayList<MediaSource> buildTextSources() {
        ArrayList<MediaSource> textSources = new ArrayList<>();
        if (textTracks != null) {
            for (int i = 0; i < textTracks.size(); ++i) {
                MediaSource textSource = buildTextSource(textTracks.getMap(i), i);
                if (textSource != null) {
                    textSources.add(textSource);
                }
            }
        }
        return textSources;
    }

    private MediaSource buildTextSource(ReadableMap textTrack, int index) {
        if (textTrack == null
                || !textTrack.hasKey("language")
                || !textTrack.hasKey("type")
                || !textTrack.hasKey("uri")
        ) {
            return null;
        }
        String language = textTrack.getString("language");
        Format textFormat = Format.createTextSampleFormat(
                textTrack.hasKey("title") ? textTrack.getString("title") : language + " " + index,
                textTrack.getString("type"), Format.NO_VALUE, language
        );
        return new SingleSampleMediaSource.Factory(mediaDataSourceFactory)
                .createMediaSource(Uri.parse(textTrack.getString("uri")), textFormat, C.TIME_UNSET);
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
        int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
                : Objects.requireNonNull(uri.getLastPathSegment()));
        LoadErrorHandlingPolicy policy = new DefaultLoadErrorHandlingPolicy(minLoadRetryCount);
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource.Factory(
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                        buildDataSourceFactory(false)
                ).setLoadErrorHandlingPolicy(policy).createMediaSource(uri);
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                        buildDataSourceFactory(false)
                ).setLoadErrorHandlingPolicy(policy).createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(
                        mediaDataSourceFactory
                ).setLoadErrorHandlingPolicy(policy).createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(
                        mediaDataSourceFactory
                ).setLoadErrorHandlingPolicy(policy).createMediaSource(uri);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return VideoDataSourceFactory.getHttpDataSourceFactory(
                reactContext,
                useBandwidthMeter ? bandwidthMeter : null,
                requestHeaders
        );
    }

    // 视频/音频/字幕 轨道设置
    public void setSelectedTrack(int trackType, String type, Dynamic value) {
        if (exoPlayer == null) {
            return;
        }
        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        if (info == null) {
            return;
        }
        int rendererIndex = getTrackRendererIndex(trackType);
        if (rendererIndex == C.INDEX_UNSET) {
            return;
        }
        DefaultTrackSelector.Parameters disableParameters = trackSelector.getParameters()
                .buildUpon()
                .setRendererDisabled(rendererIndex, true)
                .build();
        if (TextUtils.isEmpty(type)) {
            type = "default";
        }
        if (type.equals("disabled")) {
            trackSelector.setParameters(disableParameters);
            return;
        }
        int[] tracks = {0};
        int groupIndex = C.INDEX_UNSET;
        TrackGroupArray groups = info.getTrackGroups(rendererIndex);
        switch (type) {
            case "index":
                if (value.asInt() < groups.length) {
                    groupIndex = value.asInt();
                }
                break;
            case "language":
                for (int i = 0; i < groups.length; ++i) {
                    Format format = groups.get(i).getFormat(0);
                    if (format.language != null && format.language.equals(value.asString())) {
                        groupIndex = i;
                        break;
                    }
                }
                break;
            case "title":
                for (int i = 0; i < groups.length; ++i) {
                    Format format = groups.get(i).getFormat(0);
                    if (format.id != null && format.id.equals(value.asString())) {
                        groupIndex = i;
                        break;
                    }
                }
                break;
            case "height":
                int height = value.asInt();
                for (int i = 0; i < groups.length; ++i) { // Search for the exact height
                    TrackGroup group = groups.get(i);
                    for (int j = 0; j < group.length; j++) {
                        Format format = group.getFormat(j);
                        if (format.height == height) {
                            groupIndex = i;
                            tracks[0] = j;
                            break;
                        }
                    }
                }
                break;
            default:
                if (trackType == C.TRACK_TYPE_AUDIO || trackType == C.TRACK_TYPE_TEXT) {
                    // Audio Text default
                    groupIndex = getGroupIndexForDefaultLocale(groups);
                } else if (trackType == C.TRACK_TYPE_VIDEO && groups.length != 0) {
                    // Video auto
                    groupIndex = 0;
                    TrackGroup group = groups.get(0);
                    tracks = new int[group.length];
                    for (int j = 0; j < group.length; j++) {
                        tracks[j] = j;
                    }
                }
                break;
        }
        if (groupIndex == C.INDEX_UNSET) {
            trackSelector.setParameters(disableParameters);
            return;
        }
        if (trackType == C.TRACK_TYPE_TEXT) {
            createSubtitleLayout();
        }
        DefaultTrackSelector.Parameters selectionParameters = trackSelector.getParameters()
                .buildUpon()
                .setRendererDisabled(rendererIndex, false)
                .setSelectionOverride(rendererIndex, groups,
                        new DefaultTrackSelector.SelectionOverride(groupIndex, tracks))
                .build();
        trackSelector.setParameters(selectionParameters);
    }

    private int getGroupIndexForDefaultLocale(TrackGroupArray groups) {
        if (groups.length == 0){
            return C.INDEX_UNSET;
        }
        int groupIndex = 0; // default if no match
        String locale2 = Locale.getDefault().getLanguage(); // 2 letter code
        String locale3 = Locale.getDefault().getISO3Language(); // 3 letter code
        for (int i = 0; i < groups.length; ++i) {
            Format format = groups.get(i).getFormat(0);
            String language = format.language;
            if (language != null && (language.equals(locale2) || language.equals(locale3))) {
                groupIndex = i;
                break;
            }
        }
        return groupIndex;
    }

    // 视频/音频/字幕 轨道获取
    private WritableArray getTrackInfo(int trackType) {
        WritableArray tracks = Arguments.createArray();
        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        int index = getTrackRendererIndex(trackType);
        if (info == null || index == C.INDEX_UNSET) {
            return tracks;
        }
        TrackGroupArray groups = info.getTrackGroups(index);
        for (int i = 0; i < groups.length; ++i) {
            TrackGroup group = groups.get(i);
            Format format;
            switch (trackType) {
                case C.TRACK_TYPE_VIDEO:
                    for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                        format = group.getFormat(trackIndex);
                        WritableMap videoTrack = Arguments.createMap();
                        videoTrack.putInt("width", format.width == Format.NO_VALUE ? 0 : format.width);
                        videoTrack.putInt("height",format.height == Format.NO_VALUE ? 0 : format.height);
                        videoTrack.putInt("bitrate", format.bitrate == Format.NO_VALUE ? 0 : format.bitrate);
                        videoTrack.putString("codecs", format.codecs != null ? format.codecs : "");
                        videoTrack.putString("trackId",
                                format.id == null ? String.valueOf(trackIndex) : format.id);
                        tracks.pushMap(videoTrack);
                    }
                    break;
                case C.TRACK_TYPE_AUDIO:
                    WritableMap audioTrack = Arguments.createMap();
                    format = group.getFormat(0);
                    audioTrack.putInt("index", i);
                    audioTrack.putString("title", format.id != null ? format.id : "");
                    audioTrack.putString("type", format.sampleMimeType);
                    audioTrack.putString("language", format.language != null ? format.language : "");
                    audioTrack.putString("bitrate", format.bitrate == Format.NO_VALUE ? ""
                            : String.format(Locale.US, "%.2fMbps", format.bitrate / 1000000f));
                    tracks.pushMap(audioTrack);
                    break;
                case C.TRACK_TYPE_TEXT:
                    format = groups.get(i).getFormat(0);
                    WritableMap textTrack = Arguments.createMap();
                    textTrack.putInt("index", i);
                    textTrack.putString("title", format.id != null ? format.id : "");
                    textTrack.putString("type", format.sampleMimeType);
                    textTrack.putString("language", format.language != null ? format.language : "");
                    tracks.pushMap(textTrack);
                    break;
            }
        }
        return tracks;
    }

    private int getTrackRendererIndex(int trackType) {
        int rendererCount = exoPlayer.getRendererCount();
        for (int rendererIndex = 0; rendererIndex < rendererCount; rendererIndex++) {
            if (exoPlayer.getRendererType(rendererIndex) == trackType) {
                return rendererIndex;
            }
        }
        return C.INDEX_UNSET;
    }

    // relayout video 尺寸, 由于 RN 的问题, 在以下情况发生时需要 runRelayout
    // 1. 视频尺寸发生变化; 如 video加载完成后, resizeMode发生变化时
    // 2. 创建了新 view; 如 surfaceView, subtitleLayout
    private void runRelayout() {
        post(measureAndLayout);
    }

    private final Runnable measureAndLayout = new Runnable() {
        @Override
        public void run() {
            measure(
                    MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
            layout(getLeft(), getTop(), getRight(), getBottom());
        }
    };

    // 暂停/播放 视频
    private void setPlayWhenReady(boolean playWhenReady) {
        setPlayWhenReady(playWhenReady, true);
    }

    private void setPlayWhenReady(boolean playWhenReady, boolean emit) {
        if (exoPlayer == null || exoPlayer.getPlayWhenReady() == playWhenReady) {
            return;
        }
        if (!playWhenReady) {
            exoPlayer.setPlayWhenReady(false);
            setKeepScreenOn(false);
            if (emit) {
                eventEmitter.pausedChange(true);
            }
        } else if (requestAudioFocus()) {
            exoPlayer.setPlayWhenReady(true);
            setKeepScreenOn(true);
            if (emit) {
                eventEmitter.pausedChange(false);
            }
        }
    }

    // 申请成为新的声音焦点,暂停其他软件声音
    private boolean requestAudioFocus() {
        if (disableFocus
                || audioIoFocusStatus == AudioManager.AUDIOFOCUS_GAIN
                || audioIoFocusStatus == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
        ) {
            return true;
        }
        if (audioFocusChangeListener == null) {
            audioFocusChangeListener = new AudioFocusChangeListener();
        } else if (audioIoFocusStatus == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            abandonAudioFocus();
        }
        int result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        );
        boolean focus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if (focus) {
            audioIoFocusStatus = AudioManager.AUDIOFOCUS_GAIN;
        } else {
            abandonAudioFocus();
        }
        return focus;
    }

    private void abandonAudioFocus() {
        isPlayedBeforeLossTransient = false;
        audioIoFocusStatus = AudioManager.AUDIOFOCUS_LOSS;
        if (audioFocusChangeListener != null) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }
    }

    // 这里非常郁闷, 若 this 直接 implements OnAudioFocusChangeListener, 并实现 onAudioFocusChange
    // 实测在 onAudioFocusChange(LOSS) 内部调用 abandonAudioFocus(this) 无效, 所以采用这种方案
    class AudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                // 长期失去焦点(如被其他音乐软件占用), 停止播放并释放焦点
                case AudioManager.AUDIOFOCUS_LOSS:
                    abandonAudioFocus();
                    eventEmitter.audioFocusChanged(false);
                    setPlayWhenReady(false);
                    break;

                // 短暂失去焦点(如电话,短信), 暂停播放但不释放
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    audioIoFocusStatus = focusChange;
                    eventEmitter.audioFocusChanged(false);
                    if (exoPlayer != null && (isPlayedBeforeLossTransient = exoPlayer.getPlayWhenReady())) {
                        setPlayWhenReady(false);
                    }
                    break;

                // 临时失去焦点, 允许低音量播放
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    audioIoFocusStatus = focusChange;
                    if (exoPlayer != null && !muted) {
                        exoPlayer.setVolume(audioVolume * 0.8f);
                    }
                    break;

                // 重新获得焦点
                case AudioManager.AUDIOFOCUS_GAIN:
                    audioIoFocusStatus = focusChange;
                    eventEmitter.audioFocusChanged(true);
                    if (exoPlayer != null) {
                        if (!muted) {
                            exoPlayer.setVolume(audioVolume * 1);
                        }
                        if (isPlayedBeforeLossTransient) {
                            isPlayedBeforeLossTransient = false;
                            setPlayWhenReady(!isPaused);
                        }
                    }
                    break;
            }
        }
    }

    // 播放结束后
    private void onStopPlayback() {
        setKeepScreenOn(false);
        abandonAudioFocus();
    }

    /**
     * 以下方法对接  PureVideoManager props 设置
     */
    public void setSrc(Uri uri, String extension, Map<String, String> headers, boolean holdPosition) {
        if (uri == null || (srcUri != null && srcUri.equals(uri))) {
            return;
        }
        srcUri = uri;
        srcExtension = extension;
        requestHeaders = headers;
        mediaDataSourceFactory = buildDataSourceFactory(true);
        playerNeedsSource = true;
        updateVideoSrc(holdPosition);
    }

    public void setRawSrc(Uri uri, String extension, boolean holdPosition) {
        if (uri == null || (srcUri != null && srcUri.equals(uri))) {
            return;
        }
        srcUri = uri;
        srcExtension = extension;
        mediaDataSourceFactory = buildDataSourceFactory(true);
        playerNeedsSource = true;
        updateVideoSrc(holdPosition);
    }

    private void updateVideoSrc(boolean holdPosition) {
        if (!initialized) {
            return;
        }
        if (holdPosition) {
            updateResumePosition();
        } else {
            clearResumePosition();
        }
        updateVideoSource();
    }

    public void setResizeModeModifier(@VideoResizeMode.Mode int resizeMode) {
        if (aspectLayout.setResizeMode(resizeMode) && initialized) {
            runRelayout();
        }
    }

    public void setPausedModifier(boolean paused) {
        isPaused = paused;
        // 请求播放, 但不支持后台播放且刚好又在后台, 仅记录状态值, 不实际操作
        if (!initialized || exoPlayer == null || (!isPaused && isInBackground && !playInBackground)) {
            return;
        }
        setPlayWhenReady(!isPaused);
    }

    public void setMutedModifier(boolean isMuted) {
        muted = isMuted;
        audioVolume = muted ? 0.f : 1.f;
        if (initialized && exoPlayer != null) {
            exoPlayer.setVolume(audioVolume);
        }
    }

    public void setVolumeModifier(float volume) {
        audioVolume = volume;
        if (initialized && exoPlayer != null) {
            exoPlayer.setVolume(audioVolume);
        }
    }

    public void seekTo(long positionMs) {
        if (initialized && exoPlayer != null) {
            seekTime = positionMs;
            exoPlayer.seekTo(positionMs);
        }
    }

    public void setRepeatModifier(boolean enableRepeat) {
        repeat = enableRepeat;
        if (initialized && exoPlayer != null) {
            if (enableRepeat) {
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            } else {
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
            }
        }
    }

    public void setRateModifier(float newRate) {
        rate = newRate;
        if (initialized && exoPlayer != null) {
            PlaybackParameters params = new PlaybackParameters(rate, 1f);
            exoPlayer.setPlaybackParameters(params);
        }
    }

    // 二次修改会重建 resource
    public void setMinLoadRetryCountModifier(int newMinLoadRetryCount) {
        if (newMinLoadRetryCount == minLoadRetryCount) {
            return;
        }
        minLoadRetryCount = newMinLoadRetryCount;
        // 已 loadVideoStarted 的, 就不再 updateVideoSource 了
        if (initialized && exoPlayer != null && loadVideoStarted) {
            playerNeedsSource = true;
            updateVideoSource();
        }
    }

    public void setMaxBitRateModifier(int newMaxBitRate) {
        maxBitRate = newMaxBitRate;
        if (initialized && exoPlayer != null) {
            trackSelector.setParameters(trackSelector.buildUponParameters()
                    .setMaxVideoBitrate(maxBitRate == 0 ? Integer.MAX_VALUE : maxBitRate));
        }
    }

    public void setUseTextureView(boolean enable) {
        useTextureView = enable;
        if (initialized) {
            updateSurfaceView();
        }
    }

    public void setPlayInBackground(boolean inBackground) {
        playInBackground = inBackground;
    }

    public void setDisableFocus(boolean disable) {
        disableFocus = disable;
    }

    public void setProgressUpdateInterval(final float progressUpdateInterval) {
        mProgressUpdateInterval = progressUpdateInterval;
    }

    public void setSelectedVideoTrack(String type, Dynamic value) {
        videoTrackType = type;
        videoTrackValue = value;
        if (initialized && exoPlayer != null) {
            setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
        }
    }

    public void setSelectedAudioTrack(String type, Dynamic value) {
        audioTrackType = type;
        audioTrackValue = value;
        if (initialized && exoPlayer != null) {
            setSelectedTrack(C.TRACK_TYPE_AUDIO, audioTrackType, audioTrackValue);
        }
    }

    public void setSelectedTextTrack(String type, Dynamic value) {
        textTrackType = type;
        textTrackValue = value;
        if (initialized && exoPlayer != null) {
            setSelectedTrack(C.TRACK_TYPE_TEXT, textTrackType, textTrackValue);
        }
    }

    // 二次修改会重建 resource
    public void setTextTracks(ReadableArray tracks) {
        textTracks = tracks;
        if (initialized && exoPlayer != null) {
            updateResumePosition();
            updateVideoSource();
        }
    }

    public void setTextTrackStyle(ReadableMap style) {
        if (style == null) {
            return;
        }
        if (style.hasKey("color")) {
            textForegroundColor = style.getInt("color");
        }
        if (style.hasKey("background")) {
            textBackgroundColor = style.getInt("background");
        }
        if (style.hasKey("edgeType")) {
            textEdgeType = style.getInt("edgeType");
        }
        if (style.hasKey("edgeColor")) {
            textEdgeColor = style.getInt("edgeColor");
        }
        if (style.hasKey("paddingFraction")) {
            textPaddingFraction = (float) style.getDouble("paddingFraction");
        }
        if (style.hasKey("sizeFraction")) {
            textSizeFraction = (float) style.getDouble("sizeFraction");
        }
        if (style.hasKey("disableScale")) {
            disableFontScale = style.getBoolean("disableScale");
        }
        if (style.hasKey("ignorePadding")) {
            textIgnorePadding = style.getBoolean("ignorePadding");
        }
        if (subtitleLayout != null) {
            setSubtitleLayoutStyle();
        }
    }

    // 重置 buffer config, 不建议后期修改
    public void setBufferConfig(int newMinBufferMs, int newMaxBufferMs, int newBufferForPlaybackMs, int newBufferForPlaybackAfterRebufferMs) {
        minBufferMs = newMinBufferMs;
        maxBufferMs = newMaxBufferMs;
        bufferForPlaybackMs = newBufferForPlaybackMs;
        bufferForPlaybackAfterRebufferMs = newBufferForPlaybackAfterRebufferMs;
        if (initialized && exoPlayer != null) {
            reCreatePlayer();
        }
    }

    public void setListeners(ReadableMap listeners) {
        eventEmitter.setEmitListeners(listeners);
    }
}
