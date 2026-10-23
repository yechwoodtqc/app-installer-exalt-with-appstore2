package com.lge.lginstallservie;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Sends best-effort authenticated download/install telemetry without blocking UI or install callbacks. */
public final class StoreReporter {
    private StoreReporter() {}
    public static void report(Context context, String packageName, String version, String status) {
        SharedPreferences prefs = context.getSharedPreferences("yw_store", Context.MODE_PRIVATE);
        final String token = prefs.getString("device_token", null);
        if (token == null || packageName == null || packageName.length() == 0) return;
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                if (ApiConfig.BASE_URL.contains("REPLACE-WITH-YOUR-WORKER")) return;
                JSONObject body = new JSONObject();
                body.put("packageName", packageName);
                body.put("version", version == null ? "" : version);
                body.put("status", status);
                connection = (HttpURLConnection)new URL(ApiConfig.BASE_URL + "/api/report").openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization", "Bearer " + token);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream out = connection.getOutputStream()) { out.write(body.toString().getBytes("UTF-8")); }
                connection.getResponseCode();
            } catch (Exception ignored) {
                // Reporting is best effort and must never interrupt a user installation.
            } finally { if (connection != null) connection.disconnect(); }
        }).start();
    }
}
