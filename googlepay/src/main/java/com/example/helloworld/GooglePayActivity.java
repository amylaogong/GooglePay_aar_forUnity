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
import com.googlepay.util.IabResult;
import com.googlepay.util.Purchase;
import com.product.init.GooglePay;
import com.product.init.PayInterface;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Example game using in-app billing version 3.
 */
public class GooglePayActivity extends UnityPlayerActivity {

    public static final String TAG = "123GooglePay";
    IabBroadcastReceiver mBroadcastReceiver = null;
    public static GooglePay payInstance = null;
    public static int runMode = 1;// 0 正常；1测试
    private String googlePublicKey = null;
    public static PayInterface.OnPayProcessListener payProcessListener = new PayInterface.OnPayProcessListener() {
        @Override
        public void onProcess(int code, IabResult result, Purchase info) {
            JSONObject obj;
            try {
                obj = new JSONObject();
                obj.put("code",code);

                if(result!=null){
                    obj.put("IabHelerCode",result.getResponse());
                    obj.put("resultMsg",result.getMessage());
                }
                if(info!=null){
                    obj.put("googleOrderId",info.getOrderId());
                    obj.put("getSku",info.getSku());
                    obj.put("selfOrderId",info.getDeveloperPayload());
                }
                AndroidCallUnity("OnCallBackPayProcess",obj.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static PayInterface.OnPaySuccessListener paySuccessListener = new PayInterface.OnPaySuccessListener() {
        @Override
        public void onSuccess(int code, Purchase info) {
            GooglePay.logPrint( "GooglePayActivity.java..paySuccessListener().info=="+info);
            JSONObject obj;
            try {
                obj = new JSONObject();
                obj.put("code",code);
                if(info!=null){
                    obj.put("googleOrderId",info.getOrderId());
                    obj.put("getSku",info.getSku());
                    obj.put("selfOrderId",info.getDeveloperPayload());
                    GooglePay.logPrint( "GooglePayActivity.java..paySuccessListener().returnUnity=="+obj.toString());
                    AndroidCallUnity("OnCallBackPaySuccess",obj.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static PayInterface.OnQuerryOwnedSkuListener querryOwnedSkuListener = new PayInterface.OnQuerryOwnedSkuListener() {
        @Override
        public void onQuerryOwnedSku(int code, ArrayList<String> ownedSkus) {
            GooglePay.logPrint( "GooglePayActivity.java..querryOwnedSkuListener().code=="+code);
            JSONObject obj;
            try {
                obj = new JSONObject();
                obj.put("code",code);
                if(ownedSkus!=null){
                    int size = ownedSkus.size();
                    obj.put("ownedSkuSize",size);
                    for(int i=0;i<size;i++){
                        obj.put("getSku_"+i,ownedSkus.get(i));
                    }
                    GooglePay.logPrint( "GooglePayActivity.java..querryOwnedSkuListener().returnUnity=="+obj.toString());
                    AndroidCallUnity("OnCallBackQuerryOwnedSku",obj.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };



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


    // updates UI to reflect model
//    public void initUI() {
//
//        Button close = (Button)findViewById(R.id.close);
//        close.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View view){
//                //request owned items that not consumed.
//                logPrint("close,onClick...getHelperInstance()=="+getHelperInstance());
//                finish();
//            }
//        });
//
//        Button queryOwned = (Button)findViewById(R.id.queryOwned);
//        queryOwned.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View view){
//                //request owned items that not consumed.
//                logPrint("queryOwned,onClick...getHelperInstance()=="+getHelperInstance());
//                if(payInstance!=null){
//                    payInstance.querySkuOnwed();
//                }
//            }
//        });
//        Button consumeOwned = findViewById(R.id.consumeOwned);
//        consumeOwned.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                logPrint("consumeOwned,onClick...payInstance=="+payInstance);
//                if(payInstance!=null){
//                    payInstance.consumedNextOwnedItem();
//                }
//            }
//        });
//
//        Button button1 = findViewById(R.id.button1);
//        button1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                logPrint("button1 buy item1,onClick..payInstance=="+payInstance);
//                if(payInstance!=null){
//                    payInstance.buyItemBySku("item_charge_1");
//                }
//
//            }
//        });
//        Button button2 = findViewById(R.id.button2);
//        button2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                logPrint("button2 buy item2,onClick...payInstance=="+payInstance );
//                if(payInstance!=null){
//                    payInstance.buyItemBySku("item_charge_2");
//                }
//            }
//        });
//        Button button3 = findViewById(R.id.button30);
//        button3.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                logPrint("button3 buy item30,onClick..payInstance"+payInstance );
//                if(payInstance!=null){
//                    payInstance.buyItemBySku("item_charge_30");
//                }
//            }
//        });
//    }
}
