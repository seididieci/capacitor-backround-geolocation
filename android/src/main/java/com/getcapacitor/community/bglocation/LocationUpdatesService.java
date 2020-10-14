package com.getcapacitor.community.bglocation;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.getcapacitor.community.bglocation.capacitorbackgroundgeolocation.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class LocationUpdatesService extends Service {
  private static final String PACKAGE_NAME = "com.getcapacitor.community.bglocation";
  private static final String TAG = LocationUpdatesService.class.getSimpleName();
  private static final String CHANNEL_ID = "bg_location_channel";
  private static final int NOTIFICATION_ID = 0xFEDEC;

  static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
  static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";
  static final String ACTION_START = PACKAGE_NAME + ".startservice";
  static final String ACTION_STOP = PACKAGE_NAME + ".stopservice";
  static final String ACTION_CONFIGURE = PACKAGE_NAME + ".configureservice";
  static final String ACTION_GO_FOREGROUND = PACKAGE_NAME + ".goforeground";
  static final String ACTION_GO_BACKGROUND = PACKAGE_NAME + ".gobackground";

  private final IBinder mBinder = new LocalBinder();

  private NotificationManager mNotificationManager;
  private LocationRequest mLocationRequest;
  private FusedLocationProviderClient mFusedLocationClient;
  private LocationCallback mLocationCallback;
  private Handler mServiceHandler;
  private Location mLocation;

  // Configuration values
  private int updateInterval = 10000;
  private String notificationTitle;
  private String notificationText;
  private String mainActivityName;
  private int smallIconResourceID = R.drawable.ic_baseline_location_on_24;
  private int requestedAccuracy = LocationRequest.PRIORITY_HIGH_ACCURACY;

  public LocationUpdatesService() {
    notificationTitle = "App is running.";
    notificationText = "Application is getting your position. Tap this notification to open.";
  }

  @Override
  public void onCreate() {
    mFusedLocationClient =
      LocationServices.getFusedLocationProviderClient(this);

    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);
        onNewLocation(locationResult.getLastLocation());
      }
    };

    HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    mServiceHandler = new Handler(handlerThread.getLooper());
    mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    // Android O requires a Notification Channel.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      CharSequence name = getString(R.string.channel_name);
      // Create the channel for the notification
      NotificationChannel mChannel = new NotificationChannel(
        CHANNEL_ID,
        name,
        NotificationManager.IMPORTANCE_DEFAULT
      );

      // Set the Notification Channel for the Notification Manager.
      mNotificationManager.createNotificationChannel(mChannel);
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null) {
      String act = intent.getStringExtra("mainActivity");
      if (act != null) {
        this.mainActivityName = act;
      }
      if (intent.getAction() != null) {
        switch (intent.getAction()) {
          case ACTION_START:
            Log.d(TAG, "Location service started.");
            createLocationRequest();
            requestLocationUpdates();
            break;
          case ACTION_GO_FOREGROUND:
            if (Utils.isRequestingLocation(this)) {
              if (!serviceIsRunningInForeground(this)) {
                Log.d(TAG, "Location service going foreground.");
                startForeground(NOTIFICATION_ID, getNotification());
              }
            }
            break;
          case ACTION_GO_BACKGROUND:
            Log.d(TAG, "Location service going background.");
            stopForeground(true);
            break;
          case ACTION_STOP:
            Log.d(TAG, "Location service stopped.");
            stopForeground(true);
            if (Utils.isRequestingLocation(this)) {
              removeLocationUpdates();
            }
            boolean destroying = intent.getBooleanExtra("destroying", false);
            if (destroying) {
              stopSelf();
            }
            break;
          case ACTION_CONFIGURE:
            Log.d(TAG, "Location service update changed.");

            // Getting intent data
            int newInterval = intent.getIntExtra("updateInterval", updateInterval);
            String newTitle = intent.getStringExtra("notificationTitle");
            String newText = intent.getStringExtra("notificationText");
            int newSmallIcon = intent.getIntExtra("smallIcon", smallIconResourceID);
            int newAccuracy = intent.getIntExtra("requestedAccuracy", requestedAccuracy);

            // Verifying what changes
            boolean toRestart = newInterval != updateInterval || newAccuracy != requestedAccuracy;
            boolean updateNotif = (!newTitle.isEmpty() && newTitle != notificationTitle) ||
                                  (!newText.isEmpty() && newText != notificationText) ||
                                  (newSmallIcon > 0 && newSmallIcon != smallIconResourceID);

            notificationText = newText;
            notificationTitle = newTitle;
            updateInterval = newInterval;
            requestedAccuracy = newAccuracy;

            if (newSmallIcon > 0)
              smallIconResourceID = newSmallIcon;

            // Restarting service with new options
            if (toRestart && Utils.isRequestingLocation(this)) {
              boolean foreground = serviceIsRunningInForeground(this);
              if (foreground)
                stopForeground(true);

              removeLocationUpdates();
              createLocationRequest();
              requestLocationUpdates();

              if (foreground)
                startForeground(NOTIFICATION_ID, getNotification());

            } else if (updateNotif && serviceIsRunningInForeground(this)) {
              mNotificationManager.notify(NOTIFICATION_ID, getNotification());
            }

            break;
        }
      }
    }

    // Tells the system to not try to recreate the service after it has been killed.
    return START_NOT_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  public void onRebind(Intent intent) {
    super.onRebind(intent);
  }

  @Override
  public boolean onUnbind(Intent intent) {
    Log.i(TAG, "Last client unbound from service");
    return true; // Ensures onRebind() is called when a client re-binds.
  }

  @Override
  public void onDestroy() {
    mServiceHandler.removeCallbacksAndMessages(null);
    stopForeground(true);
    stopSelf();
  }

  public void requestLocationUpdates() {
    getLastLocation();

    Log.d(TAG, "Requesting location updates");
    Utils.setRequestingLocation(this);
    startService(
      new Intent(getApplicationContext(), LocationUpdatesService.class)
    );
    try {
      mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    } catch (SecurityException unlikely) {
      Utils.unsetRequestingLocation(this);
      Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
    }
  }

  public void removeLocationUpdates() {
    Log.i(TAG, "Removing location updates");
    try {
      mFusedLocationClient.removeLocationUpdates(mLocationCallback);
      Utils.unsetRequestingLocation(this);
      stopSelf();
    } catch (SecurityException unlikely) {
      Utils.setRequestingLocation(this);
      Log.e(
        TAG,
        "Lost location permission. Could not remove updates. " + unlikely
      );
    }
  }

  private Notification getNotification() {
    Intent intent;
    try {
      intent = new Intent(this,  Class.forName(mainActivityName));
    } catch (Exception ex) {
      intent = new Intent(this,  getApplication().getClass());
      Log.w(TAG, "Cannot create main activity Intent: " + ex.getLocalizedMessage());
      Log.d(TAG, "Stack: " + Log.getStackTraceString(ex));
    }
    intent.setAction(Intent.ACTION_VIEW);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentText(notificationText)
      .setContentTitle(notificationTitle)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setSmallIcon(smallIconResourceID)
      .setTicker(notificationText)
      .setContentIntent(pendingIntent)
      .setWhen(System.currentTimeMillis());

    return builder.build();
  }

  private void getLastLocation() {
    try {
      mFusedLocationClient
        .getLastLocation()
        .addOnCompleteListener(
          new OnCompleteListener<Location>() {

            @Override
            public void onComplete(@NonNull Task<Location> task) {
              if (task.isSuccessful() && task.getResult() != null) {
                mLocation = task.getResult();
                onNewLocation(mLocation);
              } else {
                Log.w(TAG, "Failed to get location.");
              }
            }
          }
        );
    } catch (SecurityException unlikely) {
      Log.e(TAG, "Lost location permission." + unlikely);
    }
  }

  private void onNewLocation(Location location) {
    Log.d(TAG, "New location: " + location);

    // Notify anyone listening for broadcasts about the new location.
    Intent intent = new Intent(ACTION_BROADCAST);
    intent.putExtra(EXTRA_LOCATION, location);
    LocalBroadcastManager
      .getInstance(getApplicationContext())
      .sendBroadcast(intent);
  }

  private void createLocationRequest() {
    mLocationRequest = new LocationRequest();
    mLocationRequest.setInterval(updateInterval);
    mLocationRequest.setFastestInterval(updateInterval / 2);
    mLocationRequest.setPriority(requestedAccuracy);
  }

  public class LocalBinder extends Binder {
    LocationUpdatesService getService() {
      return LocationUpdatesService.this;
    }
  }

  public boolean serviceIsRunningInForeground(Context context) {
    ActivityManager manager = (ActivityManager) context.getSystemService(
      Context.ACTIVITY_SERVICE
    );
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
      Integer.MAX_VALUE
    )) {
      if (getClass().getName().equals(service.service.getClassName())) {
        if (service.foreground) {
          return true;
        }
      }
    }
    return false;
  }
}
