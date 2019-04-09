import React, { PureComponent, Component } from "react";
import ReactNative, {
  Platform,
  DeviceEventEmitter,
  NativeModules,
  requireNativeComponent,
  UIManager,
  View,
  ViewPropTypes
} from "react-native";
import PropTypes from "prop-types";

const NativeGoogleCastV3 = NativeModules.GoogleCastV3;

const GoogleCastV3Handler = {
  ...NativeGoogleCastV3,

  send: (a, b) => {
    b === undefined
      ? NativeGoogleCastV3.send(NativeGoogleCastV3.namespace, a)
      : NativeGoogleCastV3.send(a, b);
  },
  addCastStateListener: fn => {
    const subscription = DeviceEventEmitter.addListener(
      "googleCastStateChanged",
      fn
    );
    return subscription;
  },
  removeCastStateListener: fn => {
    return DeviceEventEmitter.removeListener("googleCastStateChanged", fn);
  },
  addCastMessageListener: fn =>
    DeviceEventEmitter.addListener("googleCastMessage", fn),
  removeCastMessageListener: fn =>
    DeviceEventEmitter.removeListener("googleCastMessage", fn),
  addCastProgressListener: fn => {
    NativeGoogleCastV3.addCastProgressListener();
    return DeviceEventEmitter.addListener("castProgress", fn);
  },
  removeCastProgressListener: fn => {
    NativeGoogleCastV3.removeCastProgressListener();
    return DeviceEventEmitter.removeListener("castProgress", fn);
  },
  load: video => {
    return NativeGoogleCastV3.load(
      video.url,
      video.title,
      video.subtitle,
      video.image,
      video.duration,
      video.customData
    );
  },
  startScan: fn => {
    NativeGoogleCastV3.startScan();
    return DeviceEventEmitter.addListener("mediaRouteChange", fn);
  },
  stopScan: fn => {
    NativeGoogleCastV3.stopScan();
    return DeviceEventEmitter.removeListener("mediaRouteChange", fn);
  },
  connectToDevice: deviceId => {
    NativeGoogleCastV3.connectToDevice(deviceId);
  },
  togglePlayPause: () => {
    NativeGoogleCastV3.togglePlayPause();
  },
  getQueueItemByIndex: index => NativeGoogleCastV3.getQueueItemByIndex(index),
  queueNext: () => {
    NativeGoogleCastV3.queueNext();
  },
  queuePrevious: () => {
    NativeGoogleCastV3.queuePrevious();
  },
  getCurrentCastState: () => NativeGoogleCastV3.getCurrentCastState(),
  getCurrentCastPlayerState: () =>
    NativeGoogleCastV3.getCurrentCastPlayerState(),
  seek: time => {
    NativeGoogleCastV3.seek(time);
  },
  triggerUpdateCurrentState: () => {
    NativeGoogleCastV3.triggerUpdateCurrentState();
  }
};

const stub = {
  NO_DEVICES_AVAILABLE: 1,
  NOT_CONNECTED: 2,
  CONNECTING: 3,
  CONNECTED: 4,
  send: () => {},
  addCastStateListener: fn => {
    fn(stub.NO_DEVICES_AVAILABLE);
  },
  addCastMessageListener: () => {},
  getCurrentDevice: () => Promise.resolve(null),
  load: video => {}
};

export const GoogleCastV3 =
  Platform.OS === "android" ? GoogleCastV3Handler : stub;

class CastButton extends Component {
  static propTypes = {
    ...ViewPropTypes,
    color: PropTypes.string
  };

  click = () => {
    UIManager.dispatchViewManagerCommand(
      ReactNative.findNodeHandle(this),
      UIManager.CastButton.Commands.click,
      []
    );
  };

  render() {
    return <NativeCastButton {...this.props} />;
  }
}

const NativeCastButton = requireNativeComponent("CastButton", CastButton);

export default (Platform.OS === "android" ? CastButton : View);
