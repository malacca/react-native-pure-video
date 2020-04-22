import React from 'react';
import { 
  requireNativeComponent, 
  NativeModules, 
  Platform, 
  processColor,
} from 'react-native';
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource';

const IS_ANDROID = Platform.OS === 'android';
const TextTrackType = {
  SRT: 'application/x-subrip',
  TTML: 'application/ttml+xml',
  VTT: 'text/vtt',
};

// 1. source={requrie('./path')}
// 2. source={uri:requrie('./path'), type:"", ...}
// 3. source={uri:"http||file||content||asset", type:"", ...}
function makeVideoSrc(source) {
  let asset, extraProps;
  if (typeof source === 'number') {
    asset = source;
  } else {
    source = source || {};
    const {uri, ...leftProps} = source;
    if (typeof uri === 'number') {
      asset = uri;
      extraProps = leftProps;
    }  
  }
  if (asset) {
    source = resolveAssetSource(source) || {};
    if (extraProps) {
      source = {...extraProps, ...source};
    }
  }
  return resolveSourceSrc(source);
}
function resolveSourceSrc(source) {
  let uri = source.uri || '';
  if (uri && uri.match(/^\//)) {
    uri = `file://${uri}`;
  }
  const src = {
    uri,
    type: source.type || '',
    requestHeaders: stringsOnlyObject(source.headers),
    holdPosition: Boolean(source.holdPosition)
  };
  if (!IS_ANDROID) {
    src.isNetwork = !!(uri && uri.match(/^https?:/));
    src.isAsset = !!(uri && uri.match(/^(assets-library|ipod-library|file|content|ms-appx|ms-appdata):/));
    src.shouldCache = !source.__packager_asset;
    src.mainVer = source.mainVer || 0;
    src.patchVer = source.patchVer || 0;
  }
  return src;
}
function stringsOnlyObject(obj) {
  obj = obj||{};
  const strObj = {};
  for (let x in obj) {
    strObj[x] = toTypeString(obj[x]);
  }
  return strObj;
}
function toTypeString(x) {
  switch (typeof x) {
    case 'object':
      return x instanceof Date
        ? x.toISOString()
        : JSON.stringify(x); // object, null
    case 'undefined':
      return '';
    default: // boolean, number, string
      return x.toString();
  }
}

// resizeMode=""
const RESIZE_MODE = {
  "cover": 0,
  "contain": 1,
  "stretch": 2,
  "center": 3,
};
function getResizeMode(resizeMode) {
  return resizeMode && resizeMode in RESIZE_MODE ? RESIZE_MODE[resizeMode] : 0;
}

// textTrackStyle={}
const TextTrackEdgeType = {
  "none": 0,
  "outline": 1,
  "shadow": 2,
  "raised": 3,
  "depressed": 4,
};
const textTrackStyleColor = ['color', 'background', 'edgeColor'];
function getTextTrackStyle(style) {
  style = style||{};
  let key, value;
  const trackStyle = {};
  for (key in style) {
    value = style[key];
    if (key ==='edgeType') {
      trackStyle[key] = value in TextTrackEdgeType ? TextTrackEdgeType[value] : 0;
    } else if (key ==='paddingFraction' || key ==='sizeFraction') {
      trackStyle[key] = value / 100;
    } else if (key ==='disableScale' || key ==='ignorePadding') {
      trackStyle[key] = Boolean(value);
    } else if (textTrackStyleColor.includes(key)) {
      trackStyle[key] = processColor(value);
    }
  }
  return trackStyle;
}

// listeners={} (该属性自动生成)
const supportEvent = (() => {
  const manager = NativeModules.UIManager.getViewManagerConfig
    ? NativeModules.UIManager.getViewManagerConfig('RCTPureVideo')
    : NativeModules.UIManager.RCTPureVideo;
  return manager ? manager.Constants.events : [];
})();
function makeListeners(props) {
  const listeners = {};
  for (let k in props) {
    if (supportEvent.includes(k)) {
      listeners[k] = true;
    }
  }
  return listeners;
}

// format props
function formatProps(nativeProps){
  const {source, resizeMode, textTrackStyle, ...props} = nativeProps || {};
  if (source) {
    props.src = makeVideoSrc(source);
  }
  if (resizeMode) {
    props.resizeMode = getResizeMode(resizeMode);
  }
  if (textTrackStyle) {
    props.textTrackStyle = getTextTrackStyle(textTrackStyle);
  }
  return props;
}

class Video extends React.PureComponent {
  setNativeProps(props) {
    this.refs.video.setNativeProps(formatProps(props));
  }
  render() {
    const props = formatProps(this.props);
    props.listeners = makeListeners(props);
    props.ref = "video";
    return <RCTPureVideo {...props}/>;
  }
}
const RCTPureVideo = requireNativeComponent('RCTPureVideo', Video);

export {TextTrackType};
export default Video;