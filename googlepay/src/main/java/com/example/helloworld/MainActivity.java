package com.example.helloworld;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends UnityPlayerActivity {

    boolean isPrint = true;
//    int count = 0;
//    EditText ipport;
//    TextView logControl;
//    static FuncTcpClient masterClient;//连接的主机服务器
//    static FuncTcpClient localClient;//连接的设备服务器
//    public static String localServerIp = null;
//    public static int masterServerPort = 30099;
//    public static int localServerPort = 30066;
//
//    private LocalServer myServer;
//    private UdpHelper udphelper;
    public static Context cxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cxt = this;

//        setContentView(R.layout.activity_main);
//        initUI();

        StartActivityGooglePay();
//        new Thread(){
//            @Override
//            public void run() {
//                while (true){
//                    try {
//                        count++;
//                        System.out.println("MainActivity,count=="+count);
//                        Thread.sleep(5000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }.start();

    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void StartActivityGooglePay()
    {
        Intent intent = new Intent(cxt,GooglePayActivity.class);
        //intent.putExtra("name", name);
        this.startActivity(intent);
    }

    public void ChargeItem(String productID)
    {
        Log.d(GooglePayActivity.TAG, "MainActivity.java..ChargeItem().GooglePayActivity.payInstance=="+GooglePayActivity.payInstance);
        if(GooglePayActivity.payInstance != null){
            Log.d(GooglePayActivity.TAG, "MainActivity.java..ChargeItem()..productID=="+productID);
            GooglePayActivity.payInstance.buyItemBySku(productID);
        }
    }


    public void UnityCallAndroid(String msg)
    {
        Log.d(GooglePayActivity.TAG, "MainActivity.java...UnityCallAndroid,msg=="+msg);
        AndroidCallUnity(msg);
//        return "Android return : " + msg;
    }
    // Android call Unity
    public void AndroidCallUnity(String msg)
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String dateStr = format.format(new Date());

        Log.d(GooglePayActivity.TAG, "MainActivity.java...AndroidCallUnity,msg=="+msg);
        //com.unity3d.player.UnityPlayer.UnitySendMessage("UnityAndroidCommunicationObj", "AndroidCallUnityCB", msg);
        UnityPlayer.UnitySendMessage("MainCamera","GetDate","from Android..."+dateStr+",msg=="+msg);//再回调Unity的函数
    }


//    public void initUI(){
//        Button fab = findViewById(R.id.paybtn);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                System.out.println("PayActivity,onClick,view=="+view);
//                System.out.println("open PayActivity,count=="+count);
//                Intent intent = new Intent(MainActivity.this, GooglePayActivity.class);
//                startActivity(intent);
//            }
//        });
//        ipport = findViewById(R.id.ipport);
//        ipport.setText("10.0.2.11:30099");
//
//        logControl = findViewById(R.id.log);
//
//        Button connect = findViewById(R.id.connect);
//        connect.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String ip = ipport.getText().toString();
//                System.out.println("connect begin...");
//
//                String serverIP = "";
//                String ipInput = ipport.getText().toString();
//                System.out.println("open PayActivity,ipInput=="+ipInput);
//                String[] segs = ipInput.split(":");
//                if(segs.length>1){
//                    serverIP = segs[0];
//                    masterServerPort = Integer.parseInt(segs[1]);
//                }
//                //myServer = new LocalServer();
//                masterClient = new FuncTcpClient(serverIP,masterServerPort);
//                //masterClient = new FuncTcpClient("10.0.6.22",48973);
//                //masterClient = new FuncTcpClient("101.71.140.6",localServerPort);
//                //masterClient = new FuncTcpClient("192.168.2.4",localServerPort);
//                //masterClient = new FuncTcpClient("101.71.140.7",localServerPort);
//            }
//        });
//
//        Button connectudp = findViewById(R.id.connectudp);
//        connectudp.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                boolean isServer = false;
//                if(isServer){
//                    WifiManager manager = (WifiManager) cxt.getSystemService(Context.WIFI_SERVICE);
//                    udphelper = new UdpHelper(manager);
//
//                    ExecutorService exec = Executors.newCachedThreadPool();
//                    exec.execute(udphelper);
//                }else{
//                    logControl.setText("udp send ready....");
//                    new Thread(){
//                        @Override
//                        public void run() {
//                            String sendStr = "here is android_"+android.os.Build.MODEL.trim();
//                            UdpHelper.send(sendStr);
//                        }
//                    }.start();
//                }
//            }
//        });
//
//        Button udpstart = findViewById(R.id.udpstart);
//        udpstart.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                boolean isServer = true;
//                if(isServer){
//                    logControl.setText("UDPServer is running...");
//                    WifiManager manager = (WifiManager) cxt.getSystemService(Context.WIFI_SERVICE);
//                    udphelper = new UdpHelper(manager);
//
//                    ExecutorService exec = Executors.newCachedThreadPool();
//                    exec.execute(udphelper);
//                }
//            }
//        });
//
//    }

    public static void connect2LocalServer(String ip){
//        System.out.println("PayActivity,connect2LocalServer,localServerIp,localServerPort=="+localServerIp+":"+localServerPort);
//        String[] segs = ip.split(":");
//        if(segs.length>1){
//            localServerIp = segs[0];
//            localServerPort = Integer.parseInt(segs[1]);
//        }
//        localClient = new FuncTcpClient(localServerIp,localServerPort);
    }



    public static String loadJSON(String url) {
         StringBuilder json = new StringBuilder();
         try {
             URL oracle = new URL(url);
             URLConnection yc = oracle.openConnection();
             BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream(),"utf-8"));//防止乱码
             String inputLine = null;
             while ((inputLine = in.readLine()) != null) {
                 json.append(inputLine);
             }
             in.close();
         }
         catch (MalformedURLException e) {

         }
         catch (IOException e) {

         }
        return json.toString();
    }

}
