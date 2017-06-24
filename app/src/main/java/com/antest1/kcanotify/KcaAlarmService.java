package com.antest1.kcanotify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.isExpeditionDataLoaded;
import static com.antest1.kcanotify.KcaApiData.isGameDataLoaded;
import static com.antest1.kcanotify.KcaApiData.loadSimpleExpeditionInfoFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_DECKPORT;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_STARTDATA;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_NOTICOUNT_CHANGED;
import static com.antest1.kcanotify.KcaConstants.KCA_API_UPDATE_FRONTVIEW;
import static com.antest1.kcanotify.KcaConstants.NOTI_DOCK;
import static com.antest1.kcanotify.KcaConstants.NOTI_EXP;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_DOCK;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_EXP;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_NOTIFYATSVCOFF;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_RINGTONE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_SOUND_KIND;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getKcIntent;
import static com.antest1.kcanotify.KcaUtils.getNotificationId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;

public class KcaAlarmService extends Service {
    public static final int TYPE_EXPEDITION = 1;
    public static final int TYPE_DOCKING = 2;

    public static final int EXP_CANCEL_FLAG = 8;
    public static final long ALARM_DELAY = 61000;
    public static Set<Integer> alarm_set = new HashSet<>();

    public static final String ACTION_PREFIX = "action_";
    public static final String CLICK_ACTION = "action_click";
    public static final String REDUCE_COUNT = "action_reduce_count";

    AudioManager mAudioManager;
    KcaDBHelper dbHelper;
    NotificationManager notificationManager;
    Bitmap expBitmap, dockBitmap = null;
    public static Handler sHandler = null;
    Bundle bundle;
    Message sMsg;

