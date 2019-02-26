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
    NativeGoogleCastV3.triggerStateChange();
    return subscription;
  },
  addCastMessageListener: fn =>
    DeviceEventEmitter.addListener("googleCastMessage", fn),
  load: video => {
    return NativeGoogleCastV3.load(
      video.url,
      video.title,
      video.subtitle,
      video.image,
      video.duration
    );
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
