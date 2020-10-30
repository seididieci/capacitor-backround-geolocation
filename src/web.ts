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
  private watchListners: ListenerCallback[] = [];
  private watchId: number = -1;
  private initialized: boolean = false;

  private get geoOpts() {
    return {
      enableHighAccuracy: this.requestedAccuracy === BgGeolocationAccuracy.BALANCED_POWER_ACCURACY ||
        this.requestedAccuracy === BgGeolocationAccuracy.HIGH_ACCURACY,
      timeout: this.updateInterval,
      maximumAge: 0,
    };
  }

  public initialize(options: BgGeolocationOptions): Promise<void> {
    if (this.initialized)
      return Promise.resolve();

    this.initialized = true;

    // Nothing to do on web
    this.updateInterval = options.updateInterval;
    this.requestedAccuracy = options.requestedAccuracy;

    if (options.startImmediately)
      this.start();

    return Promise.resolve();
  }

  public start(): Promise<void> {
    // This may throw a violation of "Only request geolocation information in response to a user gesture" if start is called out of an user interaction event
    // On Native platform this is not the case...
    this.watchId = window.navigator.geolocation.watchPosition((pos) => {
      for (const listenerFunc of this.watchListners) {
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
      }
    }, (err) => {
      console.warn(err);
    }, this.geoOpts);

    return Promise.resolve();
  }

  public stop(): Promise<void> {
    window.navigator.geolocation.clearWatch(this.watchId);
    return Promise.resolve();
  }

  public goForeground(): Promise<void> {
    // Nothing to do on web
    return Promise.resolve();
  }

  public stopForeground(): Promise<void> {
    // Nothing to do on web
    return Promise.resolve();
  }

  public addListener(eventName: string, listenerFunc: ListenerCallback): PluginListenerHandle {

    if (eventName.localeCompare('onLocation') === 0) {

      this.watchListners.push(listenerFunc);

      return {
        remove: () => {
          this.watchListners.splice(this.watchListners.indexOf(listenerFunc), 1);
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
