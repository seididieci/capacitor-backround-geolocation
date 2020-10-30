import { ListenerCallback, PluginListenerHandle } from "@capacitor/core";

declare module '@capacitor/core' {
  interface PluginRegistry {
    BackgroundGeolocation: BackgroundGeolocationPlugin;
  }
}

export enum BgGeolocationAccuracy {
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
  updateInterval: number;
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
  /**
   * Plugin initialization. This is required before you can start receiving location data.
   *
   * @param {BgGeolocationOptions} options
   * @returns {Promise<void>}
   * @memberof BackgroundGeolocationPlugin
   */
  initialize(options: BgGeolocationOptions): Promise<void>;
  /**
   * Make the service run in foreground (starts a Notification on android).
   * This is automatically done when your APP goes on background so it will
   * not stop getting location updates
   *
   * @returns {Promise<void>}
   * @memberof BackgroundGeolocationPlugin
   */
  goForeground(): Promise<void>;
  /**
   * Make the service stop working in foreground.
   * If your APP is running background will stop getting regular location updates
   *
   * @returns {Promise<void>}
   * @memberof BackgroundGeolocationPlugin
   */
  stopForeground(): Promise<void>;
  /**
   * Adds a listner to Plugin events
   *
   * @param {string} eventName
   * @param {ListenerCallback} listenerFunc
   * @returns {PluginListenerHandle}
   * @memberof BackgroundGeolocationPlugin
   */
  addListener(eventName: string, listenerFunc: ListenerCallback): PluginListenerHandle;
  /**
   * Starts the service getting location updates.
   *
   * @returns {Promise<void>}
   * @memberof BackgroundGeolocationPlugin
   */
  start(): Promise<void>;
  /**
   * Stops the service to get location updates. If the service is running forregroud,
   * maybe you will keep seeing the notification until you call stopForeground()
   *
   * @returns {Promise<void>}
   * @memberof BackgroundGeolocationPlugin
   */
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
