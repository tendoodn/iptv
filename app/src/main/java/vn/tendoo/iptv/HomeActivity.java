package vn.tendoo.iptv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class HomeActivity extends Activity {
    private static final String IPTV = "@iptv";
    private static final int COLS = 6;
    // 11 pinned apps + the permanent Add tile = 2 rows of 6.
    private static final int MAX_PINS = 11;
    private SharedPreferences prefs;
    private GridLayout appGrid, channelGrid;
    private TextView pageText, status;
    private List<Playlist.Channel> channels = new ArrayList<>();
    private int page;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefs = getSharedPreferences("home", MODE_PRIVATE);
        channels = Playlist.load(this);
        drawScreen();
        drawApps();
        drawChannels();
        if (System.currentTimeMillis() - prefs.getLong("lastPlaylistCheck", 0L)
                > 4L * 60 * 60 * 1000L) updateChannels(false);
    }
    @Override public void onResume() {
        super.onResume();
        if (appGrid != null) drawApps();
    }
    private void drawScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.d(this, 18), Ui.d(this, 14), Ui.d(this, 18), Ui.d(this, 12));
        scroll.addView(body);
        setContentView(scroll);

        LinearLayout header = bar();
        TextView heading = Ui.text(this, "TENDOO IPTV HOME", 24, Color.WHITE);
        heading.setTypeface(null, 1);
        header.addView(heading, new LinearLayout.LayoutParams(0, Ui.d(this, 45), 1f));
        TextView edit = Ui.button(this, "☷ Quản lý");
        edit.setOnClickListener(v -> manage());
        header.addView(edit, new LinearLayout.LayoutParams(-2, Ui.d(this, 43)));
        body.addView(header);

        TextView label = Ui.text(this, "ỨNG DỤNG  ·  Giữ OK để đổi vị trí hoặc gỡ ô", 15, Ui.LIGHT);
        body.addView(label, new LinearLayout.LayoutParams(-1, Ui.d(this, 34)));
        appGrid = new GridLayout(this);
        appGrid.setColumnCount(COLS);
        appGrid.setRowCount(2);
        body.addView(appGrid, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout channelsHeader = bar();
        TextView quick = Ui.text(this, "TRUYỀN HÌNH  ·  XEM NHANH", 17, Color.WHITE);
        quick.setTypeface(null, 1);
        channelsHeader.addView(quick, new LinearLayout.LayoutParams(0, Ui.d(this, 47), 1f));
        TextView all = Ui.button(this, "Tất cả kênh");
        all.setOnClickListener(v -> startActivity(new Intent(this, ChannelsActivity.class)));
        channelsHeader.addView(all, new LinearLayout.LayoutParams(-2, Ui.d(this, 40)));
        TextView previous = Ui.button(this, "◀");
        previous.setOnClickListener(v -> { page--; drawChannels(); });
        LinearLayout.LayoutParams controlLp =
                new LinearLayout.LayoutParams(Ui.d(this, 54), Ui.d(this, 40));
        controlLp.leftMargin = Ui.d(this, 6);
        channelsHeader.addView(previous, controlLp);

        pageText = Ui.text(this, "1/1", 14, Ui.LIGHT);
        pageText.setGravity(Gravity.CENTER);
        channelsHeader.addView(pageText,
                new LinearLayout.LayoutParams(Ui.d(this, 55), Ui.d(this, 40)));

        TextView next = Ui.button(this, "▶");
        next.setOnClickListener(v -> { page++; drawChannels(); });
        channelsHeader.addView(next, new LinearLayout.LayoutParams(Ui.d(this, 54), Ui.d(this, 40)));
        TextView reload = Ui.button(this, "↻");
        reload.setContentDescription("Cập nhật playlist");
        reload.setOnClickListener(v -> updateChannels(true));
        LinearLayout.LayoutParams reloadLp =
                new LinearLayout.LayoutParams(Ui.d(this, 58), Ui.d(this, 40));
        reloadLp.leftMargin = Ui.d(this, 6);
        channelsHeader.addView(reload, reloadLp);
        body.addView(channelsHeader);

        channelGrid = new GridLayout(this);
        channelGrid.setColumnCount(COLS);
        channelGrid.setRowCount(2);
        body.addView(channelGrid, new LinearLayout.LayoutParams(-1, -2));
        status = Ui.text(this, "Playlist: " + Playlist.SOURCE, 12, Ui.LIGHT);
        status.setSingleLine(true);
        status.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        body.addView(status, new LinearLayout.LayoutParams(-1, Ui.d(this, 30)));
    }
    private LinearLayout bar() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }
    private List<String> pinned() {
        List<String> items = new ArrayList<>();
        String raw = prefs.getString("apps", null);
        if (raw == null) {
            items.add(IPTV);
            return items;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String pkg = array.optString(i);
                if (!pkg.isEmpty() && !items.contains(pkg) && items.size() < MAX_PINS)
                    items.add(pkg);
            }
        } catch (Exception ignored) { items.add(IPTV); }
        return items;
    }
    private void store(List<String> apps) {
        JSONArray out = new JSONArray();
        for (String app : apps) out.put(app);
        prefs.edit().putString("apps", out.toString()).apply();
        drawApps();
    }
    private String titleOf(String pkg) {
        if (IPTV.equals(pkg)) return "IPTV Việt Nam";
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception ignored) { return pkg; }
    }
    private Drawable iconOf(String pkg) {
        if (IPTV.equals(pkg)) return getDrawable(android.R.drawable.ic_media_play);
        try { return getPackageManager().getApplicationIcon(pkg); }
        catch (Exception ignored) { return getDrawable(android.R.drawable.sym_def_app_icon); }
    }
    private void drawApps() {
        if (appGrid == null) return;
        appGrid.removeAllViews();
        List<String> apps = pinned();
        for (int i = 0; i < 12; i++) {
            if (i < apps.size()) {
                String pkg = apps.get(i);
                LinearLayout tile = Ui.tile(this, titleOf(pkg), iconOf(pkg), Ui.PANEL);
                tile.setOnClickListener(v -> launch(pkg));
                tile.setOnLongClickListener(v -> { editTile(pkg); return true; });
                tile.setOnKeyListener((v, key, event) -> {
                    if (event.getAction() == 1 &&
                            key == android.view.KeyEvent.KEYCODE_MENU) {
                        editTile(pkg);
                        return true;
                    }
                    return false;
                });
                Ui.cell(appGrid, tile, i / COLS, i % COLS, 93);
            } else if (i == apps.size()) {
                LinearLayout add = Ui.tile(this, "+ Thêm ứng dụng",
                        getDrawable(android.R.drawable.ic_input_add), Ui.CHANNEL);
                add.setOnClickListener(v -> appPicker());
                Ui.cell(appGrid, add, i / COLS, i % COLS, 93);
            } else {
                Ui.cell(appGrid, new View(this), i / COLS, i % COLS, 93);
            }
        }
    }
    private void launch(String pkg) {
        if (IPTV.equals(pkg)) {
            startActivity(new Intent(this, ChannelsActivity.class));
            return;
        }
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) intent = pm.getLaunchIntentForPackage(pkg);
        if (intent != null) {
            try { startActivity(intent); return; }
            catch (Exception ignored) { }
        }
        Toast.makeText(this, "Không thể mở " + titleOf(pkg),
                Toast.LENGTH_LONG).show();
    }
    private void editTile(String pkg) {
        List<String> apps = pinned();
        int index = apps.indexOf(pkg);
        if (index < 0) return;
        String[] menu = {"Di chuyển qua trái / lên", "Di chuyển qua phải / xuống",
                "Chọn vị trí 1–11", "Gỡ khỏi màn hình", "Mở"};
        new AlertDialog.Builder(this).setTitle(titleOf(pkg)).setItems(menu, (d, which) -> {
            if (which == 3) {
                apps.remove(index); store(apps);
            } else if (which == 4) {
                launch(pkg);
            } else if (which == 0 && index > 0) {
                Collections.swap(apps, index, index - 1); store(apps);
            } else if (which == 1 && index < apps.size() - 1) {
                Collections.swap(apps, index, index + 1); store(apps);
            } else if (which == 2) {
                String[] positions = new String[apps.size()];
                for (int i = 0; i < apps.size(); i++)
                    positions[i] = "Ô " + (i + 1) + " · " + titleOf(apps.get(i));
                new AlertDialog.Builder(this).setTitle("Chuyển " + titleOf(pkg))
                    .setItems(positions, (dialog, selected) -> {
                        apps.remove(index);
                        apps.add(selected, pkg);
                        store(apps);
                    }).show();
            }
        }).show();
    }
    private void appPicker() {
        List<String> existing = pinned();
        if (existing.size() >= MAX_PINS) {
            Toast.makeText(this, "Tối đa 11 ứng dụng + dấu +. Hãy gỡ một ô trước.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        PackageManager pm = getPackageManager();
        Set<String> packages = new LinkedHashSet<>();
        if (!existing.contains(IPTV)) packages.add(IPTV);
        for (String category : new String[] {
                Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER }) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(category);
            List<ResolveInfo> result = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo r : result)
                if (r.activityInfo != null && !r.activityInfo.packageName.equals(getPackageName())
                        && !existing.contains(r.activityInfo.packageName))
                    packages.add(r.activityInfo.packageName);
        }
        List<String> options = new ArrayList<>(packages);
        options.sort(Comparator.comparing(this::titleOf, String.CASE_INSENSITIVE_ORDER));
        if (options.isEmpty()) {
            Toast.makeText(this, "Không có ứng dụng nào khác", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] titles = new String[options.size()];
        for (int i = 0; i < options.size(); i++) titles[i] = titleOf(options.get(i));
        new AlertDialog.Builder(this).setTitle("Thêm ứng dụng lên màn hình chính")
                .setItems(titles, (d, which) -> {
                    List<String> next = pinned();
                    String pkg = options.get(which);
                    if (next.size() < MAX_PINS && !next.contains(pkg)) {
                        next.add(pkg); store(next);
                    }
                }).setNegativeButton("Hủy", null).show();
    }
    private void manage() {
        String[] options = {"Thêm ứng dụng", "Sắp xếp / gỡ ứng dụng",
                "Cài làm launcher mặc định", "Cập nhật IPTV", "Mở toàn bộ kênh"};
        new AlertDialog.Builder(this).setTitle("Tendoo IPTV Home")
                .setItems(options, (d, which) -> {
                    if (which == 0) appPicker();
                    if (which == 1) {
                        List<String> apps = pinned();
                        String[] labels = new String[apps.size()];
                        for (int i = 0; i < apps.size(); i++) labels[i] = titleOf(apps.get(i));
                        new AlertDialog.Builder(this).setTitle("Chỉnh ô ứng dụng")
                            .setItems(labels, (x, i) -> editTile(apps.get(i))).show();
                    }
                    if (which == 2) {
                        try { startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)); }
                        catch (Exception e) { Toast.makeText(this,
                          "Nhấn HOME rồi chọn Tendoo IPTV Home → Luôn dùng.",
                          Toast.LENGTH_LONG).show(); }
                    }
                    if (which == 3) updateChannels(true);
                    if (which == 4) startActivity(new Intent(this, ChannelsActivity.class));
                }).show();
    }
    private void drawChannels() {
        if (channelGrid == null) return;
        channelGrid.removeAllViews();
        int totalPages = Math.max(1, (channels.size() + 11) / 12);
        page = Math.max(0, Math.min(page, totalPages - 1));
        pageText.setText((page + 1) + "/" + totalPages);
        for (int index = 0; index < 12; index++) {
            int n = page * 12 + index;
            if (n < channels.size()) {
                Playlist.Channel ch = channels.get(n);
                LinearLayout tile = Ui.tile(this, ch.name, null, Ui.CHANNEL);
                tile.setOnClickListener(v -> PlayerActivity.open(this, ch));
                Ui.cell(channelGrid, tile, index / COLS, index % COLS, 83);
            } else {
                Ui.cell(channelGrid, new View(this), index / COLS, index % COLS, 83);
            }
        }
    }
    private void updateChannels(boolean manual) {
        prefs.edit().putLong("lastPlaylistCheck", System.currentTimeMillis()).apply();
        status.setText("Đang tải playlist từ GitHub...");
        Playlist.refresh(this, (ok, message) -> {
            if (isFinishing() || isDestroyed()) return;
            channels = Playlist.load(this);
            drawChannels();
            status.setText(message + "  ·  " + Playlist.SOURCE);
            if (manual) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }
}
