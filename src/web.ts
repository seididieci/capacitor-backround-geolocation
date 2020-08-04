import { WebPlugin, ListenerCallback, PluginListenerHandle } from '@capacitor/core';
import { BackgroundGeolocationPlugin, BgGeolocationOptions, BgLocationEvent, BgGeolocationAccuracy } from './definitions';

export class BackgroundGeolocationWeb extends WebPlugin implements BackgroundGeolocationPlugin {
  constructor() {
    super({
      name: 'BackgroundGeolocation',
      platforms: ['web'],
    });
  }

  private updateInterval: number = 10000;
  private requestedAccuracy: BgGeolocationAccuracy = BgGeolocationAccuracy.HIGH_ACCURACY;

  async initialize(options: BgGeolocationOptions): Promise<void> {
    // Nothing to do on web
    this.updateInterval = options.updateInteval;
    this.requestedAccuracy = options.requestedAccuracy;
  }

  async goForeground(): Promise<void> {
    // Nothing to do on web
  }

  async stopForeground(): Promise<void> {
    // Nothing to do on web
  }

  addListener(eventName: string, listenerFunc: ListenerCallback): PluginListenerHandle {
    if (eventName.localeCompare('onLocation') === 0) {
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
      }, {
        enableHighAccuracy: this.requestedAccuracy === BgGeolocationAccuracy.BALANCED_POWER_ACCURACY ||
                            this.requestedAccuracy === BgGeolocationAccuracy.HIGH_ACCURACY,
        timeout: this.updateInterval,
        maximumAge: 0,
      });
      return {
        remove: () => {
          window.navigator.geolocation.clearWatch(watchId);
        }
      };
    }

    return {
      remove: () => { }
    };
  }

}

const BackgroundGeolocation = new BackgroundGeolocationWeb();

export { BackgroundGeolocation };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(BackgroundGeolocation);
