package com.tcp;

public class Player {
    public String username;
    public String userIp;
    public int userPort;

    private String passwd;

    public Player(String name){
        username = name;

    }

    public void setIpPort(String addr){
        String[] segs = addr.split(":");
        if(segs.length>1){
            userIp = segs[0];
            userPort = Integer.parseInt(segs[1]);
        }
        TcpClient.printLog("Player.java,setIpPort(),(ip,port)=="+userIp+","+userPort);
        //MainActivity.localServerPort = userPort;

    }




}
