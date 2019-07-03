package com.udp;

import android.net.wifi.WifiManager;

import com.tcp.TcpClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UdpHelper  implements Runnable {
    public    Boolean IsThreadDisable = false;//指示监听线程是否终止
    private static WifiManager.MulticastLock lock;
    private static int UDPServerPort = 30011;
    private static int UDPClientPort = 30022;
    private static int sendCount = 0;

    InetAddress mInetAddress;
    public UdpHelper(WifiManager manager) {
        this.lock = manager.createMulticastLock("UDPwifi");
    }
    public void StartListen()  {
        // UDP服务器监听的端口
        // 接收的字节大小，客户端发送的数据不能超过这个大小

        try {
            // 建立Socket连接
            DatagramSocket datagramSocket = new DatagramSocket(UDPServerPort);
            datagramSocket.setBroadcast(true);
            try {
                while (!IsThreadDisable) {
                    // 准备接收数据
                    byte[] message = new byte[4*1024];
                    DatagramPacket datagramPacket = new DatagramPacket(message,message.length);
                    //datagramPacket.setData("".getBytes());

                    TcpClient.printLog("UdpHelper.java,Server begin receive..." );
                    this.lock.acquire();

                    datagramSocket.receive(datagramPacket);
                    String strMsg = new String(datagramPacket.getData()).trim();

                    String clientIP = datagramPacket.getAddress().getHostAddress();
                    TcpClient.printLog("UdpHelper.java,Server,clientIP=="+clientIP);
                    TcpClient.printLog("UdpHelper.java,Server,strMsg=="+strMsg );

                    String sendStr = "yourData:"+strMsg+"|yourIP:"+clientIP+"|serverModel:"+android.os.Build.MODEL.trim();
                    TcpClient.printLog("UdpHelper.java,Server,sendStr=="+sendStr );
                    datagramPacket.setData(sendStr.getBytes());
                    datagramSocket.send(datagramPacket);

                    this.lock.release();
                    datagramPacket = null;
                    message = null;
                }
            } catch (IOException e) {//IOException
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    public static void send(String message) {
        message = (message == null ? "Hello UDP Android!" : message);
        sendCount++;
        message = sendCount+","+message;

        TcpClient.printLog("UdpHelper.java,send,send_message=="+message );

        DatagramSocket s = null;
        try {
            s = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        InetAddress local = null;
        try {
            String ip = "192.168.2.255";//"255.255.255.255";
            local = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        int msg_length = message.length();
        byte[] messageByte = message.getBytes();

        TcpClient.printLog("UdpHelper.java,Client,send,local=="+local );
        TcpClient.printLog("UdpHelper.java,Client,send,UDPPort=="+UDPServerPort );

        DatagramPacket p = new DatagramPacket(messageByte, msg_length, local,UDPServerPort);
        try {
            s.send(p);

            byte[] msg = new byte[4*1024];
            DatagramPacket datagramPacket = new DatagramPacket(msg,msg.length);
            s.receive(datagramPacket);
            TcpClient.printLog("UdpHelper.java,Client,ServerIP=="+datagramPacket.getAddress().getHostAddress());
            String strMsg = new String(datagramPacket.getData()).trim();
            TcpClient.printLog("UdpHelper.java,Client,send and receive strMsg=="+strMsg);

            s.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        StartListen();
    }
}


