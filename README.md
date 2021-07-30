<h3 align="center">Capacitor Background Geolocation</h3>
<p align="center"><strong><code>capacitor-background-geolocation</code></strong></p>
<p align="center">
  Capacitor plugin for enabling background geolocation service
</p>

[![npm version](https://badge.fury.io/js/capacitor-background-geolocation.svg)](https://badge.fury.io/js/capacitor-background-geolocation)
![NPM Publish](https://github.com/seididieci/capacitor-backround-geolocation/workflows/NPM%20Publish/badge.svg)

## Maintainers

| Maintainer      | GitHub                                      |
| --------------- | ------------------------------------------- |
| Damiano Brunori | [seididieci](https://github.com/seididieci) |

## Notice 🚀

This plugin actually works only in android. It creates a foreground service (with a notification on android) to keep your app getting location updates.

## Installation

Using npm:

```bash
npm install capacitor-background-geolocation
```

Sync native files:

```bash
npx cap sync
```

## API

| method           | info                                                     | platform    |
| ---------------- | -------------------------------------------------------- | ----------- |
| `initialize`     | initialize/start service and configure                   | web/android |
| `start`          | starts the service getting location updates              | web/android |
| `stop`           | stops the service getting location updates               | web/android |
| `goForeground`   | bring the service on foreground (showing a notification) | android     |
| `stopForeground` | bring the service back to the bacground                  | android     |

## Usage steps (TypeScript)

### Import plugin and types

```ts
import { Plugins } from '@capacitor/core';
const { BackgroundGeolocation } = Plugins;

import {
  BgLocationEvent,
  BgGeolocationAccuracy,
} from 'capacitor-background-geolocation';
```

### Add listner(s) for location updates

```ts
BackgroundGeolocation.addListener('onLocation', (location: BgLocationEvent) => {
  console.log('Got new location', location);
  // Put your logic here.
});
```

### Start service after the user accept permissions (through Android popup)
```ts
BackgroundGeolocation.addListener('onPermissions', (data: BgPermissions) => {
  console.log('BGLocation permissions:', location);

  // Start geolocation if user granted location permisiions
  if (data.fineLocation)
    BackgroundGeolocation.start();
});
```

### Initialize the plugin settings (required)

```ts
BackgroundGeolocation.initialize({
  notificationText: 'Your app is running, tap to open.',
  notificationTitle: 'App Running',
  updateInteval: 10000,
  requestedAccuracy: BgGeolocationAccuracy.HIGH_ACCURACY,
  // Small icon has to be in 'drawable' resources of your app
  // if you does not provide one (or it is not found) a fallback icon will be used.
  smallIcon: 'ic_small_icon',
  // Start getting location updates right away. You can set this to false or not set at all (se below).
  startImmediately: true,
});
```
### Request permissions to user
```ts
// After user accept permissions the handler above will start the service
BackgroundGeolocation.requestPermissions();
```

### Evnetually stop getting location updates when done
```ts
BackgroundGeolocation.stop();
```

### Control background/foreground behaviour
```ts
// Force the service to run in foreground
// It will show the android notification also when your app is up and running
BackgroundGeolocation.goFroreground();

// Restore the service to run in backgroud/default mode: the android notification will be shown only when your app goes in background.
BackgroundGeolocation.stopForeground();

```
Keep in mind that the plugin will send the service in the foreground when your APP is going into background until you stop the service or quit the APP.

### Android

> ### Notice
>
> Remember to add this plugin to your app main acctivity:
>
> ```java
> import com.getcapacitor.community.bglocation.BackgroundGeolocation;
>
> //....
>
> // Initializes the Bridge
> this.init(savedInstanceState, new ArrayList<Class<? extends Plugin>>() {{
>   // Additional plugins you've installed go here
>
>   // <Your other plugins if any>
>
>   add(BackgroundGeolocation.class);
> }});
> ```
>The `AndroidManifest.xml` should be automaticcaly filled with the right permissions through a `cap sync` command but sometimes it doesn't...<br/>
>Check that there are `FOREGROUND_SERVICE` and `ACCESS_FINE_LOCATION` permissions. in your App Manifest.

### iOS

This plugin is not yet implemented iOS side, if someone wants to help I will appreciate it...

## License

capacitor-background-geolocation is 100% free and open-source, under the [MIT license](LICENSE). Use it however you want.
