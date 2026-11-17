package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class TendooChannelsActivity extends Activity {
    private LinearLayout body;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(4 | 1024);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(9, 14, 24));
        body = new LinearLayout(this);
        body.setPadding(dp(36), dp(25), dp(36), dp(24));
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        setContentView(scroll);
        showList();
    }
    private int dp(int n) {
        return (int) (getResources().getDisplayMetrics().density * n + .5f);
    }
    private GradientDrawable surface(boolean focus) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(focus ? 0xFF314E72 : 0xFF1B293A);
        g.setStroke(dp(focus ? 3 : 1), focus ? Color.WHITE : 0xFF364357);
        return g;
    }
    private TextView text(String value, int size) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        v.setTypeface(getResources().getFont(R.font.outfit));
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }
    private void showList() {
        body.removeAllViews();
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("TRUYỀN HÌNH VIỆT NAM", 25);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(55), 1f));
        TextView update = text("↻ Cập nhật", 15);
        update.setGravity(Gravity.CENTER);
        update.setBackground(surface(false));
        update.setPadding(dp(14), 0, dp(14), 0);
        update.setFocusable(true);
        update.setOnFocusChangeListener((v, focused) -> v.setBackground(surface(focused)));
        update.setOnClickListener(v -> TendooPlaylist.refresh(this, summary -> {
            if (isDestroyed() || isFinishing()) return;
            Toast.makeText(this, summary, Toast.LENGTH_LONG).show();
            showList();
        }));
        top.addView(update, new LinearLayout.LayoutParams(-2, dp(46)));
        body.addView(top);
        List<TendooPlaylist.Channel> all = TendooPlaylist.load(this);
        TextView subtitle = text(all.size() + " kênh  ·  Chọn bằng remote  ·  BACK để trở về", 14);
        subtitle.setTextColor(0xFFAAB8CB);
        body.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(40)));
        if (all.isEmpty()) {
            body.addView(text("Danh sách trống. Kiểm tra Wi-Fi và nhấn Cập nhật.", 18));
            return;
        }
        int index = 0;
        String group = null;
        GridLayout grid = null;
        for (TendooPlaylist.Channel ch : all) {
            String actualGroup = ch.group.isEmpty() ? "Kênh truyền hình" : ch.group;
            if (grid == null || !actualGroup.equals(group)) {
                group = actualGroup;
                TextView heading = text(group, 19);
                body.addView(heading, new LinearLayout.LayoutParams(-1, dp(45)));
                grid = new GridLayout(this);
                grid.setColumnCount(4);
                body.addView(grid, new LinearLayout.LayoutParams(-1, -2));
                index = 0;
            }
            TextView tile = text(ch.name, 16);
            tile.setMaxLines(2);
            tile.setGravity(Gravity.CENTER);
            tile.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tile.setFocusable(true);
            tile.setClickable(true);
            tile.setBackground(surface(false));
            tile.setOnFocusChangeListener((v, focused) -> v.setBackground(surface(focused)));
            tile.setOnClickListener(v -> TendooPlayerActivity.open(this, ch));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(index / 4), GridLayout.spec(index % 4, 1f));
            lp.width = 0;
            lp.height = dp(68);
            lp.setMargins(dp(5), dp(5), dp(5), dp(5));
            grid.addView(tile, lp);
            index++;
        }
    }
}
