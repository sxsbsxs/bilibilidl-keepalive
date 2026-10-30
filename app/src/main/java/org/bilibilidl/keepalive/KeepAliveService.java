package org.bilibilidl.keepalive;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class KeepAliveService extends Service {

    public static final String ACTION_START =
            "org.bilibilidl.keepalive.START";

    public static final String ACTION_STOP =
            "org.bilibilidl.keepalive.STOP";

    private static final String CHANNEL_ID = "bilibilidl_keepalive";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("bilibilidl 服务助手")
                .setContentText("正在维持网站服务运行")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        startTermuxCommand(
                "cd /data/data/com.termux/files/home/bilibilidl-web && " +
                "if ! pgrep -f '[p]ython app.py' >/dev/null; then " +
                "nohup python app.py >/dev/null 2>&1 & fi"
        );

        startTermuxCommand(
                "if ! pgrep -f '[c]loudflared tunnel --url http://127.0.0.1:5000' >/dev/null; then " +
                "nohup cloudflared tunnel --url http://127.0.0.1:5000 >/dev/null 2>&1 & fi"
        );

        return START_STICKY;
    }

    private void startTermuxCommand(String command) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    "com.termux",
                    "com.termux.app.RunCommandService"
            ));
            intent.setAction("com.termux.RUN_COMMAND");
            intent.putExtra("com.termux.RUN_COMMAND_PATH",
                    "/data/data/com.termux/files/usr/bin/bash");
            intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",
                    new String[]{"-lc", command});
            intent.putExtra("com.termux.RUN_COMMAND_WORKDIR",
                    "/data/data/com.termux/files/home");
            intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);

            startService(intent);
        } catch (Exception e) {
            Log.e("bilibilidl", "调用 Termux 失败", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "bilibilidl 服务运行状态",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("维持 bilibilidl 网站服务运行");

            NotificationManager manager =
                    getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
