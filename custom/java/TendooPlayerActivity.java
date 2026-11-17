package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.util.List;

public final class TendooPlayerActivity extends Activity {
    private ExoPlayer player;
    private PlayerView playerView;
    private TextView banner;
    private TendooPlaylist.Channel channel;
    private List<TendooPlaylist.Channel> alternativeStreams;
    private int retryIndex;
    private String playingUrl;

    static void open(Activity from, TendooPlaylist.Channel ch) {
        Intent i = new Intent(from, TendooPlayerActivity.class);
        i.putExtra("name", ch.name);
        i.putExtra("url", ch.url);
        i.putExtra("group", ch.group);
        i.putExtra("id", ch.id);
        from.startActivity(i);
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String url = getIntent().getStringExtra("url");
        if (url == null || !url.startsWith("https://")) { finish(); return; }
        channel = new TendooPlaylist.Channel(
                getIntent().getStringExtra("name"), url,
                getIntent().getStringExtra("group") == null ? "" : getIntent().getStringExtra("group"),
                getIntent().getStringExtra("id") == null ? "" : getIntent().getStringExtra("id"));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        playerView = new PlayerView(this);
        playerView.setUseController(true);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        playerView.setControllerShowTimeoutMs(4200);
        root.addView(playerView, new FrameLayout.LayoutParams(-1, -1));
        banner = new TextView(this);
        banner.setTextColor(Color.WHITE);
        banner.setTextSize(17);
        banner.setPadding(20, 12, 20, 12);
        banner.setBackgroundColor(0x99000000);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-2, -2,
                Gravity.LEFT | Gravity.TOP);
        root.addView(banner, bp);
        setContentView(root);
        alternativeStreams = TendooPlaylist.fallbacks(this, channel);
        initializePlayer();
    }
    private void initializePlayer() {
        DefaultLoadControl load = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(3500, 25000, 1500, 2500).build();
        player = new ExoPlayer.Builder(this).setLoadControl(load).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) {
                if (player == null) return;
                if (retryIndex < alternativeStreams.size()) {
                    String fallback = alternativeStreams.get(retryIndex++).url;
                    banner.setText(channel.name + " • Thử nguồn dự phòng " + retryIndex);
                    openStream(fallback);
                } else {
                    banner.setText(channel.name + " • Nguồn hiện không phát được. Nhấn BACK.");
                    Toast.makeText(TendooPlayerActivity.this,
                            "Kênh lỗi hoặc bị giới hạn vùng / yêu cầu đăng nhập", Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY && player != null)
                    banner.setText(channel.name + " • Đang phát");
                else if (state == Player.STATE_BUFFERING && player != null)
                    banner.setText(channel.name + " • Đang đệm...");
            }
        });
        changeChannel(channel);
    }
    private void openStream(String url) {
        if (player == null || url == null || !url.startsWith("https://")) return;
        playingUrl = url;
        MediaItem item = new MediaItem.Builder().setUri(url)
                .setMimeType(MimeTypes.APPLICATION_M3U8).build();
        player.setMediaItem(item);
        player.prepare();
        player.play();
    }
    private void changeChannel(TendooPlaylist.Channel ch) {
        channel = ch;
        alternativeStreams = TendooPlaylist.fallbacks(this, ch);
        retryIndex = 0;
        banner.setText(ch.name + " • Đang mở kênh...");
        openStream(ch.url);
    }
    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN &&
                (e.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_UP
                        || e.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_DOWN)) {
            List<TendooPlaylist.Channel> list = TendooPlaylist.load(this);
            if (!list.isEmpty()) {
                int index = -1;
                for (int n = 0; n < list.size(); n++) {
                    if (list.get(n).url.equals(channel.url)) { index = n; break; }
                }
                int increment = e.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_UP ? 1 : -1;
                int next = (index + increment + list.size()) % list.size();
                changeChannel(list.get(next));
                return true;
            }
        }
        return super.dispatchKeyEvent(e);
    }
    @Override protected void onStop() {
        if (player != null) {
            playerView.setPlayer(null);
            player.release();
            player = null;
        }
        super.onStop();
    }
    @Override protected void onStart() {
        super.onStart();
        if (player == null && playerView != null) initializePlayer();
    }
}
