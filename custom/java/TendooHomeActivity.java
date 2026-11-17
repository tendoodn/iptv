package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Original CleanYourTV visuals/resources, with a purpose-built 6x4 app and IPTV layout. */
public final class TendooHomeActivity extends Activity {
    static final String IPTV = "@iptv";
    // Owner's CleanYourTV reference has five tiles across. Six is optional.
    private int columns = 5;
    int columns() { return columns; }
    int appSlots() { return columns * 2; }
    int maxPins() { return appSlots() - 1; }

    private SharedPreferences prefs;
    private RecyclerView grid;
    private TendooTileAdapter adapter;
    private List<String> pinned = new ArrayList<>();
    private List<TendooPlaylist.Channel> channels = new ArrayList<>();
    private int channelPage;
    private TextView greeting;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_main); // the real CleanYourTV tvOS screen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        Config config = Config.load(this);
        FrameLayout root = findViewById(R.id.root);
        root.setBackgroundColor(0xFF0B0E16);
        // Same warm red-to-charcoal cinematic backdrop as the owner's CleanYourTV TV.
        // A single static native gradient costs no video decoding, images or animations.
        View ambientRed = new View(this);
        GradientDrawable glow = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFA82C27, 0xFF4E2025, 0xFF0B0E16});
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        glow.setGradientCenter(0.31f, 0.79f);
        glow.setGradientRadius(getResources().getDisplayMetrics().widthPixels * 0.69f);
        ambientRed.setBackground(glow);
        root.addView(ambientRed, 1, new FrameLayout.LayoutParams(-1, -1));
        SkyView sky = findViewById(R.id.sky);
        sky.setIntensity(0);
        // These expensive cinematic layers are intentionally off by default for slow TV boxes.
        findViewById(R.id.hero).setVisibility(View.GONE);
        findViewById(R.id.heroArt).setVisibility(View.GONE);
        findViewById(R.id.heroScrim).setVisibility(View.GONE);
        findViewById(R.id.ambient).setVisibility(View.GONE);
        findViewById(R.id.glass).setVisibility(View.GONE);
        findViewById(R.id.racePanel).setVisibility(View.GONE);
        findViewById(R.id.stats).setVisibility(View.GONE);

        greeting = findViewById(R.id.greeting);
        greeting.setText("XIN CHÀO");
        TextView inputs = findViewById(R.id.btnInputs);
        inputs.setText(getString(R.string.inputs)); // original CleanYourTV 'Nguồn vào' pill
        inputs.setOnClickListener(v -> showSources());
        TextView network = findViewById(R.id.btnWifi);
        network.setText(getString(R.string.wifi)); // restore stock CleanYourTV network pill
        network.setOnClickListener(v -> launchSystem(Settings.ACTION_WIFI_SETTINGS));
        network.setOnLongClickListener(v -> { requestDefaultHome(); return true; });
        View arrange = findViewById(R.id.btnReorder);
        arrange.setVisibility(View.VISIBLE);
        arrange.setOnClickListener(v -> manage());
        View settings = findViewById(R.id.btnSettings);
        settings.setOnClickListener(v -> launchSystem(Settings.ACTION_SETTINGS));
        settings.setOnLongClickListener(v -> { manage(); return true; });

        prefs = getSharedPreferences("iptv-home-layout-v2", MODE_PRIVATE);
        columns = Math.max(5, Math.min(6, prefs.getInt("columns", 5)));
        pinned = loadPins();
        channels = TendooPlaylist.load(this);

        grid = findViewById(R.id.grid);
        grid.setClipToPadding(false);
        grid.setPadding(0, dp(62), 0, dp(12));
        grid.setLayoutManager(new GridLayoutManager(this, columns));
        // The original CleanYourTV translucent rounded band behind the pinned dock.
        grid.addItemDecoration(new DockDecoration(columns,
                getResources().getDisplayMetrics().density, true));
        grid.setHasFixedSize(true);
        grid.setItemAnimator(null);
        adapter = new TendooTileAdapter(this);
        grid.setAdapter(adapter);
        TendooLogoCache.preload(this, () -> {
            if (!isDestroyed() && !isFinishing()) adapter.notifyItemRangeChanged(appSlots(), appSlots());
        });
        grid.post(() -> {
            if (grid.getChildCount() > 0) {
                View first = grid.getLayoutManager().findViewByPosition(0);
                if (first != null) first.requestFocus();
            }
        });
        if (System.currentTimeMillis() - prefs.getLong("sync", 0) > 4L * 3600_000L) sync(false);
        // Explain and offer the system HOME chooser once; never change HOME silently.
        if (!prefs.getBoolean("home_prompt_shown_v3", false)) {
            prefs.edit().putBoolean("home_prompt_shown_v3", true).apply();
            grid.post(() -> new AlertDialog.Builder(this)
                .setTitle("Đặt Tendoo IPTV làm màn hình chính?")
                .setMessage("Ứng dụng có thể thay màn hình HOME gốc khi TV cho phép. Launcher gốc vẫn được giữ để khôi phục. Anh muốn chọn Tendoo IPTV Home làm HOME mặc định?")
                .setPositiveButton("Chọn làm HOME", (d, which) -> requestDefaultHome())
                .setNegativeButton("Để sau", null).show());
        }
    }
    int dp(int d) {
        return (int) (getResources().getDisplayMetrics().density * d + .5f);
    }
    List<String> pins() { return pinned; }
    List<TendooPlaylist.Channel> quickChannels() { return channels; }
    int currentPage() { return channelPage; }
    private List<String> loadPins() {
        List<String> values = new ArrayList<>();
        String raw = prefs.getString("apps", null);
        if (raw == null) {
            values.add(IPTV);
            return values;
        }
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) {
                String p = a.optString(i, "");
                if (!p.isEmpty() && !values.contains(p) && values.size() < maxPins()) values.add(p);
            }
        } catch (Exception ex) {
            values.add(IPTV);
        }
        return values;
    }
    private void save() {
        JSONArray a = new JSONArray();
        for (String p : pinned) a.put(p);
        prefs.edit().putString("apps", a.toString()).apply();
        adapter.notifyDataSetChanged();
    }
    private String appName(String pkg) {
        if (IPTV.equals(pkg)) return "IPTV Việt Nam";
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception ex) { return pkg; }
    }
    private void showSources() {
        TvInputManager inputManager = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        if (inputManager == null) {
            openChannels();
            return;
        }
        List<TvInputInfo> sources = inputManager.getTvInputList();
        if (sources == null || sources.isEmpty()) {
            openChannels();
            return;
        }
        String[] labels = new String[sources.size() + 1];
        labels[0] = "IPTV Việt Nam";
        for (int i = 0; i < sources.size(); i++) {
            CharSequence name = sources.get(i).loadLabel(getPackageManager());
            labels[i + 1] = name == null ? "HDMI / TV " + (i + 1) : name.toString();
        }
        new AlertDialog.Builder(this).setTitle("NGUỒN VÀO")
            .setItems(labels, (dialog, which) -> {
                if (which == 0) { openChannels(); return; }
                try {
                    Intent source = new Intent(Intent.ACTION_VIEW,
                        TvContract.buildChannelUriForPassthroughInput(sources.get(which - 1).getId()));
                    startActivity(source);
                } catch (Exception ex) {
                    Toast.makeText(this,"Không mở được nguồn vào này.",Toast.LENGTH_SHORT).show();
                }
            }).show();
    }

    private void launchSystem(String action) {
        try { startActivity(new Intent(action)); }
        catch (Exception ex) {
            Toast.makeText(this, "Thiết bị không hỗ trợ mục này", Toast.LENGTH_SHORT).show();
        }
    }
    private void requestDefaultHome() {
        // Android 10+ exposes a guarded user-confirmed HOME role picker.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    Toast.makeText(this, "Tendoo IPTV Home đang là HOME mặc định.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    startActivityForResult(
                            roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME), 1003);
                    return;
                } catch (Exception ignored) { }
            }
        }
        // Android TV boxes and older firmware may only expose the Home-app settings.
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (Exception ex) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
            } catch (Exception ignored) {
                new AlertDialog.Builder(this).setTitle("Thiết bị hạn chế đổi HOME")
                    .setMessage("Vào Cài đặt → Ứng dụng → Ứng dụng mặc định → Ứng dụng màn hình chính. Nếu TV không có mục này, firmware có thể khóa launcher gốc.")
                    .setPositiveButton("Đã hiểu", null).show();
            }
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1003) {
            Toast.makeText(this, resultCode == RESULT_OK
                        ? "Đã chọn Tendoo IPTV Home làm launcher mặc định."
                        : "Chưa đổi launcher. Anh vẫn có thể vào Cài đặt để chọn.",
                    Toast.LENGTH_LONG).show();
        }
    }

    void openApp(String pkg) {
        if (IPTV.equals(pkg)) { openChannels(); return; }
        PackageManager pm = getPackageManager();
        Intent target = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (target == null) target = pm.getLaunchIntentForPackage(pkg);
        if (target == null) {
            Toast.makeText(this, "Ứng dụng đã gỡ hoặc không khởi chạy được", Toast.LENGTH_LONG).show();
            return;
        }
        try { startActivity(target); }
        catch (Exception e) {
            Toast.makeText(this, "Không thể mở " + appName(pkg), Toast.LENGTH_SHORT).show();
        }
    }
    void openChannels() { startActivity(new Intent(this, TendooChannelsActivity.class)); }
    void openChannel(TendooPlaylist.Channel channel) {
        TendooPlayerActivity.open(this, channel);
    }
    void chooseApp() {
        if (pinned.size() >= maxPins()) {
            Toast.makeText(this, "Hàng 1–2 đã đầy: gỡ một ô trước.", Toast.LENGTH_LONG).show();
            return;
        }
        Set<String> names = new LinkedHashSet<>();
        if (!pinned.contains(IPTV)) names.add(IPTV);
        PackageManager pm = getPackageManager();
        for (String cat : new String[] {
                Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER }) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
                if (info.activityInfo == null) continue;
                String pkg = info.activityInfo.packageName;
                if (!pkg.equals(getPackageName()) && !pinned.contains(pkg)) names.add(pkg);
            }
        }
        List<String> available = new ArrayList<>(names);
        available.sort(Comparator.comparing(this::appName, String.CASE_INSENSITIVE_ORDER));
        String[] labels = new String[available.size()];
        for (int i = 0; i < available.size(); i++) labels[i] = appName(available.get(i));
        if (labels.length == 0) {
            Toast.makeText(this, "Không còn ứng dụng có thể thêm", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("THÊM ỨNG DỤNG")
                .setItems(labels, (d, i) -> {
                    String pkg = available.get(i);
                    if (!pinned.contains(pkg) && pinned.size() < maxPins()) {
                        pinned.add(pkg);
                        save();
                    }
                }).setNegativeButton("Hủy", null).show();
    }
    void editApp(String pkg) {
        int index = pinned.indexOf(pkg);
        if (index < 0) return;
        String[] options = {"Chuyển sang trái", "Chuyển sang phải",
                "Chọn vị trí", "Gỡ ô khỏi màn hình (không gỡ app)", "Mở ứng dụng"};
        new AlertDialog.Builder(this).setTitle(appName(pkg)).setItems(options,
                (d, which) -> {
                    if (which == 0 && index > 0) {
                        Collections.swap(pinned, index, index-1); save();
                    } else if (which == 1 && index < pinned.size()-1) {
                        Collections.swap(pinned, index, index+1); save();
                    } else if (which == 2) {
                        String[] choices = new String[pinned.size()];
                        for (int n = 0; n < choices.length; n++)
                            choices[n] = "Ô " + (n + 1) + ": " + appName(pinned.get(n));
                        new AlertDialog.Builder(this).setTitle("CHỌN VỊ TRÍ")
                                .setItems(choices, (dialog, newIndex) -> {
                                    pinned.remove(index);
                                    pinned.add(newIndex, pkg);
                                    save();
                                }).show();
                    } else if (which == 3) {
                        pinned.remove(index); save();
                    } else if (which == 4) openApp(pkg);
                }).setNegativeButton("Đóng", null).show();
    }
    void manage() {
        new AlertDialog.Builder(this).setTitle("TÙY CHỈNH MÀN HÌNH CHÍNH")
                .setItems(new String[]{"Thêm ứng dụng", "Sắp xếp / gỡ ứng dụng",
                        "Xem tất cả kênh", "Kênh tiếp theo", "Kênh trước",
                        "Cập nhật IPTV từ GitHub", "Cài đặt HOME mặc định",
                        "Số cột mỗi hàng (5 / 6)"},
                        (dialog, option) -> {
                            switch (option) {
                                case 0: chooseApp(); break;
                                case 1: chooseEdit(); break;
                                case 2: openChannels(); break;
                                case 3: turnPage(1); break;
                                case 4: turnPage(-1); break;
                                case 5: sync(true); break;
                                case 6: requestDefaultHome(); break;
                                case 7: changeColumnCount(); break;
                                default: break;
                            }
                        }).show();
    }
    private void changeColumnCount() {
        new AlertDialog.Builder(this).setTitle("SỐ Ô MỖI HÀNG")
            .setSingleChoiceItems(new String[]{"5 ô — giống TV trong ảnh", "6 ô — nhiều ứng dụng hơn"},
                columns == 5 ? 0 : 1, (dialog, which) -> {
                    int next = which == 0 ? 5 : 6;
                    dialog.dismiss();
                    if (next != columns) {
                        prefs.edit().putInt("columns", next).apply();
                        recreate();
                    }
                }).setNegativeButton("Đóng", null).show();
    }

    private void chooseEdit() {
        String[] choices = new String[pinned.size()];
        for (int i = 0; i < choices.length; i++) choices[i] = appName(pinned.get(i));
        new AlertDialog.Builder(this).setTitle("CHỌN ỨNG DỤNG")
                .setItems(choices, (d, n) -> editApp(pinned.get(n))).show();
    }
    private void turnPage(int step) {
        int pages = Math.max(1, (channels.size() + appSlots() - 1) / appSlots());
        channelPage = (channelPage + step + pages) % pages;
        adapter.notifyItemRangeChanged(appSlots(), appSlots());
        Toast.makeText(this, "Kênh xem nhanh: trang " + (channelPage + 1)
                + "/" + pages, Toast.LENGTH_SHORT).show();
    }
    private void sync(boolean requested) {
        prefs.edit().putLong("sync", System.currentTimeMillis()).apply();
        TendooPlaylist.refresh(this, msg -> {
            if (isFinishing() || isDestroyed()) return;
            channels = TendooPlaylist.load(this);
            channelPage = 0;
            adapter.notifyItemRangeChanged(appSlots(), appSlots());
            if (requested) Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });
    }
    @Override protected void onResume() {
        super.onResume();
        if (adapter != null) adapter.notifyItemRangeChanged(0, appSlots());
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && (event.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_UP
                    || event.getKeyCode() == KeyEvent.KEYCODE_PAGE_DOWN)) {
            turnPage(1); return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && (event.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_DOWN
                    || event.getKeyCode() == KeyEvent.KEYCODE_PAGE_UP)) {
            turnPage(-1); return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
