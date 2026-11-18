package com.gathin.tvhome;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewOutlineProvider;
import android.graphics.Outline;
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
    private final TendooAppArtwork artwork;
    TendooTileAdapter(TendooHomeActivity activity) {
        host = activity;
        artwork = new TendooAppArtwork(host);
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
        // CleanYourTV rounds the ACTUAL image card, not only its dark background.
        // Without clipping, landscape app banners render as ugly rectangular bitmaps.
        card.setClipToOutline(true);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline out) {
                out.setRoundRect(0, 0, v.getWidth(), v.getHeight(), host.dp(13));
            }
        });
        TextView name = view.findViewById(R.id.name);
        FrameLayout.LayoutParams nameLp = (FrameLayout.LayoutParams) name.getLayoutParams();
        nameLp.width = tileW;
        nameLp.height = host.dp(20);
        nameLp.topMargin = tileH + host.dp(5);
        name.setLayoutParams(nameLp);
        name.setVisibility(View.GONE);
        return new Tile(view);
    }
    private GradientDrawable background(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(host.dp(13));
        return d;
    }

    /** White tvOS focus frame drawn above artwork; unlike a background stroke,
        it remains visible when the TV banner is opaque. */
    private GradientDrawable focusOutline() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.TRANSPARENT);
        d.setCornerRadius(host.dp(13));
        d.setStroke(host.dp(3), Color.WHITE);
        return d;
    }

    private void installFocus(Tile h, int baseColor) {
        h.card.setBackground(background(baseColor));
        h.card.setForeground(null);
        h.card.setOnFocusChangeListener((v, focused) -> {
            h.card.setForeground(focused ? focusOutline() : null);
            h.card.animate().cancel();
            h.card.animate().scaleX(focused ? 1.095f : 1f)
                    .scaleY(focused ? 1.095f : 1f)
                    .setDuration(focused ? 150 : 105).start();
            h.caption.animate().cancel();
            if (h.getBindingAdapterPosition() >= columns) {
                h.caption.setVisibility(View.VISIBLE);
                h.caption.animate().alpha(focused ? 1f : 0f).setDuration(100).start();
            }
        });
    }
    private void bindEmpty(Tile h) {
        h.banner.setVisibility(View.GONE);
        h.icon.setVisibility(View.GONE);
        h.label.setText("");
        h.label.setVisibility(View.GONE);
        h.card.setBackground(background(0x181A2B3B));
        h.card.setForeground(null);
        h.card.setFocusable(false);
        h.card.setOnClickListener(null);
        h.card.setOnLongClickListener(null);
        h.card.setOnKeyListener(null);
    }
    private void bindApp(Tile h, String packageName) {
        boolean internal = TendooHomeActivity.IPTV.equals(packageName);
        h.banner.setImageDrawable(null);
        h.icon.setImageDrawable(null);
        h.icon.setVisibility(View.GONE);
        h.banner.setVisibility(View.GONE);
        h.banner.setScaleType(ImageView.ScaleType.CENTER_CROP);

        String name;
        int color = 0xFFF5F6F8;  // CleanYourTV-style light banner cards
        Drawable wideBanner = null;
        if (internal) {
            name = "IPTV Việt Nam";
            wideBanner = host.getDrawable(R.drawable.tendoo_banner);
            color = 0xFF152941;
        } else {
            PackageManager pm = host.getPackageManager();
            try {
                name = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
            } catch (Exception e) {
                name = packageName;
            }
            // Important: the activity banner is often NOT the application banner.
            // The previous adapter only called getApplicationBanner(packageName),
            // which made YouTube/Netflix/TV apps appear as small icon+text boxes.
            wideBanner = artwork.banner(packageName);
            if (wideBanner == null) {
                Drawable icon;
                try {
                    icon = pm.getApplicationIcon(packageName);
                } catch (Exception e) {
                    icon = host.getDrawable(android.R.drawable.sym_def_app_icon);
                }
                // Fallback stays a 16:9 CARD: center a large original icon on a
                // clean, light panel. Never stretch a square icon to card width.
                h.icon.setImageDrawable(icon);
                FrameLayout.LayoutParams lp =
                        (FrameLayout.LayoutParams) h.icon.getLayoutParams();
                lp.width = Math.max(host.dp(48), Math.round(tileH * 0.68f));
                lp.height = lp.width;
                lp.gravity = android.view.Gravity.CENTER;
                lp.bottomMargin = 0;
                h.icon.setLayoutParams(lp);
                h.icon.setVisibility(View.VISIBLE);
            }
        }
        if (wideBanner != null) {
            h.banner.setImageDrawable(wideBanner);
            h.banner.setVisibility(View.VISIBLE);
            h.icon.setVisibility(View.GONE);
        }
        // Main tile remains image-first; title appears UNDER the card on focus.
        h.label.setVisibility(View.GONE);
        h.caption.setText(name);
        h.caption.setVisibility(h.getBindingAdapterPosition() >= columns
                ? View.VISIBLE : View.GONE);
        h.caption.setAlpha(0f);
        h.card.setFocusable(true);
        h.card.setOnClickListener(v -> host.openApp(packageName));
        h.card.setOnLongClickListener(v -> { host.editApp(packageName); return true; });
        h.card.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_MENU
                        || (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                            && event.getRepeatCount() == 13))) {
                host.editApp(packageName);
                return true;
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
        installFocus(h, 0xFFF4F6FA);
    }
    @Override public void onBindViewHolder(@NonNull Tile h, int position) {
        h.card.animate().cancel();
        h.card.setScaleX(1f); h.card.setScaleY(1f);
        h.card.setOnFocusChangeListener(null);
        h.card.setForeground(null);
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
