package com.tcp;

import android.text.format.Time;

import com.example.helloworld.MainActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by Jason Zhu on 2017-04-25.
 *
 */

public class TcpClient implements Runnable{
    private String TAG = "TcpClient";
    private String  serverIP = "10.0.2.11";
    private int serverPort = 30099;
    private PrintWriter pw;
    private InputStream is;
    private OutputStream os;
    private DataInputStream dis;
    private DataOutputStream dos;
    private boolean isRun = true;
    private Socket socket = null;
    byte buff[]  = new byte[4096];
    private String rcvMsg;
    private int rcvLen;
    public static TcpClient tcpClient;
    private Player player = null;

    //CMD_CSNORMAL_指的是客户端和服务器公用指令，CMD_C2S指客户端请求指令，CMD_S2C指服务器返回指令
    private final int CMD_C2S_LOGIN = 1001;
    private final int CMD_C2S_LOGOUT = 1003;
    private final int CMD_C2S_CHAT =  1004;//cmd:

    private final int CMD_S2C_GET_IPPORT = 2002;
    private final int CMD_S2C_OTHERPLAYER = 2003;

    private final int  CMD_CSNORMAL_HEART = 3000;
    private final int  CMD_CSNORMAL_GETLOCALIP = 3001;
    private final int  CMD_CSNORMAL_GETOTHERIP = 3002;


    private static boolean logPrint = true;


    public TcpClient(String ip , int port){
        tcpClient = this;
        this.serverIP = ip;
        this.serverPort = port;
    }

    public void closeSelf(){
        isRun = false;
    }

    public static void setLogSwitch(boolean isOpen){
        logPrint = isOpen;
    }
    public static void printLog(String msg){
        if(logPrint){
            System.out.println(msg);
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("run: serverIP=="+ serverIP);
            System.out.println("run: serverPort=="+ serverPort);

            socket = new Socket(serverIP,serverPort);
            System.out.println("run: socket=="+ socket);
            socket.setSoTimeout(5000);
            os = socket.getOutputStream();
            pw = new PrintWriter(os,true);
            System.out.println("run: pw=="+ pw);
            is = socket.getInputStream();
            dis = new DataInputStream(is);
            dos = new DataOutputStream(os);
        } catch (IOException e) {
            System.out.println("run: IOException,e=="+ e.toString());
            e.printStackTrace();
            return;
        }

        Time time = new Time();
        time.setToNow();
        String timeStr = time.format2445();
        String username = "user"+timeStr+"_"+android.os.Build.MODEL.trim();
        String passwd = "null";
        String loginMsg = ""+CMD_C2S_LOGIN+"|"+username+":"+passwd;
        send_msg(loginMsg);
        if(player==null){
            player = new Player(username);
        }

        new Thread(){
            @Override
            public void run() {
                Time time = new Time();
                time.setToNow();
                String timeStr = "";
                int heartTime = 3000;//3s发送一次
                long timeMs = System.currentTimeMillis();
                String heartCmdStr = "";
                while(socket!=null){
                    if(System.currentTimeMillis() - timeMs >= heartTime){
                        timeMs = System.currentTimeMillis();
                        time.setToNow();
                        timeStr = time.format2445();
                        heartCmdStr = ""+ CMD_CSNORMAL_HEART +"|"+timeStr+":null";
                        if(player!=null){
                            heartCmdStr = ""+ CMD_CSNORMAL_HEART +"|"+timeStr +":"+player.username +":" + player.userIp+ ":" + player.userPort;
                        }
                        send_msg(heartCmdStr);
                    }
                }
            }
        }.start();

        while (isRun){
            try {
                rcvLen = dis.read(buff);
                System.out.println("run: rcvLen=="+ rcvLen);
                if(rcvLen<=0){
                    continue;
                }
                rcvMsg = new String(buff,0,rcvLen,"utf-8");
                //System.out.println("run: 收到消息:"+ rcvMsg);
                dealServerData(rcvMsg);

            } catch (IOException e) {
                isRun = false;
                e.printStackTrace();
            }

        }
        try {
            pw.close();
            is.close();
            dis.close();
            dos.close();
            socket.close();
            socket = null;
            dis = null;
            dos = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send_msg(String msg){
//        pw.println(msg);
//        pw.flush();
        try {
            dos.writeBytes(msg);
            dos.flush();
        }catch (java.io.IOException e){
            e.printStackTrace();
        }
    }

    public void dealServerData(String msg){
        //格式：cmdCode|cmdContent
        printLog("dealServerData(),rcvMsg=="+rcvMsg);
        int cmdCode = -1;
        String content = "";
        String[] msgStrs = msg.split("\\|");
        printLog("msgStrs[0]=="+msgStrs[0]);
        if(msgStrs.length>1){
            cmdCode = Integer.parseInt(msgStrs[0]);
            for(int i=1;i<msgStrs.length;i++){
                if(i<msgStrs.length-1){
                    content = content + msgStrs[i]+"|";
                }else{
                    content = content + msgStrs[i];
                }
            }
        }

        printLog("dealServerData(),cmdCode=="+cmdCode);
        printLog("dealServerData(),content=="+content);
        switch (cmdCode){
            case CMD_CSNORMAL_HEART:
                break;
            case CMD_S2C_GET_IPPORT:
                if(null!=player){
                    player.setIpPort(content);
                }
                break;
            case CMD_CSNORMAL_GETOTHERIP:
                printLog("dealServerData(),begin connect local_server:"+content);
                MainActivity.connect2LocalServer(content);
                break;

            default:
        }
    }




}
