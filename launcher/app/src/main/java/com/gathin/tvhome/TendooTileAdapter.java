package com.gathin.tvhome;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/** Same item_app.xml and tvOS banners as CleanYourTV; exactly 6 tiles in each of 4 rows. */
final class TendooTileAdapter extends RecyclerView.Adapter<TendooTileAdapter.Tile> {
    private final TendooHomeActivity host;
    private final int tileW, tileH, itemH;
    TendooTileAdapter(TendooHomeActivity activity) {
        host = activity;
        float density = host.getResources().getDisplayMetrics().density;
        int width = host.getResources().getDisplayMetrics().widthPixels;
        int height = host.getResources().getDisplayMetrics().heightPixels;
        int cell = (width - host.dp(112) - host.dp(14)) / 6;
        tileW = Math.max(host.dp(74), Math.min(host.dp(180),
                cell - host.dp(10)));
        // Constrain all four rows to fit under the original large clock.
        tileH = Math.max(host.dp(45), Math.min(
                Math.round(tileW * 101f / 180f),
                (height - host.dp(171)) / 4 - host.dp(10)));
        itemH = tileH + host.dp(11);
        setHasStableIds(true);
    }
    @Override public long getItemId(int position) { return position; }
    @Override public int getItemCount() { return 24; }

    static final class Tile extends RecyclerView.ViewHolder {
        final FrameLayout card;
        final ImageView banner, icon;
        final TextView label, caption;
        Tile(View view) {
            super(view);
            card = view.findViewById(R.id.card);
            banner = view.findViewById(R.id.banner);
            icon = view.findViewById(R.id.icon);
            label = view.findViewById(R.id.label);
            caption = view.findViewById(R.id.name);
        }
    }
    @NonNull @Override public Tile onCreateViewHolder(@NonNull ViewGroup parent, int kind) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        view.setLayoutParams(new RecyclerView.LayoutParams(-1, itemH));
        FrameLayout card = view.findViewById(R.id.card);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) card.getLayoutParams();
        params.width = tileW; params.height = tileH;
        params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
        card.setLayoutParams(params);
        TextView name = view.findViewById(R.id.name);
        name.setVisibility(View.GONE);
        return new Tile(view);
    }
    private GradientDrawable background(boolean focused, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(host.dp(12));
        d.setStroke(host.dp(focused ? 3 : 1),
                focused ? Color.WHITE : 0x334D607D);
        return d;
    }
    private void installFocus(Tile h, int baseColor) {
        h.card.setBackground(background(false, baseColor));
        h.card.setOnFocusChangeListener((v, focused) -> {
            h.card.setBackground(background(focused, focused ? 0xFF3A587A : baseColor));
            h.card.animate().scaleX(focused ? 1.075f : 1f)
                    .scaleY(focused ? 1.075f : 1f)
                    .setDuration(115).start();
        });
    }
    private void bindEmpty(Tile h) {
        h.banner.setVisibility(View.GONE);
        h.icon.setVisibility(View.GONE);
        h.label.setText("");
        h.label.setVisibility(View.GONE);
        h.card.setBackground(background(false, 0x181A2B3B));
        h.card.setFocusable(false);
        h.card.setOnClickListener(null);
        h.card.setOnLongClickListener(null);
        h.card.setOnKeyListener(null);
    }
    private void bindApp(Tile h, String packageName) {
        boolean internal = TendooHomeActivity.IPTV.equals(packageName);
        h.banner.setImageDrawable(null);
        h.icon.setImageDrawable(null);
        h.icon.setVisibility(View.VISIBLE);
        h.banner.setVisibility(View.GONE);
        String name;
        int color = internal ? 0xFF1F5A79 : 0xFF1B2538;
        if (internal) {
            name = "IPTV Việt Nam";
            h.icon.setImageResource(android.R.drawable.ic_media_play);
        } else {
            PackageManager pm = host.getPackageManager();
            try {
                name = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
                Drawable banner = pm.getApplicationBanner(packageName);
                if (banner != null) {
                    h.banner.setImageDrawable(banner);
                    h.banner.setVisibility(View.VISIBLE);
                    h.icon.setVisibility(View.GONE);
                } else h.icon.setImageDrawable(pm.getApplicationIcon(packageName));
            } catch (Exception e) {
                name = packageName;
                h.icon.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        }
        h.label.setText(name);
        h.label.setVisibility(View.VISIBLE);
        h.card.setFocusable(true);
        h.card.setOnClickListener(v -> host.openApp(packageName));
        h.card.setOnLongClickListener(v -> { host.editApp(packageName); return true; });
        h.card.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_MENU
                        || (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.getRepeatCount() == 13))) {
                host.editApp(packageName); return true;
            }
            return false;
        });
        installFocus(h, color);
    }
    private void bindPlus(Tile h) {
        h.banner.setVisibility(View.GONE);
        h.icon.setImageResource(android.R.drawable.ic_input_add);
        h.icon.setVisibility(View.VISIBLE);
        h.label.setVisibility(View.VISIBLE);
        h.label.setText("+ Thêm ứng dụng");
        h.card.setFocusable(true);
        h.card.setOnClickListener(v -> host.chooseApp());
        h.card.setOnLongClickListener(null);
        h.card.setOnKeyListener(null);
        installFocus(h, 0xFF263A56);
    }
    private void bindChannel(Tile h, TendooPlaylist.Channel ch) {
        h.banner.setVisibility(View.GONE);
        h.icon.setImageResource(android.R.drawable.ic_media_play);
        h.icon.setVisibility(View.VISIBLE);
        h.label.setText(ch.name);
        h.label.setVisibility(View.VISIBLE);
        h.card.setFocusable(true);
        h.card.setOnClickListener(v -> host.openChannel(ch));
        h.card.setOnLongClickListener(v -> {
            host.openChannels(); return true;
        });
        h.card.setOnKeyListener(null);
        installFocus(h, 0xFF233D56);
    }
    @Override public void onBindViewHolder(@NonNull Tile h, int position) {
        h.card.animate().cancel();
        h.card.setScaleX(1f); h.card.setScaleY(1f);
        h.card.setOnFocusChangeListener(null);
        h.card.setOnClickListener(null);
        h.card.setOnLongClickListener(null);
        h.card.setOnKeyListener(null);
        h.banner.setImageDrawable(null);
        h.icon.setImageDrawable(null);
        h.caption.setVisibility(View.GONE);
        if (position < 12) {
            List<String> pinned = host.pins();
            if (position < pinned.size()) bindApp(h, pinned.get(position));
            else if (position == pinned.size()) bindPlus(h);
            else bindEmpty(h);
            return;
        }
        int n = 12 * host.currentPage() + position - 12;
        List<TendooPlaylist.Channel> channels = host.quickChannels();
        if (n < channels.size()) bindChannel(h, channels.get(n));
        else bindEmpty(h);
    }
}
