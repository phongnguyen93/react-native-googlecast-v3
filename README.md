Google Cast SDK v3 support for React-Native

## CAVEATS!
* Only Android at the moment, iOS to come.
* Only supports talking to your custom receiver thru a custom channel at the moment, so the default use-case of playing some media is currently **NOT** supported. Sorry, but this is what I needed today.

# Installation

    npm install --save vp-google-cast
    react-native link

Or, you know, the same in yarn if that's what we are all using this week...

## Configuration for Android

Add required dependencies to `./android/app/build.gradle`:

    dependencies {
      implementation project(':vp-google-cast')
      implementation "com.android.support:appcompat-v7:23.0.1"
      implementation 'com.android.support:mediarouter-v7:23.0.1'
      implementation "com.google.android.gms:play-services-cast-framework:11.8.0"
      ... // And so on
    }

Make sure the version of the appcompat and mediarouter dependencies matches your `compileSdkVersion`.

Add the following inside the `<application>`-element in `./android/app/src/main/AndroidManifest.xml`:

    <meta-data
      android:name="com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME"
      android:value="com.reactnativegooglecastv3.CastOptionsProvider" />
    <meta-data
      android:name="com.reactnativegooglecastv3.castAppId"
      android:value="YOUR_APP_ID" />
    <meta-data
      android:name="com.reactnativegooglecastv3.castNamespace"
      android:value="urn:x-cast:your.own.namespace" />

Update your `./android/app/src/main/java/your.package/MainApplication.java`,
override onCreate, and add CastManager initialization:

      @Override
      public void onCreate() {
        super.onCreate();
        com.reactnativegooglecastv3.CastManager.init(this);
      }

# Usage

#### Import

    import CastButton, { GoogleCastV3 } from 'vp-google-cast'

    GoogleCastV3.appId // is your castAppId
    GoogleCastV3.namespace // is your castNamespace

#### Render the Cast button

The CastButton will appear and disappear depending on cast device availability, and show the current connection status:

    <View>
      <CastButton color="#f00 (optional)" />
    </View>

To trigger showing the device modal from another component, save a reference to the CastButton, and call `click()` on it:

    // render:
    <CastButton ref={c => this.myCastButton = c } ... />

    // elsewhere in your component
    this.myCastButton.click()

#### Get information about the currently connected device

     GoogleCastV3.getCurrentDevice().then(device => {
        // device: null || { id, model, name, version }
     })

#### Send messages to a connected Cast device

Using the namespace declared in you AndroidManifest.xml:

    GoogleCastV3.send('WHADDUP')

Using some other namespace:

    GoogleCastV3.send('urn:x-cast:some.other.namespace', 'WHADDUP')

#### Listen to things

    GoogleCastV3.addCastStateListener(state => {
      // state is one of: GoogleCastV3.{NO_DEVICES_AVAILABLE, NOT_CONNECTED, CONNECTING or CONNECTED}
    })

    GoogleCastV3.addCastMessageListener(message => {
      // message is: { namespace: 'urn:x-cast:your.own.namespace', message: String }
    })

#### Load media

    GoogleCastV3.load({
            url:      PropType.string,
            title:    PropType.string,
            image:    PropType.string,
            duration: PropType.number
          })
            .then(result => {
              console.log("LOAD_RESULT", result);
            })
            .catch(error => {
              console.log("LOAD_ERROR", error);
            })

