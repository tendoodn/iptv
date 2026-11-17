package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Channel logos packaged offline at build time, with optional HTTPS logo fetch for
 * channels later added by the owner to the public M3U file. Never blocks DPAD focus.
 */
final class TendooLogoCache {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, Bitmap> BITMAPS = new HashMap<>();
    private static final Set<String> PENDING = new HashSet<>();
    private static boolean bundledRequested;

    static String key(String name) {
        if (name == null) return "";
        return name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    static void preload(Context context, Runnable ready) {
        Context app = context.getApplicationContext();
        synchronized (BITMAPS) {
            if (bundledRequested) {
                if (ready != null) MAIN.post(ready);
                return;
            }
            bundledRequested = true;
        }
        IO.execute(() -> {
            try (InputStream input = app.getAssets().open("logo_pack.json")) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int n;
                while ((n = input.read(chunk)) != -1 && buffer.size() < 4_000_000)
                    buffer.write(chunk, 0, n);
                JSONObject packed = new JSONObject(buffer.toString("UTF-8"));
                java.util.Iterator<String> names = packed.keys();
                while (names.hasNext()) {
                    String name = names.next();
                    String encoded = packed.optString(name);
                    if (encoded.isEmpty()) continue;
                    byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
                    Bitmap logo = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (logo != null) synchronized (BITMAPS) { BITMAPS.put(key(name), logo); }
                }
            } catch (Exception ignored) { }
            if (ready != null) MAIN.post(ready);
        });
    }
    static Drawable drawable(Context context, TendooPlaylist.Channel channel, Runnable repaint) {
        String id = key(channel.name);
        Bitmap bitmap;
        synchronized (BITMAPS) { bitmap = BITMAPS.get(id); }
        if (bitmap != null) return new BitmapDrawable(context.getResources(), bitmap);

        if (channel.logo == null || !channel.logo.startsWith("https://")) return null;
        boolean enqueue;
        synchronized (BITMAPS) { enqueue = PENDING.add(id); }
        if (!enqueue) return null;
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            Bitmap decoded = fromDiskOrDownload(app, id, channel.logo);
            synchronized (BITMAPS) {
                if (decoded != null) BITMAPS.put(id, decoded);
                PENDING.remove(id);
            }
            if (decoded != null && repaint != null) MAIN.post(repaint);
        });
        return null;
    }
    private static Bitmap fromDiskOrDownload(Context app, String name, String uri) {
        File folder = new File(app.getCacheDir(), "channel-logos");
        if (!folder.exists() && !folder.mkdirs()) return null;
        File file = new File(folder, digest(uri) + ".img");
        if (file.isFile()) {
            Bitmap cached = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (cached != null) return cached;
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(uri);
            if (!"https".equalsIgnoreCase(url.getProtocol())) return null;
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5500);
            connection.setReadTimeout(6000);
            connection.setInstanceFollowRedirects(true);
            if (connection.getResponseCode() != 200) return null;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] chunk = new byte[4096];
                int n;
                while ((n = input.read(chunk)) != -1) {
                    if (output.size() + n > 600_000) return null;
                    output.write(chunk, 0, n);
                }
            }
            byte[] data = output.toByteArray();
            BitmapFactory.Options info = new BitmapFactory.Options();
            info.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, info);
            if (info.outWidth < 1 || info.outHeight < 1) return null;
            info.inJustDecodeBounds = false;
            info.inSampleSize = Math.max(1, Math.max(info.outWidth / 220, info.outHeight / 130));
            Bitmap decoded = BitmapFactory.decodeByteArray(data, 0, data.length, info);
            if (decoded == null) return null;
            try (FileOutputStream out = new FileOutputStream(file)) { out.write(data); }
            return decoded;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
    private static String digest(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(
                    input.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < 12; i++) result.append(String.format("%02x", bytes[i]));
            return result.toString();
        } catch (Exception ignored) { return Integer.toHexString(input.hashCode()); }
    }
}
