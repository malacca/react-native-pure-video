# react-native-pure-video

该项目源码来源于 [react-native-video](https://github.com/react-native-community/react-native-video/tree/fd8ac76e4dc8cf7226c6477314bd4c133f77d46c/android-exoplayer)，删除了原版的 Controls UI，升级了 ExoPlayer 版本，修改了部分接口。由于有不少的 Break change，所以这里单独创建，不再 fork 原版了。


（当前仅支持 android，使用 ExoPlayer，直接放弃了 mediaplayer）


# 安装

`yarn add react-native-pure-video`


# 属性


## `source` (Object)

视频地址，支持 `{uri: "(http|content|file|asset)://"}` 和 `{require("./mediaFile")}`

## `mediaType` (String)

ExoPlayer 支持 `Smoothstreaming` / `HLS` / `MPEG-DASH` / `OTHER`，一般情况下会根据 `source` 的文件名或 URL 路径自动判断，如果文件名或URL不规范，可通过该参数手动指定。以下为对应 `mediaType` 值

- ism: Smoothstreaming  (不常用，如：[test video](https://testweb.playready.microsoft.com/Content/Content2X))
- m3u8: HLS  (常用,国内流媒体网站一般用的都是这个)
- mpd: MPEG-DASH (不常用, 如 [test video](http://rdmedia.bbc.co.uk/))
- default: OTHER (常用, 支持大部分视频格式, 如: mp4, mp3, ogg等)

## `requestHeaders` (Object)

请求 `source uri` 支持自定义 Http Request header，格式为 `{key:value, key2:value}`

## `holdPosition` (Boolean)

动态修改 `source` 是否保持播放进度， 如同一个视频切换源，保持进度比较好，默认为 false;

（TODO: 同一个视频切换源，保持当前源继续播放，待新播放源可播放时再进行无缝切换）

## `minLoadRetryCount` (Int)

请求 `source` 失败尝试次数

## `maxBitRate` (Int)

请求 `source` 网速限制，默认为0，不限制

## `resizeMode` (String)

与 RN Image 组件相同，不支持 `repeat`，支持 `cover`、`contain`、`stretch`、`center` 默认为 `cover`

## `paused` (Boolean)

是否暂停，默认为 false

## `muted` (Boolean)

是否静音，默认为 false

## `volume` (Float)

音量大小，0 ~ 1 之间

## `seek` (Float)

seek 到指定播放进度

## `repeat` (Boolean)

是否循环播放，默认为 false

## `rate` (Float)

播放倍率

## `playInBackground` (Boolean)

应用处于后台时，是否持续播放，默认为 false

## `disableFocus` (Boolean)

播放前，用户在使用其他影音软件，默认情况下，会抢占音轨，暂停其他影音软件，待播放完毕释放。如果要禁用该功能，设置该属性为 true

## `useTextureView` (Boolean)

Android 播放画面可选用的原生组件一般为 `SurfaceView` 或 `TextureView`，默认使用的是 `SurfaceView`，设置该属性为 true， 则使用 `TextureView`

## `progressUpdateInterval` (float)

通知播放进度的间隔时长，即回调中的 `onProgress` 的触发间隔时长，单位为毫秒，默认 250

## `bufferConfig` (Object)

ExoPlayer 缓冲设置，该设置一旦设定，最好不要更改，因为 Native 端会重建

```js
{
    // 最小缓冲时长(毫秒)
    minBufferMs: 15000,
    // 最大缓冲时长(毫秒)
    maxBufferMs: 50000,
    // 在播放开始前, 必须缓存的最小时长(毫秒)
    bufferForPlaybackMs: 2500,
    // 重建缓冲区，在播放器需要缓存的最小时长(毫秒)
    bufferForPlaybackAfterRebufferMs: 5000
}
```


## `textTracks` (Object)

外挂字幕设置，其中 `language` 可参考 [ISO_639-1](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)，默认会根据用户系统语言选择对应的字幕。字幕格式支持 `SRT`、`TTML`、`VTT`，建议使用 VTT 格式，兼容性比较好。

```js
import PureVideo, {TextTrackType} from 'react-native-pure-video';

<PureVideo
    textTracks={[
    {
        title: "English CC",
        language: "en",
        type: TextTrackType.VTT, // "text/vtt" (支持直接使用 string)
        uri: "https://bitdash-a.akamaihd.net/content/sintel/subtitles/subtitles_en.vtt"
    },
    {
        title: "Spanish Subtitles",
        language: "es",
        type: TextTrackType.SRT, // "application/x-subrip"
        uri: "https://durian.blender.org/wp-content/content/subtitles/sintel_es.srt"
    }
    ]}
/>
```

## `textTrackStyle` (Object)

字幕样式，仅支持软字幕，软字幕可以是视频内部或外挂的 TextTrack，对于直接合并到画面的硬字幕是不支持的

```js
textTrackStyle={{
    color:"#fff",   // 字体颜色         
    background:"transparent",  // 背景颜色
    edgeType:"shadow", // 边缘样式, 支持 none|outline|shadow|raised|depressed
    edgeColor:"#000",  // 边缘颜色
    paddingFraction:8, // 字幕下边距百分比 (0 ~ 100)
    sizeFraction:5.3,  // 字幕文字大小，占整个画面的百分比 (0 ~ 100)
    disableScale:false,// 默认会在所设置的百分比基础上，根据系统文字大小设置进行缩放,
                       //是否禁用该功能
    ignorePadding:false,// 文字占用百分比计算，是否为去除下边距之后的画面
}}
```


## `selectedVideoTrack` / `selectedAudioTrack` / `selectedTextTrack`

设置视频使用的 画面/声音/字幕 轨道，具体使用方法，下面会详细解释


# 回调

## `onIdle`

播放器空闲，此时还没挂载上 source （无参数）

## `onLoadStart`

加载开始 （无参数）

## `onReady`

播放器准备完毕，此时已加载上第一帧的画面 （无参数）

## `onLoad`

加载完成，此时已缓冲了最小可播放时间，将要开始播放了，后面会解释。

## `onBuffer`

缓冲状态发生变化，buffer=true 时, 表示缓冲不足，此时可显示加载提示。

```js
onBuffer={ ({ 
    buffer
}) => {
    
} }
```

## `onProgress`

播放进度通知

```js
onProgress={ ({ 
    currentTime,
    playableDuration,
    seekableDuration
}) => {
    
} }
```

## `onEnd`

播放结束 （无参数）


## `onTimedMetadata`

HLS 直播流 timed Meta 获取成功，该回调并不是总会触发，需要 m3u8 支持。

```js
onProgress={ ({ 
    metadata
}) => {
    
} }

// ex:
metadata: [
    { value: 'Streaming Encoder', identifier: 'TRSN' },
    { value: 'Internet Stream', identifier: 'TRSO' },
    { value: 'Any Time You Like', identifier: 'TIT2' }
]
```

## `onSizeChange`

视频 size 发生变化，一般情况下，会在 onReady 后触发一次。

```js
onSizeChange={ ({ 
    width,
    height,
    unappliedRotationDegrees,
    pixelWidthHeightRatio
}) => {
    
} }
```

## `onPaused`

视频 暂停/播放；paused=true -> 暂停；paused=false -> 播放；

```js
onPaused={ ({ 
    paused
}) => {
    
} }
```

## `onSeek`

重新定位播放时间

```js
onSeek={ ({ 
    currentTime,
    seekTime
}) => {
    
} }
```

## `onRateChange`

播放速率发生变化

```js
onRateChange={ ({ 
    rate
}) => {
    
} }
```

## `onBandwidthUpdate`

带宽变动通知

```js
onBandwidthUpdate={ ({ 
    elapsedMs,  // 距上次通知的间隔市场(毫秒)
    bytes,      // 距上次通知, 新加载字节数
    bitrate,    // 带宽
}) => {
    
} }
```

## `onAudioFocusChanged`

在 `disableFocus` 未禁用的情况下；声音失去焦点、重新获得焦点时触发；比如音乐在后台播放，来电话了，会通知 focused=false，待电话结束，音乐重新播放，会通知 focused=true；当然，也有可能用户打开了其他音乐软件，那么就直接播放停止，不会再次自动获取焦点了，除非用户手动切换过来，重新播放。

```js
onAudioFocusChanged={ ({ 
    focused
}) => {
    
} }
```

## `onAudioBecomingNoisy`

音频输出设备发生变化，比如插上/拔出耳机，此时，如有必要，可暂停视频。

## `onError`

发生错误

```js
onError={ ({ 
    errorString,    // 友好提示
    errorException, // Native 端 exception message
}) => {
    
} }
```


# 轨道

有些视频格式，同一个视频，内部可能有多个 画面/音轨/字幕，另可通过 `textTracks` 拓展字幕；那么播放时就有了多种选择，默认情况下，画面会选择第一个轨道，音轨/字幕 会根据系统语言和这两个轨道的 `language` 尝试自动选择，无法匹配则使用第一个轨道。

可以通过 `selectedVideoTrack` / `selectedAudioTrack` / `selectedTextTrack` 明确指定要使用轨道。

首先，在 `onLoad` 回调函数中会通知当前可用的轨道（包含外挂字幕）

```js
onLoad={ ({ 
  duration,     // 视频时长, 直播可能小于0
  currentTime,  // 当前播放位置
  width,   //视频宽度
  height,  //视频高度
  videoTracks,  //可用画面轨道
  audioTracks,  //可用音轨
  textTracks,   //可用字幕
}) => {
    
} }
```

轨道格式为数组，三种轨道的字段不一样，但格式都是以下这种

```js
[
    {
        title:"",
        type:"xx",
        language:"",
        ...
    },
    ....
]
```

指定要使用的轨道，如

```js
selectedAudioTrack={{
    type: "index",
    value: 1,
}}
```

`type` 指定选用标准，`value` 为该选用标准下要使用的值。

  1. `type=index` : `value` 为数组下标
  2. `type` 支持使用字段名，`value` 则为字段值；这样可在明确知道轨道的前提下，无需等待 `onLoad`，在初始化时便可指定。

比如字幕

```js
textTracks=[
    {
        language:"zh",
        ...
    },
    {
        language:"en",
        ...
    },
    ....
]

// 可
selectedTextTrack={{
    type:"language",
    value:"zh"
}}
```
