package com.glasssix.zjb.indoorunitsample;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.glasssix.zjb.indoorunitsample.database.PreferenceAccess;
import com.glasssix.zjb.indoorunitsample.service.TcpConnectService;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;

/**
 * 这个是可视画面
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SurfaceView surfaceView;
    private Button openDoor, refuseOpenDoor;
    private LibVLC libVLC = null;
    private MediaPlayer mediaPlayer;
    ArrayList<String> options = new ArrayList<>();
    //private String url = "rtsp://admin:hk123456@192.168.0.64:554/h264/ch1/sub/av_stream";
    //private String url = "rtmp://127.0.0.1:1935/live/video";
    private String url;
    int i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        //初始化ui
        initView();
        //初始化vlc
        initVlc();
    }

    private void initView() {
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        openDoor = (Button) findViewById(R.id.openDoor);
        refuseOpenDoor = (Button) findViewById(R.id.refuseOpenDoor);
        openDoor.setOnClickListener(this);
        refuseOpenDoor.setOnClickListener(this);
        url = PreferenceAccess.getInstance().getRtspAddress();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.openDoor:
                sendCommand2TcpService("1");
                stopTcpService();
                break;
            case R.id.refuseOpenDoor:
                sendCommand2TcpService("2");
                stopTcpService();
                break;
            default:
                break;
        }

    }

    private void initVlc() {
        libVLC = new LibVLC(getApplicationContext(), options);
        try {
            if (null != mediaPlayer && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            mediaPlayer = new MediaPlayer(libVLC);
            mediaPlayer.getVLCVout().setVideoSurface(surfaceView.getHolder().getSurface(), surfaceView.getHolder());
            mediaPlayer.getVLCVout().attachViews();
            Media media = new Media(libVLC, Uri.parse(url));
            mediaPlayer.setMedia(media);
            mediaPlayer.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("=====", "onPause");
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("=====", "onResume");
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mediaPlayer && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * 关闭tcp的连接服务,并跳转到startActivity
     */
    private void stopTcpService() {
        Intent intent = new Intent(MainActivity.this, TcpConnectService.class);
        stopService(intent);
        Intent intent1 = new Intent(MainActivity.this, StartActivity.class);
        startActivity(intent1);
        finish();
    }

    /**
     * 发送开门指令到TcpConnectService
     *
     * @param command
     */
    private void sendCommand2TcpService(String command) {
        Intent intent = new Intent();
        intent.setAction(TcpConnectService.TcpDataBroadCastReceive.ACTION);
        intent.putExtra(TcpConnectService.TcpDataBroadCastReceive.KEY_BROADCAST_TYPE,
                TcpConnectService.TcpDataBroadCastReceive.TYPE_DOORCOMMAND);
        intent.putExtra(TcpConnectService.TcpDataBroadCastReceive.KEY_DOORCOMMAND, command);
        sendBroadcast(intent);
    }

}
