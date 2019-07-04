/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 */

package com.example.helloworld;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.googlepay.util.IabBroadcastReceiver;
import com.googlepay.util.IabHelper;
import com.product.init.GooglePay;
import com.sdk.test.TestLoginActivity;
import com.unity.callback.AndroidUnityInterface;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

/**
 * Example game using in-app billing version 3.
 */
public class GooglePayActivity extends UnityPlayerActivity {

    public static final String TAG = "123GooglePay";
    IabBroadcastReceiver mBroadcastReceiver = null;
    public static GooglePay payInstance = null;
    public static int runMode = 1;// 0 正常；1测试
    private String googlePublicKey = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    //被unity调用的方法不能使用静态的
    public void SetRunMode(int mode){
        runMode = mode;
        if(runMode == 1){
            GooglePay.logSwitch = true;
        }else if(runMode == 0){
            GooglePay.logSwitch = false;
        }
        GooglePay.logPrint( "GooglePayActivity.java..SetRunMode().runMode=="+runMode);
    }

    public void InitPay(String googleKey)
    {
        GooglePay.logPrint( "GooglePayActivity.java..InitPay().googleKey=="+googleKey);
        googlePublicKey = googleKey;
        getGooglePayInstance();
        GooglePay.logPrint( "GooglePayActivity.java..InitPay().payInstance=="+payInstance);
    }

    public void ChargeByProductID(String productID)
    {
        GooglePay.logPrint( "GooglePayActivity.java..ChargeByProductID().payInstance=="+payInstance);
        if(getGooglePayInstance() != null){
            GooglePay.logPrint( "GooglePayActivity.java..ChargeByProductID()..productID=="+productID);
            payInstance.buyItemBySku(productID);
        }
    }

    public void QuerySkuOnwed()
    {
        GooglePay.logPrint( "00GooglePayActivity.java..QuerySkuOnwed()");
        if(payInstance != null){
            payInstance.querySkuOnwed();
        }
    }

    public void DoLogin(String account,String passwd)
    {
        TestLoginActivity.setIsShowLog(TAG,GooglePay.logSwitch);
        GooglePay.logPrint( "00GooglePayActivity.java..DoLogin(),account=="+account);
        GooglePay.logPrint( "00GooglePayActivity.java..DoLogin(),passwd=="+passwd);
        TestLoginActivity.login(account,passwd);
        AndroidUnityInterface.SetUnityCache(account,passwd);
    }

    public static void TestStaticCall(String msg){
        GooglePay.logPrint( "GooglePayActivity.java...TestStaticCall,msg=="+msg);
    }

    public static void AndroidCallUnity(String method,String paramJson)
    {
        GooglePay.logPrint( "GooglePayActivity.java...AndroidCallUnity,method=="+method);
        GooglePay.logPrint( "GooglePayActivity.java...AndroidCallUnity,paramJson=="+paramJson);
        UnityPlayer.UnitySendMessage("AndroidInterface",method,paramJson);//再回调Unity的函数
    }


    public GooglePay getGooglePayInstance(){
        if(payInstance==null){
            payInstance = new GooglePay(this,googlePublicKey);
        }
        return payInstance;
    }

    public IabHelper getHelperInstance(){
        if(payInstance==null){
            return null;
        }
        return payInstance.mHelper;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        GooglePay.logPrint( "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (getHelperInstance() == null) return;

        // Pass on the activity result to the helper for handling
        if (!getHelperInstance().handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
        if(payInstance!=null){
            payInstance.onDestroy();
        }
    }

}