    private boolean isExpAlarmEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_EXP);
    }

    private boolean isDockAlarmEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_DOCK);
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    public static int getAlarmCount() { return alarm_set.size(); }

    public static void clearAlarmCount() { alarm_set.clear(); }

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    @Override
    public void onCreate() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        expBitmap = ((BitmapDrawable) ContextCompat.getDrawable(this, R.mipmap.expedition_notify_bigicon)).getBitmap();
        dockBitmap = ((BitmapDrawable) ContextCompat.getDrawable(this, R.mipmap.docking_notify_bigicon)).getBitmap();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("KCA", "KcaAlarmService Called: " + String.valueOf(startId));
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().startsWith(ACTION_PREFIX)) {
                if (intent.getAction().equals(CLICK_ACTION)) {
                    Intent kcintent = getKcIntent(getApplicationContext());
                    if (kcintent != null) startActivity(kcintent);
                }
                int nid = intent.getIntExtra("nid", -1);
                alarm_set.remove(nid);
                notificationManager.cancel(nid);
            }
        } else if (getBooleanPreferences(getApplication(), PREF_KCA_NOTI_NOTIFYATSVCOFF) || KcaService.getServiceStatus()) {
            loadTranslationData(getAssets(), getApplicationContext());
            if (intent != null && intent.getStringExtra("data") != null) {
                JsonObject data = new JsonParser().parse(intent.getStringExtra("data")).getAsJsonObject();
                int type = data.get("type").getAsInt();
                String locale = LocaleUtils.getLocaleCode(getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE));
                if (type == TYPE_EXPEDITION) {
                    int idx = data.get("idx").getAsInt();
                    KcaExpedition2.clearMissionData(idx);
                    if (isExpAlarmEnabled()) {
                        if (!isExpeditionDataLoaded()) loadSimpleExpeditionInfoFromAssets(getAssets());
                        int mission_no = data.get("mission_no").getAsInt();
                        String mission_name = KcaApiData.getExpeditionName(mission_no, locale);
                        String kantai_name = data.get("kantai_name").getAsString();
                        boolean cancelFlag = data.get("cancel_flag").getAsBoolean();
                        boolean caFlag = data.get("ca_flag").getAsBoolean();
                        if (caFlag) idx = idx | EXP_CANCEL_FLAG;
                        int nid = getNotificationId(NOTI_EXP, idx);
                        notificationManager.notify(nid, createExpeditionNotification(mission_no, mission_name, kantai_name, cancelFlag, caFlag, nid));
                        alarm_set.add(nid);
                    }
                } else if (type == TYPE_DOCKING) {
                    int dockId = data.get("dock_id").getAsInt();
                    KcaDocking.setCompleteTime(dockId, -1);
                    KcaDocking.setShipId(dockId, 0);
                    if(isDockAlarmEnabled()) {
                        int shipId = data.get("ship_id").getAsInt();
                        String shipName = "";
                        if (shipId != -1) {
                            if (!isGameDataLoaded()) {
                                JsonObject cachedData = dbHelper.getJsonObjectValue(DB_KEY_STARTDATA);
                                KcaApiData.getKcGameData(cachedData.getAsJsonObject("api_data"));
                            }
                            JsonObject kcShipData = KcaApiData.getKcShipDataById(shipId, "name");
                            shipName = getShipTranslation(kcShipData.get("name").getAsString(), false);
                        }
                        int nid = getNotificationId(NOTI_DOCK, dockId);
                        notificationManager.notify(nid, createDockingNotification(dockId, shipName, nid));
                        alarm_set.add(nid);
                    }
                }
            }
        }
        Log.e("KCA", "Noti Count: " + String.valueOf(alarm_set.size()));
        if (sHandler != null) {
            bundle = new Bundle();
            bundle.putString("url", KCA_API_PREF_NOTICOUNT_CHANGED);
            bundle.putString("data", "");
            sMsg = sHandler.obtainMessage();
            sMsg.setData(bundle);
            sHandler.sendMessage(sMsg);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (expBitmap != null) {
            expBitmap.recycle();
            expBitmap = null;
        }
        if (dockBitmap != null) {
            dockBitmap.recycle();
            dockBitmap = null;
        }
        super.onDestroy();
    }

    private Notification createExpeditionNotification(int missionNo, String missionName, String kantaiName, boolean cancelFlag, boolean caFlag, int nid) {
        PendingIntent contentPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class).setAction(REDUCE_COUNT).putExtra("nid", nid), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent deletePendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class).setAction(REDUCE_COUNT).putExtra("nid", nid), PendingIntent.FLAG_UPDATE_CURRENT);
        String title = "";
        String content = "";
        if (cancelFlag) {
            title = String.format(getStringWithLocale(R.string.kca_noti_title_exp_canceled), missionNo, missionName);
            content = String.format(getStringWithLocale(R.string.kca_noti_content_exp_canceled), kantaiName, missionNo);
        } else {
            title = String.format(getStringWithLocale(R.string.kca_noti_title_exp_finished), missionNo, missionName);
            if (caFlag)
                content = String.format(getStringWithLocale(R.string.kca_noti_content_exp_finished_canceled), kantaiName, missionNo);
            else
                content = String.format(getStringWithLocale(R.string.kca_noti_content_exp_finished_normal), kantaiName, missionNo);

        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.expedition_notify_icon)
                .setLargeIcon(expBitmap)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setTicker(title)
                .setDeleteIntent(deletePendingIntent)
                .setContentIntent(contentPendingIntent);

        String soundKind = getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_SOUND_KIND);
        if (soundKind.equals(getString(R.string.sound_kind_value_normal)) || soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
            if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                if (soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
                    builder.setDefaults(Notification.DEFAULT_VIBRATE);
                }
                builder.setSound(Uri.parse(getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE)));
            } else if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else {
                builder.setDefaults(0);
            }
        }
        if (soundKind.equals(getString(R.string.sound_kind_value_vibrate))) {
            if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else {
                builder.setDefaults(0);
            }
        }
        if (soundKind.equals(getString(R.string.sound_kind_value_mute))) {
            builder.setDefaults(0);
        }

        Notification Notifi = builder.build();
        Notifi.flags = Notification.FLAG_AUTO_CANCEL;

        if (sHandler != null) {
            bundle = new Bundle();
            bundle.putString("url", KCA_API_UPDATE_FRONTVIEW);
            bundle.putString("data", "");
            sMsg = sHandler.obtainMessage();
            sMsg.setData(bundle);
            sHandler.sendMessage(sMsg);
        }
        return Notifi;
    }

    private Notification createDockingNotification(int dockId, String shipName, int nid) {
        PendingIntent contentPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class).setAction(CLICK_ACTION).putExtra("nid", nid), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent deletePendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class).setAction(REDUCE_COUNT).putExtra("nid", nid), PendingIntent.FLAG_UPDATE_CURRENT);
        String title = String.format(getStringWithLocale(R.string.kca_noti_title_dock_finished), dockId + 1);
        String content = "";
        if (shipName.length() > 0) {
            content = String.format(getStringWithLocale(R.string.kca_noti_content_dock_finished), dockId + 1, shipName);
        } else {
            content = String.format(getStringWithLocale(R.string.kca_noti_content_dock_finished_nodata), dockId + 1);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.docking_notify_icon)
                .setLargeIcon(dockBitmap)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setTicker(title)
                .setDeleteIntent(deletePendingIntent)
                .setContentIntent(contentPendingIntent);
        String soundKind = getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_SOUND_KIND);
        if (soundKind.equals(getString(R.string.sound_kind_value_normal)) || soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
            if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                if (soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
                    builder.setDefaults(Notification.DEFAULT_VIBRATE);
                }
                builder.setSound(Uri.parse(getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE)));
            } else if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else {
                builder.setDefaults(0);
            }
        }
        if (soundKind.equals(getString(R.string.sound_kind_value_vibrate))) {
            if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else {
                builder.setDefaults(0);
            }
        }
        if (soundKind.equals(getString(R.string.sound_kind_value_mute))) {
            builder.setDefaults(0);
        }

        Notification Notifi = builder.build();
        Notifi.flags = Notification.FLAG_AUTO_CANCEL;

        if (sHandler != null) {
            bundle = new Bundle();
            bundle.putString("url", KCA_API_UPDATE_FRONTVIEW);
            bundle.putString("data", "");
            sMsg = sHandler.obtainMessage();
            sMsg.setData(bundle);
            sHandler.sendMessage(sMsg);
            // send Handler to setFrontView
        }
        return Notifi;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
