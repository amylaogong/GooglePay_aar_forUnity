/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 */

package com.example.helloworld;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.login.GoogleSignInHelper;
import com.google.pay.GooglePay;
import com.googlepay.util.IabBroadcastReceiver;
import com.googlepay.util.IabHelper;
import com.sdk.test.TestLoginActivity;
import com.unity.callback.AndroidUnityInterface;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Example game using in-app billing version 3.
 */
public class MainActivity extends UnityPlayerActivity {

    public static final String TAG = "123GooglePay";
    IabBroadcastReceiver mBroadcastReceiver = null;
    public static GooglePay payInstance = null;
    public static int runMode = 1;// 0 正常；1测试
    private String googlePublicKey = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GooglePay.logPrint( "MainActivity.java..onCreate().runMode=="+runMode);
    }

    //被unity调用的方法不能使用静态的
    public void SetRunMode(int mode){
        runMode = mode;
        if(runMode == 1){
            GooglePay.logSwitch = true;
        }else if(runMode == 0){
            GooglePay.logSwitch = false;
        }
        GooglePay.logPrint( "MainActivity.java..SetRunMode().runMode=="+runMode);
    }


    public void SDKLogin(int mode,String params){
        if(mode == 1){//google login
            String googleKey = params;
            googleLogin(googleKey);
        }
    }

    public void  SDKLogout(int mode){
        if(mode == 1){//google
            googleLogout();
        }
    }


    public void InitPay(String googleKey,String jsonTxt)
    {
        GooglePay.logPrint( "MainActivity.java..InitPay().googleKey=="+googleKey);
        googlePublicKey = googleKey;
        GooglePay.skuJsons = jsonTxt;
        getGooglePayInstance();
        GooglePay.logPrint( "MainActivity.java..InitPay().payInstance=="+payInstance);
    }

    public void ChargeByProductID(String productID,String gameOrderID)
    {
        GooglePay.logPrint( "MainActivity.java..ChargeByProductID().payInstance=="+payInstance);
        if(getGooglePayInstance() != null){
            GooglePay.logPrint( "MainActivity.java..ChargeByProductID()..productID=="+productID);
            GooglePay.logPrint( "MainActivity.java..ChargeByProductID()..gameOrderID=="+gameOrderID);
            GooglePay.gameOrderId = gameOrderID;
            payInstance.buyItemBySku(productID);
        }
    }

    public void QuerySkuOnwed()
    {
        GooglePay.logPrint( "00MainActivity.java..QuerySkuOnwed()");
        if(payInstance != null){
            payInstance.querySkuOnwed();
        }
    }

    public void ConsumedOwnedItem(){
        GooglePay.logPrint( "00MainActivity.java..ConsumedOwnedItem()");
        if(payInstance != null){
            payInstance.consumedNextOwnedItem();
        }
    }

    public void VerifyPurchase(String goolgeOrder,String purchaseData,String signature)
    {
        GooglePay.logPrint( "00MainActivity.java..VerifyPurchase()");
        if(payInstance != null){
            payInstance.verifyPurchase(goolgeOrder,purchaseData,signature);
        }
    }

    public void QuerrySkuDetail(String jsonTxt)
    {
        GooglePay.logPrint( "00MainActivity.java..QuerrySkuDetail()");
        if(payInstance != null){
            payInstance.initSkulist(jsonTxt);
        }
    }


    public void DoLogin(String account,String passwd)
    {
        TestLoginActivity.setIsShowLog(TAG,GooglePay.logSwitch);
        GooglePay.logPrint( "00MainActivity.java..DoLogin()");

        try {
            JSONObject jsonResult = new JSONObject();
            jsonResult.put("account",account);
            jsonResult.put("passwd",passwd);

            TestLoginActivity.setLoginListener(AndroidUnityInterface.loginListener);
            Intent intent = new Intent(this,TestLoginActivity.class);
            this.startActivity(intent);

            AndroidUnityInterface.SetUnityCache(account,passwd);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public static void TestStaticCall(String msg){
        GooglePay.logPrint( "MainActivity.java...from_unity_TestStaticCall,msg=="+msg);
    }

    public static void AndroidCallUnity(String method,String paramJson)
    {
        GooglePay.logPrint( "MainActivity.java...AndroidCallUnity,method=="+method);
        GooglePay.logPrint( "MainActivity.java...AndroidCallUnity,paramJson=="+paramJson);
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
        GooglePay.logPrint( "MainActivity,onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (getHelperInstance() == null) return;

        // Pass on the activity result to the helper for handling
        if (!getHelperInstance().handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "MainActivity,onActivityResult handled by IABUtil.");
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


    private void googleLogin(String googleKey){
        GoogleSignInHelper.getInstance().setGoogleKey(googleKey);
        GoogleSignInHelper.getInstance().checkState(this);
        GoogleSignInHelper.getInstance().requestLogin(this, false, new GoogleSignInHelper.GoogleSignInCallback() {

            @Override
            public void onSuccess(String authCode, String uid, String name) {
                GoogleSignInHelper.printDebug("MainActivity,googleLogin -> onSuccess():authCode = " + authCode);
                GoogleSignInHelper.printDebug("MainActivity,googleLogin -> onSuccess():uid = " + uid);

                GoogleSignInHelper.printDebug("MainActivity,googleLogin -> onSuccess():name = " + name);

                GoogleSignInHelper.printDebug("MainActivity,googleLogin -> onSuccess() 222:GoogleSignInHelper.mGoogleApiClient = " + GoogleSignInHelper.mGoogleApiClient);
                boolean isConnected = GoogleSignInHelper.mGoogleApiClient.isConnected();
                GoogleSignInHelper.printDebug("MainActivity,googleLogin,onSuccess(),222 mGoogleApiClient.isConnected=="+isConnected);

            }

            @Override
            public void onError() {
                GoogleSignInHelper.printDebug("GoogleSignInCallback -> onError()");
            }

            @Override
            public void onCancel() {
                GoogleSignInHelper.printDebug("GoogleSignInCallback -> onCancel()");
            }
        });

    }

    private void googleLogout(){
        GoogleSignInHelper.printDebug("MainActivity,googleLogin -> onSuccess():222 GoogleSignInHelper.mGoogleApiClient = " + GoogleSignInHelper.mGoogleApiClient);
        GoogleSignInHelper.signOut();
    }

}
