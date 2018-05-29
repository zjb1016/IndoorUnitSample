package com.glasssix.zjb.indoorunitsample.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import com.glasssix.zjb.indoorunitsample.StartActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * 这是在匹配房间号之后的通信服务
 */
public class TcpConnectService extends Service {
    private static final String TAG = "TcpConnectService";
    public final static String KEY_SERVICEIP = "serviceip";
    public final static String KEY_TCPPORT = "tcpport";

    private String serviceIp;
    private String tcpPort;
    private Socket socket;
    private boolean tcpIsConnect = false;
    private BindTcpSocketRunnable bindTcpSocketRunnable;
    private boolean receiving = true;

    public class TcpDataBroadCastReceive extends BroadcastReceiver {
        public static final String ACTION = "com.glasssix.zjb.indoorunitsample.tcpdata";

        public static final String KEY_BROADCAST_TYPE = "commandtype";

        //这里是接收的指令类型
        public static final int TYPE_PHONECOMMAND = 1;
        public static final int TYPE_DOORCOMMAND = 2;
        //这是数据存放的key
        public static final String KEY_PHONECOMMAND = "phonecommand";
        public static final String KEY_DOORCOMMAND = "doorcommand";

        @Override
        public void onReceive(Context context, Intent intent) {
            int type_broadCastReceive = intent.getIntExtra(KEY_BROADCAST_TYPE, 0);
            switch (type_broadCastReceive) {

                //接收到的接听指令
                case TYPE_PHONECOMMAND:
                    String command = intent.getStringExtra(KEY_PHONECOMMAND);
                    int command_ = Integer.parseInt(command);
                    Log.d(TAG, "接受到的接听指令为:    " + command_);
                    send2Socket(command_);
                    break;
                //接收到的开门指令
                case TYPE_DOORCOMMAND:
                    String command1 = intent.getStringExtra(KEY_PHONECOMMAND);
                    int command1_ = Integer.parseInt(command1);
                    Log.d(TAG, "接受到的开门指令为:    " + command1_);
                    send2Socket(command1_);
                    break;
                default:
                    break;
            }
        }
    }

    private TcpDataBroadCastReceive tcpDataBroadCastReceive = new TcpDataBroadCastReceive();

    public TcpConnectService() {
        bindTcpSocketRunnable = new BindTcpSocketRunnable();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"==tcp服务被创建了");
        //注册结果广播接收机
        registerReceiver();

        //这是接受室外机数据的线程,一开始就得开启,但是刚开始并没有进行接受
        ReceiveDataRunnable receiveDataRunnable = new ReceiveDataRunnable();
        Thread thread = new Thread(receiveDataRunnable);
        thread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceIp = intent.getStringExtra(KEY_SERVICEIP);
        tcpPort = intent.getStringExtra(KEY_TCPPORT);

        //开启tcp的连接线程
        if (bindTcpSocketRunnable != null) {
            if (!tcpIsConnect) {
                Thread thread = new Thread(bindTcpSocketRunnable);
                thread.start();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(tcpDataBroadCastReceive);
        Log.e(TAG,"==tcp服务被销毁了");
    }

    void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TcpDataBroadCastReceive.ACTION);
        registerReceiver(tcpDataBroadCastReceive, intentFilter);
    }

    /**
     * 开启一个子线程
     */
    class BindTcpSocketRunnable implements Runnable {

        @Override
        public void run() {
            bindTcpSocket();
        }
    }

    /**
     * 与室外机绑定tcp
     */
    private void bindTcpSocket() {
        int port = Integer.parseInt(tcpPort);

        try {
            socket = new Socket(serviceIp, port);
            socket.setSoTimeout(10000);
            tcpIsConnect = true;
            sendTcpStatus2Activity("1");
        } catch (IOException e) {
            e.printStackTrace();
            tcpIsConnect = false;
            sendTcpStatus2Activity("-1");
            Log.d(TAG, "bindTcpSocket" + "=====IOException");
        }
    }

    /**
     * 给室外机发送数据
     */
    private void send2Socket(int info) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            byte[] answerByte = intToByteArray(info);
            out.write(answerByte);
            Log.d(TAG, "====发送了请求" + info);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收室外机数据的线程
     */
    class ReceiveDataRunnable implements Runnable {

        @Override
        public void run() {
            while (receiving) {
                byte[] socketData = getSocketData();
                Log.d(TAG, "==tcp接收到的byte=" + socketData);
                if (null != socketData) {
                    int data = byteArrayToInt(socketData);
                    sendTcpReceiveActivity(String.valueOf(data));
                }
            }
        }
    }

    /**
     * 接收Socket服务端发来的数据
     *
     * @return
     */
    private byte[] getSocketData() {
        byte[] bytes = null;
        if (tcpIsConnect) {
            try {
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                byte[] buffer;
                buffer = new byte[dis.available()];
                if (buffer.length != 0) {
                    Log.e("length=", buffer.length + "");
                    // 读取缓冲区
                    dis.read(buffer);
                    String s = new String(buffer);
                    Log.e("-6--", s);
                    bytes = buffer;
                }

            } catch (IOException e) {
                Log.e(TAG, "=io接收数据读取异常=");
            }
            return bytes;
        } else {
            return null;
        }
    }

    /**
     * 发送tcp接受到的数据到StartActivity
     *
     * @param
     */
    private void sendTcpReceiveActivity(String tcpReceiveData) {
        Intent intent = new Intent();
        intent.setAction(StartActivity.SocketDataBroadCastReceive.ACTION);
        intent.putExtra(StartActivity.SocketDataBroadCastReceive.KEY_BROADCAST_TYPE,
                StartActivity.SocketDataBroadCastReceive.TYPE_TCPREVEIVEDATA);
        intent.putExtra(StartActivity.SocketDataBroadCastReceive.KEY_TCPREVEIVEDATA, tcpReceiveData);
        sendBroadcast(intent);
    }

    /**
     * 发送tcp连接状态到StartActivity
     *
     * @param tcpStatusData
     */
    private void sendTcpStatus2Activity(String tcpStatusData) {
        Intent intent = new Intent();
        intent.setAction(StartActivity.SocketDataBroadCastReceive.ACTION);
        intent.putExtra(StartActivity.SocketDataBroadCastReceive.KEY_BROADCAST_TYPE,
                StartActivity.SocketDataBroadCastReceive.TYPE_TCPSTATUS);
        intent.putExtra(StartActivity.SocketDataBroadCastReceive.KEY_TCPSTATUS, tcpStatusData);
        sendBroadcast(intent);
    }

    //byte 数组与 int 的相互转换
    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }
}
