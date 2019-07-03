package com.hxl.xshb;

import android.os.Bundle;
import android.util.Log;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

public class MainActivity extends UnityPlayerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
    }
    public void CallUnityMethod(String str)
    {

        Log.d("MainActivity","MainActivity.java,CallUnityMethod(),str=="+str);

        UnityPlayer.UnitySendMessage("Canvas","AndroidCallUnityCallBack","MainActivity.java,CallUnityMethod()|||"+str);
    }
}
