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
  permissionRequestCode = 0xFFAABB // Used in checking for runtime permissions.
)
public class BackgroundGeolocation extends Plugin {

  private static final String TAG = "BackgroundGeolocation";

  // The BroadcastReceiver used to listen from broadcasts from the service.
  private GeolocationReceiver receiver;

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
    super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

    for(int result : grantResults) {
      if (result == PackageManager.PERMISSION_DENIED) {
        return;
      }
    }

    // Permission was granted.
    Log.d(TAG, "User granted permissions, starting service...");

    // Actual start
    Intent intent = new Intent(getContext(), LocationUpdatesService.class);
    intent.setAction(LocationUpdatesService.ACTION_START);
    getContext().startService(intent);

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
