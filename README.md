<h3 align="center">Capacitor Background Geolocation</h3>
<p align="center"><strong><code>capacitor-background-geolocation</code></strong></p>
<p align="center">
  Capacitor plugin for enabling bacground geolocation service
</p>

[![npm version](https://badge.fury.io/js/capacitor-background-geolocation.svg)](https://badge.fury.io/js/capacitor-background-geolocation)

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
| `goForeground`   | bring the service on foreground (showing a notification) | android     |
| `stopForeground` | bring the service back to the bacground                  | android     |

## Usage steps (TypeScript)

### Import plugin an types

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

### Configure plugin settings (required)

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

### Getting notified about permission results
```ts
BackgroundGeolocation.addListener('onPermissions', (data: BgPermissions) => {
  console.log('BGLocation permissions:', location);

  // Do something with data
});
```

### Start and stopp getting location updates
```ts
// Start getting location updates
BackgroundGeolocation.start();

// Stop getting location updates
BackgroundGeolocation.stop();

```

### Control background/foreground behaviour
```ts
// Force the service to run in foreground
// It will show the android icon also when your app is up and running
BackgroundGeolocation.goFroreground();

// Restore the service to run in backgroud/default mode: the android icon will be shown when your app is in background.
BackgroundGeolocation.stopForeground();

```
Keep in mind that the plugin will send the service in the foreground when your APP is going into bacground until you stop the service or quit the APP.

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

### iOS

This plugin is not yet implemented iOS side, if you want to help I will appreciate it...

## License

MIT

```

```
