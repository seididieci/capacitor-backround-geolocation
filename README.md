<h3 align="center">Capacitor Background Geolocation</h3>
<p align="center"><strong><code>capacitor-background-geolocation</code></strong></p>
<p align="center">
  Capacitor plugin for enabling bacground geolocation service
</p>

## Maintainers

| Maintainer      | GitHub
| --------------- | -------------------------------------------------------
| Damiano Brunori | [seididieci](https://github.com/seididieci)

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

| method            | info                                                     | platform    |
| ----------------- | -------------------------------------------------------- | ----------- |
| `initialize`      | initialize/start service and configure                   | web/android |
| `goForeground`    | bring the service on foreground (showing a notification) | android     |
| `stopForeground`  | bring the service back to the bacground                  | android     |


## Usage

```ts
import { Plugins } from '@capacitor/core';
const { BackgroundGeolocation } = Plugins;

import { BgLocationEvent, BgGeolocationAccuracy } from "capacitor-background-geolocation";

BackgroundGeolocation.initialize({
  notificationText: "Your app is running, tap to open.",
  notificationTitle: "App Running",
  updateInteval: 10000,
  requestedAccuracy: BgGeolocationAccuracy.HIGH_ACCURACY,
  // Small icon has to be in 'drawable' resources of your app
  // if you does not provide it or it is not found a fallback icon will be used.
  smallIcon: "ic_small_icon",
});

BackgroundGeolocation.addListener("onLocation", (location: BgLocationEvent) => {
  console.log("Got new location", location);
  // Put your logic here.
});

```

### Android

> ### Notice
>
> Remember to add this plugin to your app main acctivity:
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
