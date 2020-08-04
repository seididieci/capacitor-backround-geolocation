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
  permissions={
    Manifest.permission.FOREGROUND_SERVICE,
    Manifest.permission.INSTANT_APP_FOREGROUND_SERVICE,
    Manifest.permission.ACCESS_FINE_LOCATION,
  }
)
public class BackgroundGeolocation extends Plugin {
  // Used in checking for runtime permissions.
  private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
  private static final String TAG = "BackgroundGeolocation";

  // The BroadcastReceiver used to listen from broadcasts from the service.
  private GeolocationReceiver receiver;

  // A reference to the service used to get location updates.
  private LocationUpdatesService mService = null;

  // Tracks the bound state of the service.
  private boolean mBound = false;
  private boolean initialized = false;

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
    // When the applications goes background we go foreground to keep getting data
    Intent intent = new Intent(getContext(), LocationUpdatesService.class);
    intent.setAction(LocationUpdatesService.ACTION_GO_FOREGROUND);
    getContext().startService(intent);
  }

  @Override
  protected void handleOnDestroy() {
    // UnBind to the service.
    getContext().unbindService(mServiceConnection);

    // When app gets destroyed we stop the service.
    Intent intent = new Intent(getContext(), LocationUpdatesService.class);
    intent.setAction(LocationUpdatesService.ACTION_STOP);
    getContext().startService(intent);

  }

  @Override
  protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.handleRequestPermissionsResult(
      requestCode,
      permissions,
      grantResults
    );

    Log.i(TAG, "onRequestPermissionResult");

    PluginCall savedCall = getSavedCall();
    if (savedCall == null) {
      Log.e(TAG, "No stored plugin call for permissions request result");
      return;
    }

    if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
      if (grantResults.length <= 0) {
        // If user interaction was interrupted, the permission request is cancelled and you
        // receive empty arrays.
        Log.i(TAG, "User interaction was cancelled.");
      } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        // Permission was granted.
        mService.requestLocationUpdates();
      } else {
        // Permission denied.
        savedCall.error("User denied permission");
        return;
      }
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
  public void initialize(PluginCall call) {
    if (initialized) {
      call.success();
      return;
    }

    receiver = new GeolocationReceiver();

    // Check that the user hasn't revoked permissions by going to Settings.
    if (Utils.isRequestingLocation(getContext())) {
      if (!hasDefinedPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
        pluginRequestPermission(
          Manifest.permission.ACCESS_FINE_LOCATION,
          REQUEST_PERMISSIONS_REQUEST_CODE
        );
      }
    }

    // Configuring
    Intent configIntent = new Intent(getContext(), LocationUpdatesService.class);
    configIntent.setAction(LocationUpdatesService.ACTION_CONFIGURE);
    if (call.hasOption("notificationTitle"))
      configIntent.putExtra("notificationTitle", call.getString("notificationTitle"));
    if (call.hasOption("notificationText"))
      configIntent.putExtra("notificationText", call.getString("notificationText"));
    if (call.hasOption("updateInterval"))
      configIntent.putExtra("updateInterval", call.getInt("updateInterval"));
    if (call.hasOption("smallIcon"))
      configIntent.putExtra("smallIcon", getContext().getResources().getIdentifier(call.getString("smallIcon"), "drawable", getContext().getApplicationContext().getPackageName()));
    getContext().startService(configIntent);

    // Actual start
    Intent intent = new Intent(getContext(), LocationUpdatesService.class);
    intent.setAction(LocationUpdatesService.ACTION_START);
    intent.putExtra("mainActivity", getBridge().getActivity().getClass().getCanonicalName());

    // Starting the service
    getContext().startService(intent);

    // Bind to the service. If the service is in foreground mode, this signals to the service
    // that since this activity is in the foreground, the service can exit foreground mode.
    getContext()
      .bindService(
        new Intent(getContext(), LocationUpdatesService.class),
        mServiceConnection,
        Context.BIND_AUTO_CREATE
      );

    initialized = true;

    call.success();
  }

  @PluginMethod
  public void goForeground(PluginCall call) {
    if (!initialized) {
      call.error("Plugin in not initialized, call init first!");
      return;
    }

    // Plugin user is forcing foreground mode
    Intent intent = new Intent(getContext(), LocationUpdatesService.class);
    intent.setAction(LocationUpdatesService.ACTION_GO_FOREGROUND);
    getContext().startService(intent);

    call.success();
  }

  @PluginMethod
  public void stopForeground(PluginCall call) {
    if (!initialized) {
      call.error("Plugin in not initialized, call init first!");
      return;
    }

    // Plugin user forcing exiting foreground mode
    Intent intent = new Intent(getContext(), LocationUpdatesService.class);
    intent.setAction(LocationUpdatesService.ACTION_GO_BACKGROUND);
    getContext().startService(intent);

    call.success();
  }
}
