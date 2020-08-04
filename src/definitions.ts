declare module '@capacitor/core' {
  interface PluginRegistry {
    BackgroundGeolocation: BackgroundGeolocationPlugin;
  }
}

export interface BgGeolocationOptions {
  notificationTitle: string;
  notificationText: string;
  updateInteval : number;
}

export interface BackgroundGeolocationPlugin {
  initialize(options: BgGeolocationOptions): Promise<void>;
  goForeground(): Promise<void>;
  stopForeground(): Promise<void>;
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
