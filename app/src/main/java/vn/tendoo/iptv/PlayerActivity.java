package vn.tendoo.iptv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public final class PlayerActivity extends Activity implements SurfaceHolder.Callback {
    private MediaPlayer player;
    private SurfaceHolder surface;
    private TextView hint;
    private String url, title;
    private boolean surfaceReady = false;
    private boolean active = false;

    static void open(Activity from, Playlist.Channel channel) {
        Intent intent = new Intent(from, PlayerActivity.class);
        intent.putExtra("url", channel.url);
        intent.putExtra("title", channel.name);
        from.startActivity(intent);
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        url = getIntent().getStringExtra("url");
        title = getIntent().getStringExtra("title");
        if (url == null || !url.startsWith("https://")) { finish(); return; }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        SurfaceView video = new SurfaceView(this);
        video.getHolder().addCallback(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1));
        hint = Ui.text(this, title + "  •  Đang kết nối…", 17, Color.WHITE);
        hint.setPadding(Ui.d(this, 15), 0, Ui.d(this, 15), 0);
        hint.setBackgroundColor(0xA0000000);
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
                -2, Ui.d(this, 45), Gravity.TOP | Gravity.LEFT);
        root.addView(hint, hintParams);
        setContentView(root);
    }
    @Override protected void onStart() {
        super.onStart();
        active = true;
        if (surfaceReady && player == null) startStream();
    }
    @Override protected void onStop() {
        active = false;
        stopStream();
        super.onStop();
    }
    private void startStream() {
        if (!active || !surfaceReady || url == null) return;
        stopStream();
        hint.setText(title + "  •  Đang kết nối…");
        try {
            MediaPlayer next = new MediaPlayer();
            player = next;
            next.setAudioStreamType(AudioManager.STREAM_MUSIC);
            next.setDisplay(surface);
            next.setScreenOnWhilePlaying(true);
            next.setOnPreparedListener(mp -> {
                if (player != mp || !active) return;
                hint.setText(title + "  •  CH ▲▼ đổi kênh");
                mp.start();
                hint.postDelayed(() -> {
                    if (player == mp && active && hint != null) hint.setText(title);
                }, 2500);
            });
            next.setOnErrorListener((mp, what, extra) -> {
                if (player == mp && hint != null) {
                    hint.setText(title + "  •  Kênh lỗi hoặc bị chặn mạng. BACK để chọn kênh khác.");
                    Toast.makeText(this, "Không phát được luồng HLS", Toast.LENGTH_LONG).show();
                }
                return true;
            });
            next.setOnInfoListener((mp, what, extra) -> {
                if (player == mp && what == MediaPlayer.MEDIA_INFO_BUFFERING_START)
                    hint.setText(title + "  •  Đang tải dữ liệu…");
                if (player == mp && what == MediaPlayer.MEDIA_INFO_BUFFERING_END)
                    hint.setText(title);
                return false;
            });
            next.setDataSource(url);
            next.prepareAsync();
        } catch (Exception e) {
            stopStream();
            hint.setText(title + "  •  Không kết nối được luồng phát.");
        }
    }
    private void stopStream() {
        MediaPlayer last = player;
        player = null;
        if (last == null) return;
        try { last.reset(); } catch (Exception ignored) { }
        try { last.release(); } catch (Exception ignored) { }
    }
    @Override public void surfaceCreated(SurfaceHolder holder) {
        surface = holder;
        surfaceReady = true;
        if (active) startStream();
    }
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surface = holder;
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stopStream();
    }
    private void nextChannel(int step) {
        List<Playlist.Channel> channels = Playlist.load(this);
        if (channels.isEmpty()) return;
        int index = -1;
        for (int i = 0; i < channels.size(); i++) {
            if (channels.get(i).url.equals(url)) { index = i; break; }
        }
        int n = (index + step + channels.size()) % channels.size();
        Playlist.Channel c = channels.get(n);
        url = c.url; title = c.name;
        startStream();
    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            nextChannel(1);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            nextChannel(-1);
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }
}
