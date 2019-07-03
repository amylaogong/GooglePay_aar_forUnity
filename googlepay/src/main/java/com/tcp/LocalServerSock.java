package com.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

//本地服务器上所连接的
public class LocalServerSock implements Runnable{
    InputStream is;
    OutputStream os;
    DataInputStream dis;
    DataOutputStream dos;
    Socket sock;
    boolean isRun = true;
    byte buff[]  = new byte[4096];
    private String rcvMsg;
    private int rcvLen;

    public LocalServerSock(Socket sock){
        this.sock = sock;
    }

    public void send_msg(String msg){
        try {
            dos.writeBytes(msg);
            dos.flush();
        }catch (java.io.IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        try {
            os = sock.getOutputStream();
            is = sock.getInputStream();
            dis = new DataInputStream(is);
            dos = new DataOutputStream(os);
        }catch (IOException e){
            e.printStackTrace();
        }

        while (isRun){
            try {
                rcvLen = dis.read(buff);
                System.out.println("run: rcvLen=="+ rcvLen);
                if(rcvLen<=0){
                    continue;
                }
                rcvMsg = new String(buff,0,rcvLen,"utf-8");
                System.out.println("LocalServerSock,run: rcvMsg==:"+ rcvMsg);
                send_msg("88993|"+rcvMsg);
            } catch (IOException e) {
                isRun = false;
                e.printStackTrace();
            }
        }
        try {
            is.close();
            dis.close();
            dos.close();
            sock.close();
            sock = null;
            dis = null;
            dos = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
