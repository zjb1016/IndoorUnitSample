package com.glasssix.zjb.indoorunitsample.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.glasssix.zjb.indoorunitsample.StartActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * 这是一直运行的,用来监听是否被呼叫的一个服务
 */
public class UdpConnectService extends Service {
    private static final String TAG = "UdpConnectService";
    private int servicePort = 4001;
    private boolean receiving = true;
    private DatagramSocket socket;
    private final ConnectWork connectWork;
    private Thread thread;

    public UdpConnectService() {
        connectWork = new ConnectWork();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (thread == null){
            thread = new Thread(connectWork);
            thread.start();
        }else {
            Log.d(TAG,"====UDP连接线程已开启");
        }
        Log.d(TAG,"====UdpConnectService开启了");
//        return super.onStartCommand(intent, flags, startId);
        return START_STICKY;//服务被销毁后自动启动
    }

    /**
     * 这是一个关键的线程,主要是接收室外机发来的信息(室外机ip+房间号+tcp通信端口)
     */
    public class ConnectWork implements Runnable {

        @Override
        public void run() {
            try {
                socket = new DatagramSocket(servicePort);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            while (receiving) {
                byte[] data = new byte[1024 * 1024];
                DatagramPacket recpacket = new DatagramPacket(data, data.length);
                try {
//                    lock.acquire();
                    socket.receive(recpacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //这是接受到的数据
                String recString = new String(recpacket.getData(), 0, recpacket.getLength());
                recString = recString.trim();
                Log.d(TAG, "收到广播:" + recString);
                sendData2Activity(recString);
            }
        }
    }
    private void sendData2Activity(String udpData){
        Intent intent = new Intent();
        intent.setAction(StartActivity.SocketDataBroadCastReceive.ACTION);
        intent.putExtra(StartActivity.SocketDataBroadCastReceive.KEY_BROADCAST_TYPE,
                StartActivity.SocketDataBroadCastReceive.TYPE_BECALLEDINFO);
        intent.putExtra(StartActivity.SocketDataBroadCastReceive.KEY_BCALLEDINFO, udpData);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
