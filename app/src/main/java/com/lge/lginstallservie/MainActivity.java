package com.lge.lginstallservie;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ReminderReceiver.scheduleNext(this);
        TextView message = new TextView(this);
        message.setPadding(24, 24, 24, 24);
        message.setText("YW app installer\n\nOpen an APK file with this app to start an Android PackageInstaller session. Android may ask you to confirm the installation.\n\nThe device-admin reminder opens the separate system confirmation screen directly.");
        setContentView(message);

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri apkUri = getIntent().getData();
            if (apkUri != null) installApk(apkUri);
            else Toast.makeText(this, "No APK file was provided.", Toast.LENGTH_LONG).show();
        }
    }

    private void installApk(Uri apkUri) {
        PackageInstaller.Session session = null;
        try {
            ContentResolver resolver = getContentResolver();
            InputStream input = resolver.openInputStream(apkUri);
            if (input == null) throw new IllegalStateException("Cannot read the selected APK.");

            PackageInstaller installer = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params =
                    new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionId = installer.createSession(params);
            session = installer.openSession(sessionId);

            try (InputStream source = input;
                 OutputStream output = session.openWrite("base.apk", 0, -1)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = source.read(buffer)) != -1) output.write(buffer, 0, count);
                session.fsync(output);
            }

            Intent statusIntent = new Intent(this, InstallStatusReceiver.class);
            statusIntent.setAction("com.lge.lginstallservie.INSTALL_STATUS." + sessionId);
            PendingIntent statusPendingIntent = PendingIntent.getBroadcast(
                    this, sessionId, statusIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            session.commit(statusPendingIntent.getIntentSender());
            Toast.makeText(this, "APK sent to Android PackageInstaller.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not start APK installation: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            if (session != null) {
                try { session.abandon(); } catch (Exception ignored) { }
            }
        }
    }
}
