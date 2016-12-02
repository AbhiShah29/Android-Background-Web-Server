/**
 * Copyright (c) 2016 Volansys - All rights reserved.
 * This software is authored by Volansys and is Volansys's intellectual
 * property, including the copyrights in all countries in the world.
 * This software is provided under a license to use only with all other rights,
 * including ownership rights, being retained by Volansys.
 * This file may not be distributed, copied, or reproduced in any manner,
 * electronic or otherwise, without the written consent of Volansys.
 */
package ab.explore.android_background_web_server;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * Shared Preference
 */
public class AppSharedPreference {

    public AppSharedPreference() {
        super();
    }

    public void savePreferences(Context context, String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public String loadSavedPreferences(Context context,String key) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        return sharedPreferences.getString(key, "");
    }

}
