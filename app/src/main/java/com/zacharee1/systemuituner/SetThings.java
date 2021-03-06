package com.zacharee1.systemuituner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.zacharee1.systemuituner.activities.NoRootSystemSettingsActivity;
import com.zacharee1.systemuituner.activities.SetupActivity;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Zacha on 4/19/2017.
 */

public class SetThings {
    public final boolean Dark;
    public final boolean setup;

    public final int titleText;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final int style;
    final ColorStateList drawerItem;

    public final SharedPreferences sharedPreferences;
    public final SharedPreferences.Editor editor;

    final ArrayList<Integer> pages;

    private final Activity currentActivity;

    private final Context context;

    private final Exceptions exceptions;

    public final int SDK_INT;

    public SetThings(Activity activity) {
        //set all variables
        sharedPreferences = activity.getSharedPreferences(activity.getResources().getText(R.string.sharedprefs_id).toString(), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.apply();
        Dark = sharedPreferences.getBoolean("isDark", false);
        setup = sharedPreferences.getBoolean("isSetup", false);
        exceptions = new Exceptions();

        SDK_INT = Build.VERSION.SDK_INT;

        //noinspection deprecation
        titleText = activity.getResources().getColor(Dark ? android.R.color.primary_text_dark : android.R.color.primary_text_light);
        //noinspection deprecation,deprecation
        drawerItem = Dark ? activity.getResources().getColorStateList(R.color.drawer_item_dark) : activity.getResources().getColorStateList(R.color.drawer_item_light);

//        activity.setTheme(SetupActivity.class == activity.getClass() || NoRootSystemSettingsActivity.class == activity.getClass() ? Dark ? R.style.DARK : R.style.AppTheme : Dark ? R.style.DARK_NoAppBar : R.style.AppTheme_NoActionBar);
        activity.setTheme(Dark ? R.style.DARK : R.style.AppTheme);

        style = Dark ? R.style.DARK_NoAppBar : R.style.AppTheme_NoActionBar; //is dark mode on?

        pages = new ArrayList<Integer>() {{ //all (currently used) fragments
                add(R.id.nav_home);
                add(R.id.nav_statusbar);
                add(R.id.nav_demo_mode);
                add(R.id.nav_about);
                add(R.id.nav_settings);
                add(R.id.nav_misc);
                add(R.id.nav_quick_settings);
                add(R.id.nav_touchwiz);
            }};

        currentActivity = activity;

        context = currentActivity; //kinda pointless...
    }

    public void buttons(final Button button, final String name) { //set button listeners
        button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("InlinedApi")
            @Override
            public void onClick(View v) {
                Intent intent;
                try {
                    switch (name) {
                        case "setup":
                            intent = new Intent(currentActivity.getApplicationContext(), SetupActivity.class);
                            currentActivity.startActivity(intent);
                            currentActivity.finish();
                            break;
                        case "enableDemo":
                            settings("global", "sysui_demo_allowed", "1");
                            break;
                        case "SystemSettingsPerms":
                            intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                            intent.setData(Uri.parse("package:" + currentActivity.getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            currentActivity.startActivity(intent);
                            break;
                        case "WriteSystemSettings":
//                            Settings.System.putInt(currentActivity.getContentResolver(), sharedPreferences.getString("systemSettingKey", ""), Integer.decode(sharedPreferences.getString("isSystemSwitchEnabled", "0")));
                            WriteSettings writeSettings = new WriteSettings(currentActivity);

                            writeSettings.writeSystem(sharedPreferences.getString("systemSettingKey", ""), sharedPreferences.getString("isSystemSwitchEnabled", "0"));
                            break;
                        case "reset_blacklist":
                            Settings.Secure.putString(currentActivity.getContentResolver(), "icon_blacklist", "");
                            intent = new Intent("check_statbar_toggles");
                            currentActivity.sendBroadcast(intent);
                            break;
                    }
                } catch (Exception e) {
                    exceptions.systemSettings(currentActivity, currentActivity.getApplicationContext(), e, "SetThings");
                }
            }
        });
    }

    public void donate() {
        try {
            boolean labsInstalled = isPackageInstalled("com.xda.labs", context.getPackageManager());
            Uri uri = Uri.parse(labsInstalled ? "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=andywander@yahoo.com" : "https://forum.xda-developers.com/donatetome.php?u=7055541");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            currentActivity.startActivity(intent);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void switches(final Switch toggle, final String pref, final String settingType, @SuppressWarnings("UnusedParameters") final View view) { //set switch listeners

        //check to see if switch should be toggled
        int setting = 0;
        switch (settingType) {
            case "global":
                setting = Settings.Global.getInt(currentActivity.getContentResolver(), pref, 0);
                break;
            case "secure":
                setting = Settings.Secure.getInt(currentActivity.getContentResolver(), pref, 0);
                break;
            case "system":
                setting = Settings.System.getInt(currentActivity.getContentResolver(), pref, 0);
                break;
            case "icon_blacklist":
                setting = 1;

                String blacklist = Settings.Secure.getString(currentActivity.getContentResolver(), "icon_blacklist");

                ArrayList<String> blacklistItems = new ArrayList<>();
                if (blacklist != null) blacklistItems.addAll(Arrays.asList(blacklist.split(",")));

                ArrayList<String> blacklistPref = new ArrayList<>();
                if (pref != null) blacklistPref.addAll(Arrays.asList(pref.split(",")));

//                for (String item : blacklistItems) {
//                    if (blacklistItems.contains(item)) setting = 0;
//                    Log.e("SETTING_BL", blacklistItems.toString());
//                    Log.e("SETTING_PR", blacklistPref.toString());
//                    Log.e("SETTING_IT", item.concat(" ").concat(setting + ""));
//                }

                for (String i : blacklistItems)
                {
                    for (String j : blacklistPref)
                    {
                        if (i.equals(j)) {
                            setting = 0;
                            Log.e("SETTING", i);
                        }
                    }
                }
                break;
            case "dark_mode":
                setting = Dark ? 1 : 0;
                break;
        }
        toggle.setChecked(setting == 1);

        //set switch listeners
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                try {
                    switch (settingType) {
                        case "icon_blacklist":
                            String blacklist = Settings.Secure.getString(currentActivity.getContentResolver(), "icon_blacklist");
                            String[] blacklistItems = new String[] {""};
                            if (blacklist != null && blacklist.length() > 0) blacklistItems = blacklist.split(",");

                            Log.i("blacklistItems", Arrays.toString(blacklistItems));

                            ArrayList<String> blacklistPref = new ArrayList<>();
                            if (pref != null) blacklistPref.addAll(Arrays.asList(pref.split(",")));

                            ArrayList<String> blItems = new ArrayList<>();
                            blItems.addAll(Arrays.asList(blacklistItems));

                            if (isChecked) {
                                for (String item : blacklistPref) {
                                    for (int i = 0; i < blItems.size(); i++) {
                                        if (item.equals(blItems.get(i))) blItems.remove(i);
                                    }
                                }
                            } else {
                                for (String item: blacklistPref) {
                                    if (item.length() > 0) blItems.add(item);
                                }
                            }

                            Log.i("blItems", blItems.toString());

                            StringBuilder bl = new StringBuilder();

                            for (int i = 0; i < blItems.size(); i++) {
                                if (bl.length() > 0) bl.append(",");
                                bl.append(blItems.get(i));
                            }

                            Log.i("bl", bl.toString());

                            try {
                                Settings.Secure.putString(currentActivity.getContentResolver(), "icon_blacklist", bl.toString());
                                Settings.Secure.putString(currentActivity.getContentResolver(), "icon_blacklist2", bl.toString());
                            } catch (final Exception e) {
                                exceptions.secureSettings(currentActivity, currentActivity.getApplicationContext(), e, "icon_blacklist");
                            }
                            break;
                        case "dark_mode":
                            editor.putBoolean("isDark", isChecked);
                            editor.apply();
                            currentActivity.recreate();
                            break;
                        default:
                            settings(settingType, pref, isChecked ? "1" : "0");
                            break;
                    }
                } catch (Exception e) {
                    exceptions.secureSettings(currentActivity, currentActivity.getApplicationContext(), e, "Status Bar");
                }
            }
        });
    }

