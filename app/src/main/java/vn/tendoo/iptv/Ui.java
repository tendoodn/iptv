package vn.tendoo.iptv;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG = Color.rgb(10, 17, 30);
    static final int PANEL = Color.rgb(23, 36, 55);
    static final int CHANNEL = Color.rgb(29, 49, 68);
    static final int LIGHT = Color.rgb(177, 194, 212);
    static final int BLUE = Color.rgb(94, 193, 255);
    static int d(Context c, int dp) {
        return (int) (c.getResources().getDisplayMetrics().density * dp + 0.5f);
    }
    static TextView text(Context c, String s, int size, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setSingleLine(false);
        return t;
    }
    static GradientDrawable fill(int color, boolean focused) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(14);
        bg.setStroke(focused ? 3 : 1, focused ? BLUE : Color.rgb(50, 67, 87));
        return bg;
    }
    static LinearLayout tile(Context c, String title, Drawable icon, int color) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setFocusable(true);
        card.setClickable(true);
        card.setPadding(d(c, 4), d(c, 2), d(c, 4), d(c, 2));
        card.setBackground(fill(color, false));
        if (icon != null) {
            ImageView image = new ImageView(c);
            image.setImageDrawable(icon);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(image, new LinearLayout.LayoutParams(d(c, 40), d(c, 40)));
        }
        TextView label = text(c, title, icon == null ? 16 : 14, Color.WHITE);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = d(c, icon == null ? 0 : 7);
        card.addView(label, params);
        card.setOnFocusChangeListener((v, focused) -> {
            v.setBackground(fill(focused ? Color.rgb(34, 76, 110) : color, focused));
        });
        return card;
    }
    static TextView button(Context c, String label) {
        TextView t = text(c, label, 15, Color.WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(d(c, 15), 0, d(c, 15), 0);
        t.setFocusable(true);
        t.setClickable(true);
        t.setBackground(fill(PANEL, false));
        t.setOnFocusChangeListener((v, focused) ->
                v.setBackground(fill(focused ? Color.rgb(34, 76, 110) : PANEL, focused)));
        return t;
    }
    static void cell(GridLayout grid, View view, int row, int col, int height) {
        Context c = grid.getContext();
        GridLayout.LayoutParams p = new GridLayout.LayoutParams(
                GridLayout.spec(row), GridLayout.spec(col, 1f));
        p.width = 0;
        p.height = d(c, height);
        p.setMargins(d(c, 5), d(c, 5), d(c, 5), d(c, 5));
        grid.addView(view, p);
    }
}
