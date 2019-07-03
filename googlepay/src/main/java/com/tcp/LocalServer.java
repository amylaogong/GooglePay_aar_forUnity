package com.tcp;

import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalServer {
    private ArrayList<Socket> sockList = new ArrayList<Socket>() ;
    ExecutorService exec = Executors.newCachedThreadPool();


    public LocalServer(){
//        ipPort = addr;
//        String[] segs = addr.split(":");
//        if(segs.length>1){
//            ip = segs[0];
//            port = Integer.parseInt(segs[1]);
//        }
//        TcpClient.printLog("LocalServer.java,LocalServer(),(ip,port)=="+ip+","+port);
        createServer();

    }

    public void createServer() {
//        new Thread() {
//            @Override
//            public void run() {
//                try {
//                    TcpClient.printLog("LocalServer.java,createServer(),MainActivity.localServerPort=="+MainActivity.localServerPort);
//                    ServerSocket ss = new ServerSocket(MainActivity.localServerPort);
//                    //采用循环不断接受来自客户端的请求,服务器端也对应产生一个Socket
//                    while (true) {
//                        TcpClient.printLog("LocalServer.java,createServer(),server is ready..waiting for link...");
//                        Socket socket = ss.accept();
//                        sockList.add(socket);
//                        TcpClient.printLog("LocalServer.java,createServer(),socket=="+socket);
//                        TcpClient.printLog("LocalServer.java,createServer(),socket.getInetAddress().getAddress().toString()=="+socket.getInetAddress().getAddress().toString());
//                        TcpClient.printLog("LocalServer.java,createServer(),socket.getLocalAddress().getHostAddress()=="+socket.getLocalAddress().getHostAddress());
//                        TcpClient.printLog("LocalServer.java,createServer(),socket.getPort()=="+socket.getPort());
//
//                        LocalServerSock localSock = new LocalServerSock(socket);
//                        exec.execute(localSock);
//
//                    }
//                } catch (java.io.IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }.start();
    }


}
