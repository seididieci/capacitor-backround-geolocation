package com.getcapacitor.community.bglocation;

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceManager;

class Utils {
  public static final String IS_REQUESTING_LOCATION = "is_requesting_location";

  public static void setRequestingLocation(Context context) {
    PreferenceManager
      .getDefaultSharedPreferences(context)
      .edit()
      .putBoolean(IS_REQUESTING_LOCATION, true)
      .apply();
  }

  public static void unsetRequestingLocation(Context context) {
    PreferenceManager
      .getDefaultSharedPreferences(context)
      .edit()
      .putBoolean(IS_REQUESTING_LOCATION, false)
      .apply();
  }

  public static boolean isRequestingLocation(Context context) {
    return PreferenceManager
      .getDefaultSharedPreferences(context)
      .getBoolean(IS_REQUESTING_LOCATION, false);
  }

  // This is not used anymore
  // public static int getResourceId(Context context, String pVariableName, String pResourcename, String pPackageName) throws RuntimeException {
  //   try {
  //     return context.getResources().getIdentifier(pVariableName, pResourcename, pPackageName);
  //   } catch (Exception e) {
  //     throw new RuntimeException("Error getting Resource ID.", e);
  //   }
  // }

}
