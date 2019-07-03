package com.tcp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Jason Zhu on 2017-04-24.
 *
 */

public class FuncTcpClient{
    private String TAG = "FuncTcpClient";
    private TcpClient tcpClient = null;
    ExecutorService exec = Executors.newCachedThreadPool();

    public FuncTcpClient(String ip,int port) {
        tcpClient = new TcpClient(ip,port);
        exec.execute(tcpClient);
    }

}
