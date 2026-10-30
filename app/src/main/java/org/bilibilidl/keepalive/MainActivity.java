package org.bilibilidl.keepalive;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private void startKeepAlive() {
        Intent intent = new Intent(this, KeepAliveService.class);
        intent.setAction(KeepAliveService.ACTION_START);

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 48, 36, 36);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("bilibilidl 保活助手");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView description = new TextView(this);
        description.setText(
            "Android 14 后台服务管理\n\n" +
            "启动后将请求 Termux 运行 Flask 和 Cloudflare Tunnel。\n\n" +
            "请确保 Termux 中存在 ~/bilibilidl-web 项目。"
        );
        description.setTextSize(16);
        description.setPadding(0, 28, 0, 28);
        layout.addView(description);

        Button start = new Button(this);
        start.setText("启动网站服务");

        start.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    20
                );
            } else {
                startKeepAlive();
            }
        });

        layout.addView(start);

        Button stop = new Button(this);
        stop.setText("停止网站服务");

        stop.setOnClickListener(v -> {
            Intent intent = new Intent(this, KeepAliveService.class);
            intent.setAction(KeepAliveService.ACTION_STOP);
            startService(intent);
        });

        layout.addView(stop);

        setContentView(layout);
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] results
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results);

        if (requestCode == 20 &&
            results.length > 0 &&
            results[0] == PackageManager.PERMISSION_GRANTED) {
            startKeepAlive();
        }
    }
}
