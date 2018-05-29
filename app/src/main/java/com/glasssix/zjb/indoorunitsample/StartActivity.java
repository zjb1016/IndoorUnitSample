package com.glasssix.zjb.indoorunitsample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.glasssix.zjb.indoorunitsample.database.PreferenceAccess;
import com.glasssix.zjb.indoorunitsample.service.TcpConnectService;
import com.glasssix.zjb.indoorunitsample.service.UdpConnectService;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 这个是初始页面(主要的操作内容是:来电显示(接听,拒绝,直接开门))
 */
public class StartActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "StartActivity";
    private LinearLayout linearLayout;
    private Button openPhone;
    private Button closePhone;
    private Button settings;
    private EditText et_roomNumber,et_rtspAddress;
    private String serviceIp;
    private String roomNumber;
    private String becalledroomNumber;
    private String rtspAddress;
    private String tcpPort;
    private Timer beCalledTimer;
    private BeCalledTimerTask beCalledTimerTask;
    private MediaPlayer mediaPlayer;

    public class SocketDataBroadCastReceive extends BroadcastReceiver {
        public static final String ACTION = "com.glasssix.zjb.indoorunitsample.becalled";

        public static final String KEY_BROADCAST_TYPE = "type";

        //这里是接收的三种数据类型
        public static final int TYPE_BECALLEDINFO = 1;
        public static final int TYPE_TCPSTATUS = 2;
        public static final int TYPE_TCPREVEIVEDATA = 3;
        //这三种数据存放的key
        public static final String KEY_BCALLEDINFO = "becalledinfo";
        public static final String KEY_TCPSTATUS = "tcpstatus";
        public static final String KEY_TCPREVEIVEDATA = "tcpreceivedata";

        @Override
        public void onReceive(Context context, Intent intent) {
            int type = intent.getIntExtra(KEY_BROADCAST_TYPE, 0);
            switch (type) {
                case TYPE_BECALLEDINFO:
                    String becalledInfo = intent.getStringExtra(KEY_BCALLEDINFO);
                    String[] info = becalledInfo.split("/");
                    if (info.length == 3) {
                        //室外机ip地址
                        serviceIp = info[0];
                        //被叫的房间号
                        becalledroomNumber = info[1];
                        //tcp通信端口
                        tcpPort = info[2];
                    } else {
                        Log.d(TAG, "收到室外机信息,但是无法处理的数据格式");
                    }

                    Log.d(TAG, "==收到室外机信息了:  " + becalledInfo);
                    Log.d(TAG, "室外机ip地址为:  " + serviceIp + "被叫的房间号为:  " + becalledroomNumber + "tcp通信端口为:  " + tcpPort);

                    //拿自己的房间号与之匹配
                    if (becalledroomNumber.equals(roomNumber)) {
                        //匹配成功了,1:绑定tcp
                        //          2:绑定成功就响铃
                        //          3:并显示接听,拒绝两个按钮
                        Toast.makeText(StartActivity.this, "房间号匹配成功", Toast.LENGTH_SHORT).show();
                        //开启tcp服务
                        startTcpService();
                    }else {

                    }
                    break;

                case TYPE_TCPSTATUS:
                    String tcpStatus = intent.getStringExtra(KEY_TCPSTATUS);
                    if (tcpStatus.equals("1")) {
                        Toast.makeText(StartActivity.this, "tcp连接成功", Toast.LENGTH_SHORT).show();
                        //// TODO: 2018/5/18 响铃
                        bell();
                        if (beCalledTimer != null) {
                            if (beCalledTimerTask != null) {
                                //30秒之后执行
                                beCalledTimer.schedule(beCalledTimerTask, 30000, 0);
                            }
                        }
                        linearLayout.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(StartActivity.this, "tcp连接失败", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case TYPE_TCPREVEIVEDATA:
                    String tcpReceiveData = intent.getStringExtra(KEY_TCPREVEIVEDATA);
                    Log.d(TAG, "接受到的tcp数据为:   " + tcpReceiveData);
                    if (tcpReceiveData.equals("5")) {
                        //这里返回了"5",我们需要去调用rtsp流媒体
                        Intent intent1 = new Intent(StartActivity.this, MainActivity.class);
                        startActivity(intent1);
                        finish();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    SocketDataBroadCastReceive socketDataBroadCastReceive = new SocketDataBroadCastReceive();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        //开启udp服务
        startUdpService();
        //注册广播
        registerReceiver();
        //初始化ui
        initView();
        beCalledTimer = new Timer();
        beCalledTimerTask = new BeCalledTimerTask();
        mediaPlayer = new MediaPlayer();
    }

    private void startUdpService() {
        Intent intent = new Intent(StartActivity.this, UdpConnectService.class);
        startService(intent);
    }

    private void startTcpService() {
        Intent intent = new Intent(StartActivity.this, TcpConnectService.class);
        //这里需要传ip和端口过去,开启tcp服务
        intent.putExtra(TcpConnectService.KEY_SERVICEIP, serviceIp);
        intent.putExtra(TcpConnectService.KEY_TCPPORT, tcpPort);
        startService(intent);
    }

    private void initView() {
        linearLayout = (LinearLayout) findViewById(R.id.becalled_layout);
        linearLayout.setVisibility(View.INVISIBLE);
        openPhone = (Button) findViewById(R.id.openPhone);
        closePhone = (Button) findViewById(R.id.closePhone);
        settings = (Button) findViewById(R.id.bt_settings);
        openPhone.setOnClickListener(this);
        closePhone.setOnClickListener(this);
        settings.setOnClickListener(this);
        roomNumber = PreferenceAccess.getInstance().getRoomNumber();
        rtspAddress = PreferenceAccess.getInstance().getRtspAddress();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //拒绝接听
            case R.id.closePhone:
                sendCommand2TcpService("3");
                cancelTimerTask();
                break;
            //接听
            case R.id.openPhone:
                sendCommand2TcpService("4");
                cancelTimerTask();
                break;
            //设置
            case R.id.bt_settings:
                showSettingsDialog();
                break;
            default:
                break;
        }
    }

    private void showSettingsDialog() {
        LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.settings, null);
        et_roomNumber  = (EditText) view.findViewById(R.id.et_roomNumber);
        et_rtspAddress  = (EditText) view.findViewById(R.id.et_rtspAddress);
        //初始化设置ui
        et_roomNumber.setText(roomNumber);
        et_rtspAddress.setText(rtspAddress);
        AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);

        builder.setTitle("设置").setIcon(android.R.drawable.ic_dialog_info).setView(view)
                .setNegativeButton("取消", null);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                String roomNumber_ = et_roomNumber.getText().toString();
                String rtspAddress_ = et_rtspAddress.getText().toString();
                if (roomNumber_.equals("") || rtspAddress_.equals("")) {

                } else {
                    PreferenceAccess.getInstance().setRoomNumber(roomNumber_);
                    PreferenceAccess.getInstance().setRtspAddress(rtspAddress_);
                    roomNumber = roomNumber_;
                    rtspAddress = rtspAddress_;
                }
            }
        });
        builder.show();
    }

    /**
     * 这个是取消计时器
     */
    private void cancelTimerTask() {
        if (beCalledTimer != null) {
            beCalledTimer.cancel();
        }
        if (beCalledTimerTask != null) {
            beCalledTimerTask.cancel();
        }
    }

    /**
     * 这里做响铃的操作
     */
    private void bell() {
        AssetFileDescriptor fd = null;
        try {
            fd = getAssets().openFd("call.mp3");
            mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
            //设置循环播放
            mediaPlayer.setLooping(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 这是被叫的倒计时线程,30秒之后执行,无人接听,并且把接听按钮隐藏起来
     */
    private class BeCalledTimerTask extends TimerTask {

        @Override
        public void run() {
            sendCommand2TcpService("2");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    linearLayout.setVisibility(View.INVISIBLE);
                }
            });
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(socketDataBroadCastReceive);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    //注册服务端下发的消息的广播接收器
    void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SocketDataBroadCastReceive.ACTION);
        registerReceiver(socketDataBroadCastReceive, intentFilter);
    }

    /**
     * 发送接听指令到TcpConnectService
     *
     * @param command
     */
    private void sendCommand2TcpService(String command) {
        Intent intent = new Intent();
        intent.setAction(TcpConnectService.TcpDataBroadCastReceive.ACTION);
        intent.putExtra(TcpConnectService.TcpDataBroadCastReceive.KEY_BROADCAST_TYPE,
                TcpConnectService.TcpDataBroadCastReceive.TYPE_PHONECOMMAND);
        intent.putExtra(TcpConnectService.TcpDataBroadCastReceive.KEY_PHONECOMMAND, command);
        sendBroadcast(intent);
    }
}
