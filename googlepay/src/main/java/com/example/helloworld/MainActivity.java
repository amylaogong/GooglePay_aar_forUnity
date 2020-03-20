/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 */

package com.example.helloworld;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.login.GoogleSignInHelper;
import com.google.pay.GooglePay;
import com.googlepay.util.IabBroadcastReceiver;
import com.googlepay.util.IabHelper;
import com.sdk.test.TestLoginActivity;
import com.tools.listener.FunctionCalledListener;
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

    public static MainActivity mainActivity = null;
    private ViewGroup contentView;
    private LinearLayout mContainer;


    private PayTask curPayTask = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GooglePay.logPrint( "MainActivity.java..onCreate().runMode=="+runMode);
        mainActivity = this;
//        contentView = (ViewGroup)this.getWindow().getDecorView();
//        GooglePay.logPrint( "MainActivity.java..onCreate().contentView=="+contentView);
//
//        SetOperation(false);
    }

    public void CancelPayTask(){
        if(curPayTask!=null){
            curPayTask.cancel(true);
            curPayTask = null;
        }
    }

    public void SetOperation(boolean canOp){
        GooglePay.logPrint( "MainActivity.java..SetOperation()..canOp=="+canOp);

//        if(!canOp){
//        }else{
//
//        }
    }


    //被unity调用的方法
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    GooglePay.logPrint( "MainActivity.java..InitPay().payInstance=="+payInstance);
                    getGooglePayInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void ChargeByProductID(String productID,String gameOrderID)
    {
        SetOperation(false);

        GooglePay.logPrint( "MainActivity.java..ChargeByProductID().payInstance=="+payInstance);
        if(payInstance != null){
            GooglePay.logPrint( "MainActivity.java..ChargeByProductID()..productID=="+productID);
            GooglePay.logPrint( "MainActivity.java..ChargeByProductID()..gameOrderID=="+gameOrderID);
            GooglePay.gameOrderId = gameOrderID;
            GooglePay.curProductId = productID;
            payInstance.buyItemBySku(GooglePay.curProductId);

//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        payInstance.buyItemBySku(GooglePay.curProductId);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();

        }
    }

    public void QuerySkuOnwed()
    {
        GooglePay.logPrint( "00MainActivity.java..QuerySkuOnwed()");
        if(payInstance != null){
//            payInstance.querySkuOnwed();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        payInstance.querySkuOnwed();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public void ConsumedOwnedItem(){
        GooglePay.logPrint( "00MainActivity.java..ConsumedOwnedItem()");
        if(payInstance != null){
//            payInstance.consumedNextOwnedItem();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        payInstance.consumedNextOwnedItem();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public void VerifyPurchase(String goolgeOrder,String purchaseData,String signature)
    {
        GooglePay.logPrint( "00MainActivity.java..VerifyPurchase()");
        GooglePay.verifyPurchase(goolgeOrder,purchaseData,signature);
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
        payInstance = new GooglePay(this,googlePublicKey);
        return payInstance;

//        if(payInstance==null){
//            payInstance = new GooglePay(this,googlePublicKey);
//        }else if(!payInstance.isPayServiceReady){
//            GooglePay.logPrint( "MainActivity.java...getGooglePayInstance,isPayServiceReady=="+payInstance.isPayServiceReady);
//            payInstance = new GooglePay(this,googlePublicKey);
//        }
//        return payInstance;
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


    //异步方式，时间消耗更少，实验结果：异步7.7s,主线程里10s
    public void RunPayTaskOnce(String googleKey,String jsonTxt,String productID,String gameOrderID){
        if(curPayTask!=null){
            curPayTask.cancel(true);
            curPayTask = null;
        }

        curPayTask = new PayTask();
        String[] params = {
                googleKey,
                jsonTxt,
                productID,
                gameOrderID
        };
        curPayTask.execute(params);
    }


    private class PayTask extends AsyncTask<String, Object, Long> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            GooglePay.logPrint( "MainActivity.java..GooglePayTask(),onPreExecute...");
        }

        @Override
        protected Long doInBackground(String... params) {
            long resultValue = 0;

            GooglePay.logPrint( "MainActivity.java..GooglePayTask(),doInBackground...");
            String googleKey = params[0];
            String jsonTxt = params[1];
            String productID = params[2];
            String gameOrderID = params[3];

            GooglePay.logPrint( "MainActivity.java..GooglePayTask(),doInBackground...productID=="+productID);
            GooglePay.logPrint( "MainActivity.java..GooglePayTask(),doInBackground...gameOrderID=="+gameOrderID);
            GooglePay.logPrint( "MainActivity.java..GooglePayTask(),doInBackground...GooglePay.payProgressCode=="+GooglePay.payProgressCode);


            googlePublicKey = googleKey;
            GooglePay.skuJsons = jsonTxt;
            getGooglePayInstance();

            int count = 0;
            while(GooglePay.payProgressCode != FunctionCalledListener.PAY_STATE_SERVICE_READY){
//                count++;
//                GooglePay.logPrint( "MainActivity.java..GooglePayTask(),doInBackground..not ready..count=="+count);

                if(GooglePay.payProgressCode == FunctionCalledListener.PAY_STATE_SERVICE_INIT_FAILED){
                    Object[] result = new Object[1];
                    result[0] = GooglePay.payProgressCode;
                    publishProgress(result);

                    if(curPayTask!=null){
                        curPayTask.cancel(true);
                        curPayTask = null;
                        return resultValue;
                    }
                }
            }

            Object[] result_ready = new Object[1];
            result_ready[0] = GooglePay.payProgressCode;
            publishProgress(result_ready);


            GooglePay.logPrint( "MainActivity.java..GooglePayTask(),doInBackground...GooglePay.payProgressCode=="+GooglePay.payProgressCode);
            GooglePay.gameOrderId = gameOrderID;
            GooglePay.curProductId = productID;
            payInstance.buyItemBySku(GooglePay.curProductId);
            count = 0;
            while(GooglePay.payProgressCode != FunctionCalledListener.PAY_STATE_PROCESS_PURCHASE_DONE){
//                count++;
//                GooglePay.logPrint( "MainActivity.java..GooglePayTask(),doInBackground..Not_PURCHASE_DONE..count=="+count);

                if(GooglePay.payProgressCode == FunctionCalledListener.PAY_STATE_PROCESS_PURCHASE_CANCELLED){
                    Object[] result = new Object[1];
                    result[0] = GooglePay.payProgressCode;
                    publishProgress(result);

                    if(curPayTask!=null){
                        curPayTask.cancel(true);
                        curPayTask = null;
                        return resultValue;
                    }
                }
            }

            Object[] result_purchase_done = new Object[1];
            result_purchase_done[0] = GooglePay.payProgressCode;
            publishProgress(result_purchase_done);
            count = 0;
            while(GooglePay.payProgressCode != FunctionCalledListener.PAY_STATE_CONSUME_SUCCESS){
//                count++;
//                GooglePay.logPrint( "MainActivity.java..GooglePayTask(),doInBackground..Not_CONSUME_SUCCESS..count=="+count);
            }

            Object[] result_success = new Object[1];
            result_success[0] = GooglePay.payProgressCode;
            publishProgress(result_success);


            return resultValue;
        }



        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            GooglePay.logPrint( "MainActivity.java..GooglePayTask(),onProgressUpdate...Thread_name==" + Thread.currentThread().getName());
            int code = (int)values[0];
            GooglePay.logPrint( "MainActivity.java..GooglePayTask(),onProgressUpdate...code==" + code);

        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);
            GooglePay.logPrint( "MainActivity.java..GooglePayTask(),onPostExecute...Thread_name==" + Thread.currentThread().getName());
            GooglePay.logPrint( "MainActivity.java..GooglePayTask(),onPostExecute...purchase over..." );
        }

        @Override
        protected void onCancelled() {
            GooglePay.logPrint( "MainActivity.java..GooglePayTask(),onCancelled...Thread_name==" + Thread.currentThread().getName());
            super.onCancelled();
        }
    }



}

