package org.bilibilidl.keepalive;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private final int PINK = Color.rgb(240, 75, 135);
    private final int DARK = Color.rgb(45, 45, 55);
    private final int LIGHT = Color.rgb(255, 241, 247);

    private EditText urlInput;
    private TextView flaskStatus;
    private TextView tunnelStatus;
    private SharedPreferences prefs;

    private static final String PREFS = "bilibilidl_settings";
    private static final String URL_KEY = "public_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 35, 24, 24);
        root.setBackgroundColor(Color.WHITE);

        TextView title = text("bilibilidl 服务助手", 25, PINK);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        TextView subtitle = text(
                "管理网站服务和公网访问链接",
                14, Color.GRAY
        );
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 8, 0, 28);
        root.addView(subtitle, matchWrap());

        TextView urlTitle = text("公网访问地址", 17, DARK);
        root.addView(urlTitle, matchWrap());

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setTextSize(15);
        urlInput.setTextColor(DARK);
        urlInput.setHint("粘贴 OpenPort 地址，例如 http://openport.pw:27329");
        urlInput.setHintTextColor(Color.GRAY);
        urlInput.setPadding(14, 12, 14, 12);
        urlInput.setBackgroundColor(LIGHT);
        urlInput.setText(prefs.getString(URL_KEY, ""));
        LinearLayout.LayoutParams inputParams = matchWrap();
        inputParams.topMargin = 10;
        inputParams.bottomMargin = 12;
        root.addView(urlInput, inputParams);

        Button saveButton = button("保存访问链接", PINK);
        root.addView(saveButton, matchWrap());

        Button copyButton = button("复制访问链接", PINK);
        LinearLayout.LayoutParams copyParams = matchWrap();
        copyParams.topMargin = 9;
        root.addView(copyButton, copyParams);

        Button openButton = button("浏览器打开网站", PINK);
        LinearLayout.LayoutParams openParams = matchWrap();
        openParams.topMargin = 9;
        root.addView(openButton, openParams);

        TextView serviceTitle = text("服务运行状态", 18, DARK);
        LinearLayout.LayoutParams titleParams = matchWrap();
        titleParams.topMargin = 30;
        titleParams.bottomMargin = 12;
        root.addView(serviceTitle, titleParams);

        flaskStatus = text("Flask 网站：等待启动", 15, Color.GRAY);
        root.addView(flaskStatus, matchWrap());

        tunnelStatus = text("OpenPort：等待启动", 15, Color.GRAY);
        LinearLayout.LayoutParams tunnelParams = matchWrap();
        tunnelParams.topMargin = 9;
        root.addView(tunnelStatus, tunnelParams);

        Button startButton = button("启动网站和内网穿透", PINK);
        LinearLayout.LayoutParams startParams = matchWrap();
        startParams.topMargin = 25;
        root.addView(startButton, startParams);

        Button stopButton = button("停止服务助手", Color.GRAY);
        LinearLayout.LayoutParams stopParams = matchWrap();
        stopParams.topMargin = 10;
        root.addView(stopButton, stopParams);

        TextView note = text(
                "提示：公网地址可能在重新连接后变化，请及时更新。",
                12, Color.GRAY
        );
        note.setPadding(0, 22, 0, 0);
        root.addView(note, matchWrap());

        setContentView(root);

        saveButton.setOnClickListener(v -> saveUrl());
        copyButton.setOnClickListener(v -> copyUrl());
        openButton.setOnClickListener(v -> openUrl());

        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, KeepAliveService.class);
            intent.setAction(KeepAliveService.ACTION_START);
            startForegroundService(intent);

            flaskStatus.setText("Flask 网站：已发送启动请求");
            tunnelStatus.setText("OpenPort：正在启动");
            toast("已发送启动请求");
        });

        stopButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, KeepAliveService.class);
            intent.setAction(KeepAliveService.ACTION_STOP);
            startService(intent);

            flaskStatus.setText("Flask 网站：停止请求已发送");
            tunnelStatus.setText("OpenPort：停止请求已发送");
            toast("已发送停止请求");
        });
    }

    private void saveUrl() {
        String url = urlInput.getText().toString().trim();

        if (!isValidUrl(url)) {
            toast("请输入有效的公网网址");
            return;
        }

        prefs.edit().putString(URL_KEY, normalizeUrl(url)).apply();
        urlInput.setText(normalizeUrl(url));
        toast("访问链接已保存");
    }

    private void copyUrl() {
        String url = getUrl();

        if (url.isEmpty()) {
            toast("请先填写公网访问地址");
            return;
        }

        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        clipboard.setPrimaryClip(
                ClipData.newPlainText("bilibilidl 公网地址", url)
        );

        toast("链接已复制");
    }

    private void openUrl() {
        String url = getUrl();

        if (url.isEmpty()) {
            toast("请先填写公网访问地址");
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            toast("无法打开浏览器");
        }
    }

    private String getUrl() {
        String url = urlInput.getText().toString().trim();

        if (!isValidUrl(url)) {
            return "";
        }

        return normalizeUrl(url);
    }

    private boolean isValidUrl(String url) {
        return url.matches("(?i)^https?://[^\\s/]+(?::\\d+)?/?$");
    }

    private String normalizeUrl(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String value, int color) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(color)
        );
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
