package com.getcapacitor.community.bglocation;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

@NativePlugin(
  permissions = {
    Manifest.permission.FOREGROUND_SERVICE,
    Manifest.permission.ACCESS_FINE_LOCATION,
  },
  permissionRequestCode = 86 // Used in checking for runtime permissions.
)
public class BackgroundGeolocation extends Plugin {

  private static final String TAG = "BackgroundGeolocation";

  // The BroadcastReceiver used to listen from broadcasts from the service.
  private GeolocationReceiver receiver;

  // Tracks the bound state of the service.
  private boolean mBound = false;
  private boolean initialized = false;
  private boolean foregroundPermission = false;
  private boolean locationPermission = false;
  private boolean startRequested = false;
  private boolean forceForeground = false;
  private boolean appInBackground = false;
  private boolean serviceRunning = false;

  // Monitors the state of the connection to the service.
  private final ServiceConnection mServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      LocalBroadcastManager
        .getInstance(getContext())
        .registerReceiver(
          receiver,
          new IntentFilter(LocationUpdatesService.ACTION_BROADCAST)
        );
      mBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      LocalBroadcastManager
        .getInstance(getContext())
        .unregisterReceiver(receiver);
    }
  };

  @Override
  protected void handleOnStop() {
    if (this.serviceRunning) {
      this.appInBackground = true;
      // When the applications goes background we go foreground to keep getting data
      Intent intent = new Intent(getContext(), LocationUpdatesService.class);
      intent.setAction(LocationUpdatesService.ACTION_GO_FOREGROUND);
      getContext().startService(intent);
    }
  }

  @Override
  protected void handleOnResume() {
    if (this.appInBackground) {
      this.appInBackground = false;

      if (!this.forceForeground) {
        // When the applications is resumed if foreground is not forced we put the service background again
        Intent intent = new Intent(getContext(), LocationUpdatesService.class);
        intent.setAction(LocationUpdatesService.ACTION_GO_BACKGROUND);
        getContext().startService(intent);
      }
    }
  }

  @Override
  protected void handleOnDestroy() {
    // UnBind to the service.
    getContext().unbindService(mServiceConnection);

    // When app gets destroyed we stop and kill the service.
    Intent intent = new Intent(getContext(), LocationUpdatesService.class);
    intent.putExtra("destroying", true);
    intent.setAction(LocationUpdatesService.ACTION_STOP);
    getContext().startService(intent);

  }

  @Override
  protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

    for(int i = 0; i < Math.min(permissions.length, grantResults.length); i++) {
      switch (permissions[i]) {

        case Manifest.permission.FOREGROUND_SERVICE:
          // No need for FOREGROUND_SERVICE permission before Android 9 (see https://developer.android.com/about/versions/pie/android-9.0-changes-28#fg-svc)
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || grantResults[i] == PackageManager.PERMISSION_GRANTED) {
            this.foregroundPermission = true;
          }
          break;

        case Manifest.permission.ACCESS_FINE_LOCATION:
          if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
            this.locationPermission = true;
          }
          break;
      }
    }

    // Let frontend know about permissions.
    JSObject ret = new JSObject();
    ret.put("foreground", this.foregroundPermission);
    ret.put("fineLocation", this.locationPermission);
    notifyListeners("onPermissions", ret);

    // If we have permissions we can start the service if requested and initialized.
    if (this.locationPermission && this.initialized && this.startRequested) {
      // Permission was granted.
      Log.d(TAG, "User granted permissions, starting service...");

      // Actual start
      this.serviceRunning = true;
      Intent intent = new Intent(getContext(), LocationUpdatesService.class);
      intent.setAction(LocationUpdatesService.ACTION_START);
      getContext().startService(intent);
    }
  }

  /**
   * Receiver for broadcasts sent by {@link LocationUpdatesService}.
   */
  private class GeolocationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      Location location = intent.getParcelableExtra(
        LocationUpdatesService.EXTRA_LOCATION
      );
      if (location != null) {
        JSObject ret = new JSObject();
        ret.put("latitude", location.getLatitude());
        ret.put("longitude", location.getLongitude());
        ret.put("locationAccuracy", location.getAccuracy());

        ret.put("altitude", location.getAltitude());
        ret.put("bearing", location.getBearing());
        ret.put("speed", location.getSpeed());

        ret.put("provider", location.getProvider());
        ret.put("time", location.getTime());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          ret.put("altitudeAccuracy", location.getVerticalAccuracyMeters());
          ret.put("bearingAccuracy", location.getBearingAccuracyDegrees());
          ret.put("speedAccuracy", location.getSpeedAccuracyMetersPerSecond());
        } else {
          ret.put("altitudeAccuracy", 0);
          ret.put("bearingAccuracy", 0);
          ret.put("speedAccuracy", 0);
        }

        notifyListeners("onLocation", ret);
      }
    }
  }

  @PluginMethod
  public void requestPermissions(PluginCall call) {
    // Ensure we have permissions
    pluginRequestAllPermissions();

    call.resolve();
  }

  @PluginMethod
  public void initialize(PluginCall call) {
    if (initialized) {
      call.success();
      return;
    }

    initialized = true;

    receiver = new GeolocationReceiver();

    // Setting this before requesting permissions
    if (call.hasOption("startImmediately"))
      this.startRequested = call.getBoolean("startImmediately");

    // Ensure we have permissions
    pluginRequestAllPermissions();

    // Configuring
    Intent configIntent = new Intent(getContext(), LocationUpdatesService.class);
    configIntent.setAction(LocationUpdatesService.ACTION_CONFIGURE);
    configIntent.putExtra("mainActivity", getBridge().getActivity().getClass().getCanonicalName());
    if (call.hasOption("notificationTitle"))
      configIntent.putExtra("notificationTitle", call.getString("notificationTitle"));
    if (call.hasOption("notificationText"))
      configIntent.putExtra("notificationText", call.getString("notificationText"));
    if (call.hasOption("updateInterval"))
      configIntent.putExtra("updateInterval", call.getInt("updateInterval"));
    if (call.hasOption("smallIcon"))
      configIntent.putExtra("smallIcon", getContext().getResources().getIdentifier(call.getString("smallIcon"), "drawable", getContext().getApplicationContext().getPackageName()));
    if (call.hasOption("requestedAccuracy"))
      configIntent.putExtra("requestedAccuracy", call.getInt("requestedAccuracy"));
    getContext().startService(configIntent);

    // Bind to the service.
    getContext()
      .bindService(
        new Intent(getContext(), LocationUpdatesService.class),
        mServiceConnection,
        Context.BIND_AUTO_CREATE
      );

    call.success();
  }

  @PluginMethod
  public void start(PluginCall call) {
    if (!initialized) {
      call.error("Plugin in not initialized, try to call initialize() first.");
      return;
    }

    // Plugin user is requesting to start location update service
    this.serviceRunning = true;
    Intent intent = new Intent(getContext(), LocationUpdatesService.class);
    intent.setAction(LocationUpdatesService.ACTION_START);
    getContext().startService(intent);

    // Resume sevice foreground state
    if (this.appInBackground || this.forceForeground) {
      Intent bgIntent = new Intent(getContext(), LocationUpdatesService.class);
      intent.setAction(LocationUpdatesService.ACTION_GO_FOREGROUND);
      getContext().startService(intent);
    }

    call.success();
  }

  @PluginMethod
  public void stop(PluginCall call) {
    // Plugin user is requesting to stop location update service
    this.serviceRunning = false;
    Intent intent = new Intent(getContext(), LocationUpdatesService.class);
    intent.setAction(LocationUpdatesService.ACTION_STOP);
    getContext().startService(intent);

    call.success();
  }

  @PluginMethod
  public void goForeground(PluginCall call) {
    if (!initialized) {
      call.error("Plugin in not initialized, try to call initialize() first.");
      return;
    }

    if (!this.foregroundPermission) {
      call.error("Cannot start a foreground Service: user denied FOREGROUND permissions.");
      return;
    }

    if (!this.serviceRunning) {
      call.error("Cannot send service to foreground: it is not started. Try to call start() before.");
      return;
    }

    this.forceForeground = true;

    // Plugin user is forcing foreground mode
    Intent intent = new Intent(getContext(), LocationUpdatesService.class);
    intent.setAction(LocationUpdatesService.ACTION_GO_FOREGROUND);
    getContext().startService(intent);

    call.success();
  }

  @PluginMethod
  public void stopForeground(PluginCall call) {
    if (!initialized) {
      call.error("Plugin in not initialized, try to call initialize() first.");
      return;
    }

    if (!this.serviceRunning) {
      call.success();
      return;
    }

    this.forceForeground = false;

    // Plugin user forcing exiting foreground mode
    Intent intent = new Intent(getContext(), LocationUpdatesService.class);
    intent.setAction(LocationUpdatesService.ACTION_GO_BACKGROUND);
    getContext().startService(intent);

    call.success();
  }
}
