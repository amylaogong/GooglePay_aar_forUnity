package com.sdk.test;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

public class TestLoginActivity extends Activity {

    private static boolean isShowLog = true;
    private static String Tag = "TestLogin";
    private static SdkListener.OnLoginListener loginListener = null;

    private Button btnclose;
    public static TestLoginActivity mAct;


    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);

        //下一步测试打开Activity界面
        mAct = this;
        setContentView(R.layout.laout_login);
        btnclose = findViewById(R.id.btnclose);
        btnclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logPrint("TestLoginActivity,login and btnclose(),onClick");

                login();

                finish();
            }
        });

    }

    public static void setLoginListener(SdkListener.OnLoginListener listener){
        loginListener = listener;
    }



    public static void login(){
        logPrint("TestLoginActivity,login()");
        try {
            JSONObject jsonResult = new JSONObject();
            jsonResult.put("account","input_Account_FromTestLoginActivity");
            jsonResult.put("passwd","input_PASS_FromTestLoginActivity");
            jsonResult.put("uid","uid_123456");
            jsonResult.put("token","token_9876543");

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
