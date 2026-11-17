package com.gathin.tvhome;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Public playlist and independent fallback list. No background service or polling timer. */
final class TendooPlaylist {
    static final String PRIMARY = "https://raw.githubusercontent.com/tendoodn/iptv/main/playlist/vietnam.m3u";
    static final String FALLBACK = "https://iptv-org.github.io/iptv/countries/vn.m3u";
    private static final String LOCAL = "vietnam-main.m3u";
    private static final String BACKUP = "vietnam-fallback.m3u";
    private static final int LIMIT = 1024 * 1024 * 2;

    static final class Channel {
        final String name, url, group, id, logo;
        Channel(String name, String url, String group, String id) {
            this(name, url, group, id, "");
        }
        Channel(String name, String url, String group, String id, String logo) {
            this.name = name;
            this.url = url;
            this.group = group;
            this.id = id;
            this.logo = logo;
        }
    }
    interface Callback { void done(String summary); }

    static List<Channel> load(Context c) {
        List<Channel> entries = readCache(c, LOCAL);
        if (!entries.isEmpty()) return entries;
        try (InputStream in = c.getAssets().open("vietnam.m3u")) {
            entries = parse(new String(readBytes(in), StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
        if (!entries.isEmpty()) return entries;
        return readCache(c, BACKUP);
    }
    static List<Channel> fallbacks(Context c, Channel chosen) {
        List<Channel> all = new ArrayList<>();
        all.addAll(readCache(c, BACKUP));
        all.addAll(load(c));
        List<Channel> matches = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        seen.add(chosen.url);
        String key = canonical(chosen.name);
        for (Channel candidate : all) {
            if (!candidate.url.startsWith("https://")) continue;
            boolean sameId = !chosen.id.isEmpty() && chosen.id.equals(candidate.id);
            boolean sameName = canonical(candidate.name).equals(key);
            if ((sameId || sameName) && seen.add(candidate.url)) matches.add(candidate);
        }
        return matches;
    }
    static String canonical(String title) {
        return title.toLowerCase(Locale.ROOT)
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("\\[[^]]*\\]", "")
                .replaceAll("[^\\p{L}\\p{N}]", "")
                .replace("hd", "").replace("sd", "");
    }
    static void refresh(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            int ownerCount = fetchAndStore(app, PRIMARY, LOCAL);
            int backupCount = fetchAndStore(app, FALLBACK, BACKUP);
            String summary = ownerCount > 0
                    ? ("Đã cập nhật " + ownerCount + " kênh; " + backupCount + " nguồn dự phòng.")
                    : ("Không lấy được danh sách mới; dùng danh sách đã lưu. "
                    + (backupCount > 0 ? backupCount + " nguồn dự phòng đã cập nhật." : ""));
            new Handler(Looper.getMainLooper()).post(() -> callback.done(summary));
        }, "IPTV-playlist-fetch").start();
    }
    private static int fetchAndStore(Context c, String url, String filename) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(8000);
            con.setReadTimeout(10000);
            con.setInstanceFollowRedirects(true);
            con.setRequestProperty("User-Agent", "TendooIPTVHome/1.0");
            if (con.getResponseCode() != 200) return 0;
            byte[] bytes;
            try (InputStream in = con.getInputStream()) { bytes = readBytes(in); }
            List<Channel> parsed = parse(new String(bytes, StandardCharsets.UTF_8));
            if (parsed.size() < 2) return 0;
            File temp = new File(c.getFilesDir(), filename + ".new");
            try (FileOutputStream out = new FileOutputStream(temp)) {
                out.write(bytes);
                out.getFD().sync();
            }
            File target = new File(c.getFilesDir(), filename);
            if (!temp.renameTo(target)) return 0;
            return parsed.size();
        } catch (Exception ignored) { return 0; }
        finally { if (con != null) con.disconnect(); }
    }
    static List<Channel> readCache(Context c, String file) {
        try (FileInputStream in = new FileInputStream(new File(c.getFilesDir(), file))) {
            return parse(new String(readBytes(in), StandardCharsets.UTF_8));
        } catch (Exception ignored) { return new ArrayList<>(); }
    }
    private static byte[] readBytes(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            if (out.size() + n > LIMIT) throw new IllegalArgumentException("Playlist too large");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
    static String attribute(String line, String key) {
        int i = line.indexOf(key + "=\"");
        if (i < 0) return "";
        i += key.length() + 2;
        int end = line.indexOf('"', i);
        return end < 0 ? "" : line.substring(i, end);
    }
    static List<Channel> parse(String content) {
        List<Channel> list = new ArrayList<>();
        String name = null, group = "", id = "", logo = "";
        for (String raw : content.replace("\ufeff", "").replace("\r", "").split("\n")) {
            String line = raw.trim();
            if (line.startsWith("#EXTINF:")) {
                int i = line.lastIndexOf(',');
                name = i >= 0 ? line.substring(i + 1).trim() : "";
                group = attribute(line, "group-title");
                id = attribute(line, "tvg-id");
                logo = attribute(line, "tvg-logo");
            } else if (name != null && !line.isEmpty() && !line.startsWith("#")) {
                if (line.startsWith("https://") && !name.isEmpty()) {
                    list.add(new Channel(name, line, group, id, logo));
                }
                name = null; group = ""; id = ""; logo = "";
                if (list.size() >= 1000) break;
            }
        }
        return list;
    }
}
