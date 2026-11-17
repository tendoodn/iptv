package vn.tendoo.iptv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class Playlist {
    static final String SOURCE =
        "https://raw.githubusercontent.com/tendoodn/iptv/main/playlist/vietnam.m3u";
    private static final String CACHE = "vietnam.m3u";
    private static final ExecutorService NETWORK = Executors.newSingleThreadExecutor();

    static final class Channel {
        final String name, url, group;
        Channel(String name, String url, String group) {
            this.name = name;
            this.url = url;
            this.group = group;
        }
    }
    interface Done {
        void completed(boolean success, String message);
    }
    static List<Channel> load(Context context) {
        File cached = new File(context.getFilesDir(), CACHE);
        if (cached.isFile()) {
            try (FileInputStream in = new FileInputStream(cached)) {
                List<Channel> list = parse(new String(read(in), StandardCharsets.UTF_8));
                if (!list.isEmpty()) return list;
            } catch (Exception ignored) { }
        }
        try (InputStream in = context.getAssets().open("vietnam.m3u")) {
            return parse(new String(read(in), StandardCharsets.UTF_8));
        } catch (Exception ignored) { return new ArrayList<>(); }
    }
    static void refresh(Context context, Done callback) {
        Context app = context.getApplicationContext();
        NETWORK.execute(() -> {
            boolean ok = false;
            String message;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(SOURCE).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(10000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("Accept", "application/x-mpegURL, text/plain");
                if (conn.getResponseCode() != 200) throw new IllegalStateException("HTTP");
                byte[] data;
                try (InputStream in = conn.getInputStream()) { data = read(in); }
                List<Channel> channels = parse(new String(data, StandardCharsets.UTF_8));
                if (channels.isEmpty()) throw new IllegalStateException("Empty M3U");
                File temp = new File(app.getFilesDir(), CACHE + ".part");
                try (FileOutputStream out = new FileOutputStream(temp)) {
                    out.write(data);
                    out.getFD().sync();
                }
                if (!temp.renameTo(new File(app.getFilesDir(), CACHE)))
                    throw new IllegalStateException("Cannot save");
                ok = true;
                message = "Đã cập nhật " + channels.size() + " kênh";
            } catch (Exception e) {
                message = "Không tải được playlist mới; dùng bản đã lưu";
            } finally { if (conn != null) conn.disconnect(); }
            boolean success = ok;
            String result = message;
            new Handler(Looper.getMainLooper()).post(() ->
                    callback.completed(success, result));
        });
    }
    private static byte[] read(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int count;
        while ((count = in.read(buf)) != -1) {
            if (out.size() + count > 2_000_000)
                throw new IllegalStateException("Playlist too large");
            out.write(buf, 0, count);
        }
        return out.toByteArray();
    }
    private static String attr(String line, String key) {
        int start = line.indexOf(key + "=\"");
        if (start < 0) return "";
        start += key.length() + 2;
        int end = line.indexOf('"', start);
        return end < 0 ? "" : line.substring(start, end);
    }
    static List<Channel> parse(String text) {
        List<Channel> channels = new ArrayList<>();
        String name = null;
        String group = "";
        String normal = text.replace("\ufeff", "").replace("\r", "");
        for (String raw : normal.split("\n")) {
            String line = raw.trim();
            if (line.startsWith("#EXTINF:")) {
                int comma = line.lastIndexOf(',');
                name = comma < 0 ? "" : line.substring(comma + 1).trim();
                group = attr(line, "group-title");
            } else if (name != null && !line.startsWith("#") && !line.isEmpty()) {
                // HTTPS playlists only; ordinary Android TV network-security policy stays safe.
                if (line.startsWith("https://") && !name.isEmpty())
                    channels.add(new Channel(name, line, group));
                name = null;
                group = "";
                if (channels.size() >= 1000) break;
            }
        }
        return channels;
    }
}
