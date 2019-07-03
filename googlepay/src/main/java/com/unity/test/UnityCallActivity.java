package com.unity.test;

import android.os.Bundle;
import android.util.Log;

import com.example.helloworld.GooglePayActivity;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UnityCallActivity extends UnityPlayerActivity {
    @Override
    protected void onCreate(Bundle bundle){
        super.onCreate(bundle);
    }


    public String UnityCallAndroid(String msg)
    {
        Log.e(GooglePayActivity.TAG, "UnityCallActivity.java...UnityCallAndroid,msg=="+msg);
        AndroidCallUnity(msg);
        return "Android return : " + msg;
    }
    // Android call Unity
    public void AndroidCallUnity(String msg)
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String dateStr = format.format(new Date());

        Log.e(GooglePayActivity.TAG, "UnityCallActivity.java...AndroidCallUnity,msg=="+msg);
        //com.unity3d.player.UnityPlayer.UnitySendMessage("UnityAndroidCommunicationObj", "AndroidCallUnityCB", msg);
        UnityPlayer.UnitySendMessage("MainCamera","GetDate",dateStr+",msg=="+msg);//再回调Unity的函数
    }


}
