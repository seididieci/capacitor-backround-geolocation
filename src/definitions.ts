import { ListenerCallback, PluginListenerHandle } from "@capacitor/core";

declare module '@capacitor/core' {
  interface PluginRegistry {
    BackgroundGeolocation: BackgroundGeolocationPlugin;
  }
}

export const enum BgGeolocationAccuracy {
  HIGH_ACCURACY = 100,
  BALANCED_POWER_ACCURACY = 102,
  LOW_POWER = 104,
  NO_POWER = 105,
}

export interface BgGeolocationOptions {
  notificationTitle: string;
  notificationText: string;
  updateInteval: number;
  smallIcon: string;
  requestedAccuracy: BgGeolocationAccuracy;
}

export interface BackgroundGeolocationPlugin {
  initialize(options: BgGeolocationOptions): Promise<void>;
  goForeground(): Promise<void>;
  stopForeground(): Promise<void>;
  addListener(eventName: string, listenerFunc: ListenerCallback): PluginListenerHandle;
}


export interface BgLocationEvent {

  latitude: number;
  longitude: number;
  locationAccuracy: number;

  altitude: number;
  altitudeAccuracy: number;

  bearing: number;
  bearingAccuracy: number;

  speed: number;
  speedAccuracy: number;

  time: number;
  provider: string;
}
