package com.lge.lginstallservie;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final String PREFS = "yw_store";
    private static final String TOKEN = "device_token";
    private static final String DEVICE_ID = "device_id";
    private LinearLayout content;
    private EditText codeInput;
    private CheckBox mobileData;
    private BroadcastReceiver downloadReceiver;
    private String pendingPackageName = "";
    private String pendingVersion = "";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri apkUri = getIntent().getData();
            if (apkUri != null) { installApk(apkUri); return; }
        }
        render();
    }

    @Override protected void onDestroy() {
        if (downloadReceiver != null) {
            try { unregisterReceiver(downloadReceiver); } catch (Exception ignored) {}
            downloadReceiver = null;
        }
        super.onDestroy();
    }

    private SharedPreferences prefs() { return getSharedPreferences(PREFS, MODE_PRIVATE); }
    private String deviceId() {
        String id = prefs().getString(DEVICE_ID, null);
        if (id == null) { id = UUID.randomUUID().toString(); prefs().edit().putString(DEVICE_ID, id).apply(); }
        return id;
    }
    private String token() { return prefs().getString(TOKEN, null); }

    private void render() {
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        TextView title = new TextView(this);
        title.setText("YW App Store");
        title.setTextSize(24);
        root.addView(title);
        TextView intro = new TextView(this);
        intro.setPadding(0, pad / 2, 0, pad);
        intro.setText("Authorized app catalog for LG Exalt and LG Classic Flip. Use the D-pad to move between controls.");
        root.addView(intro);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);

        if (token() == null) {
            TextView prompt = new TextView(this);
            prompt.setText("Enter the one-time authorization code provided by your administrator.");
            content.addView(prompt);
            codeInput = new EditText(this);
            codeInput.setSingleLine(true);
            codeInput.setHint("Authorization code");
            content.addView(codeInput);
            Button authorize = new Button(this);
            authorize.setText("Authorize this device");
            authorize.setOnClickListener(v -> redeemCode());
            content.addView(authorize);
        } else {
            TextView authorized = new TextView(this);
            authorized.setText("Device authorization saved.");
            content.addView(authorized);
            Button refresh = new Button(this);
            refresh.setText("Refresh app catalog");
            refresh.setOnClickListener(v -> loadCatalog());
            content.addView(refresh);
            mobileData = new CheckBox(this);
            mobileData.setText("Allow downloads over mobile data (Wi-Fi only by default)");
            mobileData.setChecked(prefs().getBoolean("mobile_data", false));
            mobileData.setOnCheckedChangeListener((button, checked) -> prefs().edit().putBoolean("mobile_data", checked).apply());
            content.addView(mobileData);
            Button admin = new Button(this);
            admin.setText("Device admin settings / deactivate");
            admin.setOnClickListener(v -> {
                toast("Warning: Livigent or another device-management policy may block deactivation or app launch.");
                startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            });
            content.addView(admin);
            loadCatalog();
        }
        Button notifications = new Button(this);
        notifications.setText("Notification settings");
        notifications.setOnClickListener(v -> openNotificationSettings());
        content.addView(notifications);
    }

    private void redeemCode() {
        final String code = codeInput == null ? "" : codeInput.getText().toString().trim();
        if (code.length() < 6) { toast("Enter the authorization code from your administrator."); return; }
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("code", code);
                body.put("deviceId", deviceId());
                JSONObject response = request("POST", "/api/redeem", body, null);
                String newToken = response.optString("token", "");
                if (newToken.length() < 16) throw new Exception("Server did not return a valid device token.");
                prefs().edit().putString(TOKEN, newToken).apply();
                runOnUiThread(() -> { toast("Device authorized."); render(); });
            } catch (Exception e) { runOnUiThread(() -> toast("Authorization failed: " + e.getMessage())); }
        }).start();
    }

    private void loadCatalog() {
        if (token() == null) return;
        new Thread(() -> {
            try {
                JSONObject response = request("GET", "/api/catalog", null, token());
                JSONArray apps = response.optJSONArray("apps");
                runOnUiThread(() -> showCatalog(apps));
            } catch (Exception e) {
                runOnUiThread(() -> addMessage("Could not load catalog: " + e.getMessage() + "\nCheck network and server configuration."));
            }
        }).start();
    }

    private void showCatalog(JSONArray apps) {
        if (content == null) return;
        for (int i = content.getChildCount() - 1; i >= 0; i--) {
            if ("catalog_item".equals(content.getChildAt(i).getTag())) content.removeViewAt(i);
        }
        if (apps == null || apps.length() == 0) { addMessage("No apps are currently listed."); return; }
        for (int i = 0; i < apps.length(); i++) {
            JSONObject app = apps.optJSONObject(i);
            if (app == null) continue;
            final String name = app.optString("name", "App");
            final String description = app.optString("description", "");
            final String version = app.optString("version", "Unknown");
            final String url = app.optString("apkUrl", "");
            final String packageName = app.optString("packageName", "");
            final String iconUrl = app.optString("iconUrl", "");
            long bytes = app.optLong("sizeBytes", 0);
            String size = bytes > 0 ? String.format(java.util.Locale.US, "%.1f MB", bytes / 1048576.0) : "Size listed by server";
            LinearLayout card = new LinearLayout(this);
            card.setTag("catalog_item");
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(0, 12, 0, 12);
            if (iconUrl.startsWith("https://")) {
                ImageView icon = new ImageView(this);
                int iconSize = (int)(64 * getResources().getDisplayMetrics().density);
                icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
                icon.setContentDescription(name + " icon");
                card.addView(icon);
                new Thread(() -> {
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection)new URL(iconUrl).openConnection();
                        connection.setConnectTimeout(8000);
                        connection.setReadTimeout(8000);
                        if (connection.getResponseCode() == 200) {
                            try (InputStream in = connection.getInputStream()) {
                                Bitmap bitmap = BitmapFactory.decodeStream(in);
                                if (bitmap != null) runOnUiThread(() -> icon.setImageBitmap(bitmap));
                            }
                        }
                    } catch (Exception ignored) {
                    } finally { if (connection != null) connection.disconnect(); }
                }).start();
            }
            TextView label = new TextView(this);
            label.setText(name + "\nVersion: " + version + "   Size: " + size + "\n" + description);
            label.setTextSize(16);
            card.addView(label);
            Button download = new Button(this);
            download.setText("Download and install " + name);
            download.setOnClickListener(v -> startDownload(url, name, packageName, version));
            card.addView(download);
            content.addView(card);
        }
    }

    private void startDownload(String url, String name, String packageName, String version) {
        if (url.length() == 0 || !url.startsWith("https://")) { toast("This app has no valid HTTPS download URL."); return; }
        if (!hasNetwork()) { toast("Connect to Wi-Fi or enable mobile data first."); return; }
        try {
            DownloadManager manager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(name);
            request.setDescription("Downloading " + version);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "yw-" + safeName(name) + ".apk");
            request.setAllowedNetworkTypes(prefs().getBoolean("mobile_data", false)
                    ? DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE
                    : DownloadManager.Request.NETWORK_WIFI);
            final long downloadId = manager.enqueue(request);
            downloadReceiver = new BroadcastReceiver() {
                @Override public void onReceive(Context context, Intent intent) {
                    if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
                    if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != downloadId) return;
                    android.database.Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(downloadId));
                    try {
                        if (cursor != null && cursor.moveToFirst()) {
                            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                StoreReporter.report(getApplicationContext(), packageName, version, "downloaded");
                                pendingPackageName = packageName;
                                pendingVersion = version;
                                toast("Download complete. Opening Android installer.");
                                Uri downloadedUri = manager.getUriForDownloadedFile(downloadId);
                                if (downloadedUri == null) { toast("Download completed, but Android did not provide the APK file."); return; }
                                installApk(downloadedUri);
                            } else toast("Download failed. Please retry.");
                        }
                    } finally { if (cursor != null) cursor.close(); }
                    try { unregisterReceiver(this); } catch (Exception ignored) {}
                    downloadReceiver = null;
                }
            };
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            toast("Download started. Progress appears in Android notifications.");
        } catch (Exception e) { toast("Could not start download: " + e.getMessage()); }
    }

    private String safeName(String s) { return s.replaceAll("[^A-Za-z0-9._-]", "_"); }
    private boolean hasNetwork() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifi = cm == null ? null : cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifi != null && wifi.isConnected()) return true;
        return prefs().getBoolean("mobile_data", false) && cm != null
                && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private JSONObject request(String method, String path, JSONObject body, String bearer) throws Exception {
        if (ApiConfig.BASE_URL.contains("REPLACE-WITH-YOUR-WORKER")) throw new Exception("Store server is not configured in ApiConfig.java.");
        HttpURLConnection connection = (HttpURLConnection)new URL(ApiConfig.BASE_URL + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "application/json");
        if (bearer != null) connection.setRequestProperty("Authorization", "Bearer " + bearer);
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream out = connection.getOutputStream()) { out.write(body.toString().getBytes("UTF-8")); }
        }
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (stream != null) {
            try (InputStream in = stream) { byte[] buffer = new byte[4096]; int n; while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n); }
        }
        String text = new String(bytes.toByteArray(), "UTF-8");
        connection.disconnect();
        JSONObject result = text.length() == 0 ? new JSONObject() : new JSONObject(text);
        if (status < 200 || status >= 300) throw new Exception(result.optString("error", "HTTP " + status));
        return result;
    }

    private void addMessage(String text) {
        if (content == null) return;
        TextView message = new TextView(this);
        message.setTag("catalog_item");
        message.setText(text);
        message.setPadding(0, 12, 0, 12);
        content.addView(message);
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    private void openNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= 26) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void installApk(Uri apkUri) {
        android.content.pm.PackageInstaller.Session session = null;
        try {
            ContentResolver resolver = getContentResolver();
            InputStream input = resolver.openInputStream(apkUri);
            if (input == null) throw new IllegalStateException("Cannot read the selected APK.");
            android.content.pm.PackageInstaller installer = getPackageManager().getPackageInstaller();
            android.content.pm.PackageInstaller.SessionParams params =
                    new android.content.pm.PackageInstaller.SessionParams(android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionId = installer.createSession(params);
            session = installer.openSession(sessionId);
            try (InputStream source = input; OutputStream output = session.openWrite("base.apk", 0, -1)) {
                byte[] buffer = new byte[64 * 1024]; int count;
                while ((count = source.read(buffer)) != -1) output.write(buffer, 0, count);
                session.fsync(output);
            }
            Intent statusIntent = new Intent(this, InstallStatusReceiver.class);
            statusIntent.setAction("com.lge.lginstallservie.INSTALL_STATUS." + sessionId);
            statusIntent.putExtra("yw_package_name", pendingPackageName);
            statusIntent.putExtra("yw_version", pendingVersion);
            PendingIntent statusPendingIntent = PendingIntent.getBroadcast(this, sessionId, statusIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0));
            session.commit(statusPendingIntent.getIntentSender());
            toast("APK sent to Android PackageInstaller. Confirm installation when prompted.");
        } catch (Exception e) {
            toast("Could not start APK installation: " + e.getMessage());
            if (session != null) try { session.abandon(); } catch (Exception ignored) {}
        }
    }
}