    public void settings(final String type, final String pref, final String value) { //write to settings
        try {
            switch (type) {
                case "global":
                    Settings.Global.putString(currentActivity.getContentResolver(), pref, value);
                    break;
                case "secure":
                    Settings.Secure.putString(currentActivity.getContentResolver(), pref, value);
                    break;
                case "system":
                    try {
                        Settings.System.putString(currentActivity.getContentResolver(), pref, value);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("HALP", e.getMessage());
                        if (sharedPreferences.getBoolean("isRooted", true)) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    sudo("settings put system " + pref + " " + value);
                                }
                            }).start();
                        } else {
                            editor.putString("isSystemSwitchEnabled", value);
                            editor.putString("systemSettingKey", pref);
                            editor.apply();
                            Intent intent = new Intent(currentActivity.getApplicationContext(), NoRootSystemSettingsActivity.class);
                            currentActivity.startActivity(intent);
                        }
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
            exceptions.secureSettings(currentActivity, currentActivity.getApplicationContext(), e, "SetThings");
        }
    }

    private boolean isPackageInstalled(@SuppressWarnings("SameParameterValue") String packagename, PackageManager packageManager) { //check to see if a
        try {
            packageManager.getPackageInfo(packagename, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void sudo(String... strings) {
        try{
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            for (String s : strings) {
                outputStream.writeBytes(s+"\n");
                outputStream.flush();
            }

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            try {
                su.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e("No Root?", e.getMessage());
            }
            outputStream.close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private boolean testSudo() {
        StackTraceElement st = null;

        try{
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            outputStream.writeBytes("exit\n");
            outputStream.flush();

            DataInputStream inputStream = new DataInputStream(su.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            while (bufferedReader.readLine() != null) {
                bufferedReader.readLine();
            }

            su.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            for (StackTraceElement s : e.getStackTrace()) {
                st = s;
                if (st != null) break;
            }
        }

        return st == null;
    }
}
