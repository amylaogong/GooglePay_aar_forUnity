package com.sdk.test;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class TestLoginActivity extends Activity {

    private static boolean isShowLog = true;
    private static String Tag = "TestLogin";
    private static SdkListener.OnLoginListener loginListener = null;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);

    }

    public static void login(SdkListener.OnLoginListener listener,String jsonStr){
        loginListener = listener;
        logPrint("TestLoginActivity,login(),jsonStr=="+jsonStr);

        try {
            JSONObject jsonResult = new JSONObject();
            JSONObject obj = new JSONObject(jsonStr);

            String account = obj.getString("account");
            String passwd = obj.getString("passwd");

            jsonResult.put("uid","uid_111"+account+"111"+passwd);
            jsonResult.put("token","token_222"+account+"222"+passwd);

            logPrint("TestLoginActivity,login(),jsonResult=="+jsonResult.toString());

            loginListener.onLoginFinished(jsonResult.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }



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
