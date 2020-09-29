import { WebPlugin, ListenerCallback, PluginListenerHandle } from '@capacitor/core';
import { BackgroundGeolocationPlugin, BgGeolocationOptions, BgLocationEvent, BgGeolocationAccuracy, BgPermissions } from './definitions';

export class BackgroundGeolocationWeb extends WebPlugin implements BackgroundGeolocationPlugin {
  constructor() {
    super({
      name: 'BackgroundGeolocation',
      platforms: ['web'],
    });
  }

  private updateInterval: number = 10000;
  private requestedAccuracy: BgGeolocationAccuracy = BgGeolocationAccuracy.HIGH_ACCURACY;
  private get geoOpts() {
    return {
      enableHighAccuracy: this.requestedAccuracy === BgGeolocationAccuracy.BALANCED_POWER_ACCURACY ||
        this.requestedAccuracy === BgGeolocationAccuracy.HIGH_ACCURACY,
      timeout: this.updateInterval,
      maximumAge: 0,
    };
  }

  initialize(options: BgGeolocationOptions): Promise<void> {
    // Nothing to do on web
    this.updateInterval = options.updateInteval;
    this.requestedAccuracy = options.requestedAccuracy;
    return Promise.resolve();
  }

  goForeground(): Promise<void> {
    // Nothing to do on web
    return Promise.resolve();
  }

  stopForeground(): Promise<void> {
    // Nothing to do on web
    return Promise.resolve();
  }

  addListener(eventName: string, listenerFunc: ListenerCallback): PluginListenerHandle {

    if (eventName.localeCompare('onLocation') === 0) {
      // This is a violation of "Only request geolocation information in response to a user gesture." if listner is added out of a user interaction event
      // But on Native plugin this is not necessary...
      const watchId = window.navigator.geolocation.watchPosition((pos) => {
        listenerFunc({
          altitude: pos.coords.altitude,
          altitudeAccuracy: pos.coords.altitudeAccuracy,
          bearing: 0,
          bearingAccuracy: 0,
          latitude: pos.coords.latitude,
          locationAccuracy: pos.coords.accuracy,
          longitude: pos.coords.longitude,
          provider: "navigator",
          speed: pos.coords.speed,
          speedAccuracy: 0,
          time: pos.timestamp
        } as BgLocationEvent);
      }, (err) => {
        console.warn(err);
      }, this.geoOpts);
      return {
        remove: () => {
          window.navigator.geolocation.clearWatch(watchId);
        }
      };
    }

    if (eventName.localeCompare('onPermissions') === 0) {
      let permissions: PermissionStatus | null = null;
      // Check for Geolocation Permissions (web does not need foreground service one)
      navigator.permissions.query({ name: 'geolocation' }).then((result) => {
        permissions = result;

        const notify = this.parsePermissonState(permissions.state);
        if (notify)
          listenerFunc(notify);

        result.onchange = () => {
          const notify = this.parsePermissonState(result.state);
          if (notify)
            listenerFunc(notify);
        };
      });

      // Remove status change notification
      return {
        remove: () => {
          if (permissions)
            permissions.onchange = null;
        }
      };
    }

    return {
      remove: () => { }
    };
  }

  private parsePermissonState(state: PermissionState): BgPermissions | null {

    switch (state) {
      case "denied":
        return { foreground: false, fineLocation: false } as BgPermissions;
      case "granted":
        return { foreground: false, fineLocation: true } as BgPermissions;
      case "prompt":
        // No Permission has been requested yet
        return null;
    }
  }

}

const BackgroundGeolocation = new BackgroundGeolocationWeb();

export { BackgroundGeolocation };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(BackgroundGeolocation);
