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
  /**
   * Sets the title for Foreground service notification
   *
   * @type {string}
   * @memberof BgGeolocationOptions
   */
  notificationTitle?: string;
  /**
   * Sets the text for the
   *
   * @type {string}
   * @memberof BgGeolocationOptions
   */
  notificationText?: string;
  /**
   * Sets the requested interval for location updates.
   *
   * @type {number}
   * @memberof BgGeolocationOptions
   */
  updateInteval: number;
  /**
   * Sets the small icon for the Foregroud service notification.
   * The icon name you set must be in 'drawable' resources of your app
   * if you does not provide one (or it is not found) a fallback icon will be used.
   * @type {string}
   * @memberof BgGeolocationOptions
   */
  smallIcon?: string;
  /**
   * Sets the requested accuracy for geolocation position.
   *
   * @type {BgGeolocationAccuracy}
   * @memberof BgGeolocationOptions
   */
  requestedAccuracy: BgGeolocationAccuracy;
  /**
   * Start immediateli to get locations (you should register the handler before callig initialize)
   *
   * @type {boolean}
   * @memberof BgGeolocationOptions
   */
  startImmediately?: boolean;
}

export interface BackgroundGeolocationPlugin {
  initialize(options: BgGeolocationOptions): Promise<void>;
  goForeground(): Promise<void>;
  stopForeground(): Promise<void>;
  addListener(eventName: string, listenerFunc: ListenerCallback): PluginListenerHandle;
  start(): Promise<void>;
  stop(): Promise<void>;
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

export interface BgPermissions {
  foreground: boolean;
  fineLocation: boolean;
}
