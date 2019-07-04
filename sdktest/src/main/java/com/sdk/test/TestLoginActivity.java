package com.sdk.test;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class TestLoginActivity extends Activity {

    private static boolean isShowLog = true;
    private static String Tag = "TestLogin";

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);

    }

    public static void login(String account,String passwd){
        logPrint("TestLoginActivity,login(),account=="+account);
        logPrint("TestLoginActivity,login(),passwd=="+passwd);
    }

    public static void setIsShowLog(String tag,boolean show){
        Tag = tag;
        isShowLog = show;

        logPrint("TestLoginActivity,setIsShowLog(),Tag=="+Tag);
        logPrint("TestLoginActivity,setIsShowLog(),isShowLog=="+isShowLog);
    }

    public static void logPrint(String msg){
        if(isShowLog){
            Log.d(Tag,msg);
        }
    }

}
