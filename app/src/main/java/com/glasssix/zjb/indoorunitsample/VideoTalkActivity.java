package com.glasssix.zjb.indoorunitsample;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import com.rockcarry.fanplayer.MediaPlayer;
import com.rockcarry.fanplayer.playerView;

public class VideoTalkActivity extends AppCompatActivity {
    private MediaPlayer mPlayer    = null;
    private playerView mRoot      = null;
    private SurfaceView mVideo     = null;
    private SeekBar mSeek      = null;
    private ProgressBar mBuffering = null;
    private ImageView mPause     = null;
    private boolean      mIsPlaying = false;
    private boolean      mIsLive    = false;
    //private String       mURL       = "rtmp://live.hkstv.hk.lxdns.com/live/hks";
    private String       mURL       = "rtsp://admin:hk123456@192.168.0.64:554/h264/ch1/sub/av_stream";
    //private String       mURL       = "rtsp://testuser:testpass@192.168.0.118:554/live";

    private Surface mVideoSurface;
    private int          mVideoViewW;
    private int          mVideoViewH;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_talk);

        Intent intent = getIntent();
        String action = intent.getAction();
        if (intent.ACTION_VIEW.equals(action)) {
            Uri uri    = (Uri) intent.getData();
            String scheme = uri.getScheme();
            if (scheme.equals("file")) {
                mURL = uri.getPath();
            } else if (  scheme.equals("http" )
                    || scheme.equals("https")
                    || scheme.equals("rtsp" )
                    || scheme.equals("rtmp" ) ) {
                mURL = uri.toString();
            } else if (scheme.equals("content")) {
                String[] proj = { MediaStore.Images.Media.DATA };
                Cursor cursor = managedQuery(uri, proj, null, null, null);
                int    colidx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                mURL = cursor.getString(colidx);
            }
        }

        mIsLive = mURL.startsWith("http://") && mURL.endsWith(".m3u8") || mURL.startsWith("rtmp://") || mURL.startsWith("rtsp://");
        mPlayer = new MediaPlayer(mURL, mHandler, "video_hwaccel=1;video_rotate=0");

        mRoot = (playerView)findViewById(R.id.player_root);
        mRoot.setOnSizeChangedListener(new playerView.OnSizeChangedListener() {
            @Override
            public void onSizeChanged(int w, int h, int oldw, int oldh) {
                mVideo.setVisibility(View.INVISIBLE);
                mVideoViewW = w;
                mVideoViewH = h;
                mHandler.sendEmptyMessage(MSG_UDPATE_VIEW_SIZE);
            }
        });

        mVideo = (SurfaceView)findViewById(R.id.video_view);
        mVideo.getHolder().addCallback(
                new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//                  mPlayer.setDisplaySurface(holder.getSurface());
                    }

                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {
                        mVideoSurface = holder.getSurface();
                        mPlayer.setDisplaySurface(mVideoSurface);
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {
                        mVideoSurface = null;
                        mPlayer.setDisplaySurface(mVideoSurface);
                    }
                }
        );

        mSeek = (SeekBar)findViewById(R.id.seek_bar);
        mSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mPlayer.seek(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mPause = (ImageView)findViewById(R.id.btn_playpause);
        mPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testPlayerPlay(!mIsPlaying);
            }
        });

        mBuffering = (ProgressBar)findViewById(R.id.buffering);
        mBuffering.setVisibility(mIsLive ? View.VISIBLE : View.INVISIBLE);

        // show buttons with auto hide
        showUIControls(true, true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MSG_UPDATE_PROGRESS);
        mPlayer.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mIsLive) testPlayerPlay(true);
    }

    @Override
    public void onPause() {
        if (!mIsLive) testPlayerPlay(false);
        super.onPause();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            showUIControls(true, true);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void testPlayerPlay(boolean play) {
        if (play) {
            mPlayer.play();
            mIsPlaying = true;
            mPause  .setImageResource(R.drawable.icn_media_pause);
            mHandler.sendEmptyMessage(MSG_UPDATE_PROGRESS);
        } else {
            mPlayer.pause();
            mIsPlaying = false;
            mPause  .setImageResource(R.drawable.icn_media_play );
            mHandler.removeMessages  (MSG_UPDATE_PROGRESS);
        }
    }

    private void showUIControls(boolean show, boolean autohide) {
        mHandler.removeMessages(MSG_HIDE_BUTTONS);
        if (mIsLive) show = false;
        if (show) {
            mSeek .setVisibility(View.VISIBLE);
            mPause.setVisibility(View.VISIBLE);
            if (autohide) {
                mHandler.sendEmptyMessageDelayed(MSG_HIDE_BUTTONS, 5000);
            }
        }
        else {
            mSeek .setVisibility(View.INVISIBLE);
            mPause.setVisibility(View.INVISIBLE);
        }
    }

    private static final int MSG_UPDATE_PROGRESS  = 1;
    private static final int MSG_UDPATE_VIEW_SIZE = 2;
    private static final int MSG_HIDE_BUTTONS     = 3;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_PROGRESS: {
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 200);
                    int progress = (int)mPlayer.getParam(MediaPlayer.PARAM_MEDIA_POSITION);
                    if (!mIsLive) {
                        if (progress >= 0) mSeek.setProgress(progress);
                    } else {
                        mBuffering.setVisibility(View.INVISIBLE);
                        //mBuffering.setVisibility(progress == -1 ? View.VISIBLE : View.INVISIBLE);
                    }
                }
                break;
                case MSG_HIDE_BUTTONS: {
                    mSeek .setVisibility(View.INVISIBLE);
                    mPause.setVisibility(View.INVISIBLE);
                }
                break;
                case MSG_UDPATE_VIEW_SIZE: {
                    if (mPlayer.initVideoSize(mVideoViewW, mVideoViewH, mVideo)) {
                        mVideo.setVisibility(View.VISIBLE);
                    }
                }
                break;
                case MediaPlayer.MSG_OPEN_DONE: {
                    mPlayer .setDisplaySurface(mVideoSurface);
                    mVideo  .setVisibility(View.INVISIBLE);
                    mHandler.sendEmptyMessage(MSG_UDPATE_VIEW_SIZE);
                    mSeek.setMax((int)mPlayer.getParam(MediaPlayer.PARAM_MEDIA_DURATION));
                    testPlayerPlay(true);
                }
                break;
                case MediaPlayer.MSG_OPEN_FAILED: {
                    String str = String.format(getString(R.string.open_video_failed), mURL);
                    Toast.makeText(VideoTalkActivity.this, str, Toast.LENGTH_LONG).show();
                }
                break;
                case MediaPlayer.MSG_PLAY_COMPLETED: {
                    if (!mIsLive) finish();
                }
                break;
            }
        }
    };
}
