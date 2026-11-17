package vn.tendoo.iptv;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public final class ChannelsActivity extends Activity {
    private LinearLayout content;
    private TextView summary;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.d(this, 18), Ui.d(this, 18),
                Ui.d(this, 18), Ui.d(this, 22));
        scroll.addView(content);
        setContentView(scroll);
        redraw();
    }
    private void redraw() {
        content.removeAllViews();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = Ui.text(this, "IPTV VIỆT NAM", 25, Color.WHITE);
        heading.setTypeface(null, 1);
        top.addView(heading, new LinearLayout.LayoutParams(
                0, Ui.d(this, 48), 1f));
        TextView refresh = Ui.button(this, "↻ Đồng bộ kênh");
        refresh.setOnClickListener(v -> {
            summary.setText("Đang cập nhật playlist...");
            Playlist.refresh(this, (ok, message) -> {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                redraw();
            });
        });
        top.addView(refresh, new LinearLayout.LayoutParams(-2, Ui.d(this, 43)));
        content.addView(top);
        List<Playlist.Channel> channels = Playlist.load(this);
        summary = Ui.text(this, channels.size() +
                " kênh · Chọn kênh bằng remote · Quay lại để về màn hình chính",
                13, Ui.LIGHT);
        content.addView(summary, new LinearLayout.LayoutParams(-1, Ui.d(this, 36)));

        String oldGroup = null;
        GridLayout grid = null;
        int index = 0;
        for (Playlist.Channel c : channels) {
            if (grid == null || !c.group.equals(oldGroup)) {
                oldGroup = c.group;
                TextView label = Ui.text(this,
                        c.group.isEmpty() ? "TRUYỀN HÌNH" : c.group,
                        18, Color.WHITE);
                label.setTypeface(null, 1);
                content.addView(label, new LinearLayout.LayoutParams(
                        -1, Ui.d(this, 42)));
                grid = new GridLayout(this);
                grid.setColumnCount(4);
                content.addView(grid, new LinearLayout.LayoutParams(-1, -2));
                index = 0;
            }
            LinearLayout tile = Ui.tile(this, c.name, null, Ui.CHANNEL);
            tile.setOnClickListener(v -> PlayerActivity.open(this, c));
            Ui.cell(grid, tile, index / 4, index % 4, 80);
            index++;
        }
        if (channels.isEmpty()) {
            TextView empty = Ui.text(this,
                "Chưa có kênh. Hãy kết nối Internet và nhấn Đồng bộ kênh.",
                17, Color.WHITE);
            content.addView(empty);
        }
    }
}
