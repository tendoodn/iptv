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
    private final int tileW, tileH, itemH, columns, appSlots;
    TendooTileAdapter(TendooHomeActivity activity) {
        host = activity;
        columns = host.columns();
        appSlots = host.appSlots();
        float density = host.getResources().getDisplayMetrics().density;
        int width = host.getResources().getDisplayMetrics().widthPixels;
        int height = host.getResources().getDisplayMetrics().heightPixels;
        int cell = (width - host.dp(112) - host.dp(14)) / columns;
        tileW = Math.max(host.dp(74), Math.min(host.dp(180),
                cell - host.dp(10)));
        // Constrain all four rows to fit under the original large clock.
        tileH = Math.max(host.dp(45), Math.min(
                Math.round(tileW * 101f / 180f),
                (height - host.dp(171)) / 4 - host.dp(10)));
        itemH = tileH + host.dp(29);
        setHasStableIds(true);
    }
    @Override public long getItemId(int position) { return position; }
    @Override public int getItemCount() { return appSlots * 2; }

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
        FrameLayout.LayoutParams nameLp = (FrameLayout.LayoutParams) name.getLayoutParams();
        nameLp.width = tileW;
        nameLp.height = host.dp(20);
        nameLp.topMargin = tileH + host.dp(5);
        name.setLayoutParams(nameLp);
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
            h.card.setBackground(background(focused, focused ? 0xFF384152 : baseColor));
            h.card.animate().scaleX(focused ? 1.075f : 1f)
                    .scaleY(focused ? 1.075f : 1f)
                    .setDuration(115).start();
            if (h.getBindingAdapterPosition() >= columns) {
                h.caption.setVisibility(View.VISIBLE);
                h.caption.animate().alpha(focused ? 1f : 0f).setDuration(105).start();
            }
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
        h.banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
        String name;
        int color = internal ? 0xFF1E3945 : 0xFF1C1C1E;
        if (internal) {
            name = "IPTV Việt Nam";
            h.icon.setImageResource(R.drawable.tendoo_icon);
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
        h.label.setVisibility(h.banner.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        h.caption.setText(name);
        h.caption.setVisibility(h.getBindingAdapterPosition() >= columns ? View.VISIBLE : View.GONE);
        h.caption.setAlpha(0f);
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
        // The two channel rows are LOGO cards, not text labels.
        // Logos are packaged for offline/instant display and future channels
        // may fetch their tvg-logo HTTPS URL asynchronously into a disk cache.
        Drawable logo = TendooLogoCache.drawable(host, ch, () -> {
            if (!host.isFinishing() && !host.isDestroyed())
                notifyItemRangeChanged(appSlots, appSlots);
        });
        h.banner.setScaleType(ImageView.ScaleType.FIT_CENTER);
        h.banner.setImageDrawable(logo);
        h.banner.setVisibility(logo != null ? View.VISIBLE : View.GONE);
        h.icon.setVisibility(logo == null ? View.VISIBLE : View.GONE);
        if (logo == null) h.icon.setImageResource(R.drawable.ic_livetv);
        h.label.setText("");
        h.label.setVisibility(View.GONE);
        h.caption.setText(ch.name);
        h.caption.setVisibility(View.VISIBLE);
        h.caption.setAlpha(0f);
        h.card.setFocusable(true);
        h.card.setOnClickListener(v -> host.openChannel(ch));
        h.card.setOnLongClickListener(v -> {
            host.openChannels(); return true;
        });
        h.card.setOnKeyListener(null);
        installFocus(h, 0xD01C1C22);
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
        h.caption.setAlpha(0f);
        if (position < appSlots) {
            List<String> pinned = host.pins();
            if (position < pinned.size()) bindApp(h, pinned.get(position));
            else if (position == pinned.size()) bindPlus(h);
            else bindEmpty(h);
            return;
        }
        int n = appSlots * host.currentPage() + position - appSlots;
        List<TendooPlaylist.Channel> channels = host.quickChannels();
        if (n < channels.size()) bindChannel(h, channels.get(n));
        else bindEmpty(h);
    }
}
