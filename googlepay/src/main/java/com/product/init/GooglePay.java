package com.product.init;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import com.example.helloworld.GooglePayActivity;
import com.googlepay.util.IabException;
import com.googlepay.util.IabHelper;
import com.googlepay.util.IabHelper.IabAsyncInProgressException;
import com.googlepay.util.IabResult;
import com.googlepay.util.Inventory;
import com.googlepay.util.Purchase;

public class GooglePay {
    //google支付部分：
    // 声明属性The helper object
    private Context payAct;
    public static boolean isUse = true;
    private int requestCode = 10020;
    public static boolean logSwitch = true;
    private boolean isPayServiceReady = false;

    public IabHelper mHelper = null;
    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {//监听查询所有的产品
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            logPrint( "Query inventory finished.");
            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;
            // Is it a failure?
            if (result.isFailure()) {
                logPrint("Failed to query inventory: " + result);
                return;
            }
            logPrint( "Query inventory was successful.");
        }
    };

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {//购买完成的回调
            logPrint("Purchase finished: " + result + ", purchase: " + purchase);
            logPrint("OnIabPurchaseFinishedListener...purchase=="+purchase);
            logPrint("OnIabPurchaseFinishedListener...result=="+result);

            if(result!=null){
                if(result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED){
                    GooglePayActivity.payProcessListener.onProcess(PayInterface.PAY_STATE_PROCESS_PURCHASE_CANCELLED,result,purchase);
                    return ;
                }
            }
            GooglePayActivity.payProcessListener.onProcess(PayInterface.PAY_STATE_PROCESS_PURCHASE,result,purchase);

            if(purchase==null){
                return;
            }

            logPrint("OnIabPurchaseFinishedListener...purchase.getSku()=="+purchase.getSku());
            logPrint("OnIabPurchaseFinishedListener...result.isFailure()=="+result.isFailure());
            logPrint("OnIabPurchaseFinishedListener...verifyDeveloperPayload(purchase)=="+verifyDeveloperPayload(purchase));
            logPrint("OnIabPurchaseFinishedListener...result=="+result);
            logPrint("OnIabPurchaseFinishedListener,buy item succeed...but not consumed");

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                return;
            }
            logPrint("OnIabPurchaseFinishedListener...now will consumeAsync");

            GooglePayActivity.payProcessListener.onProcess(PayInterface.PAY_STATE_PROCESS_PURCHASE_DONE,result,purchase);

            try {
                mHelper.consumeAsync(purchase, mConsumeFinishedListener);
            } catch (IabAsyncInProgressException e) {
                logPrint("Error consuming gas. Another async operation in progress.");
                return;
            }
            logPrint( "Purchase successful.");

        }
    };
    // Called when consumption is complete//监听物品是否消耗成功
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            logPrint("onConsumeFinished...purchase=="+purchase);
            logPrint("onConsumeFinished...purchase.toString()=="+purchase.toString());
            logPrint("onConsumeFinished...purchase.getSku()=="+purchase.getSku());
            logPrint("onConsumeFinished...purchase.getOrderId()=="+purchase.getOrderId());
            logPrint("onConsumeFinished...purchase.getDeveloperPayload()=="+purchase.getDeveloperPayload());

            logPrint("onConsumeFinished...result=="+result);
            logPrint("onConsumeFinished...result.isSuccess()=="+result.isSuccess());


            GooglePayActivity.payProcessListener.onProcess(PayInterface.PAY_STATE_PROCESS_CONSUME,result,purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the gas tank a bit
                logPrint("onConsumeFinished...successful..then we can give the item to player!!!");

                GooglePayActivity.paySuccessListener.onSuccess(PayInterface.PAY_STATE_CONSUME_SUCCESS,purchase);


                //alert("onConsumeFinished...successful ");
                if(mHelper.mInv!=null){
                    mHelper.mInv.erasePurchase(purchase.getSku());
                    logPrint("onConsumeFinished,.mHelper.mInv.mPurchaseMap.size=="+mHelper.mInv.mPurchaseMap.size());
                    consumedNextOwnedItem();
                }
            }
            else {
                logPrint("consume failed..Error while consuming: " + result);
            }

            logPrint("onConsumeFinished...over...");
        }
    };

    //base64EncodedPublicKey是在Google开发者后台复制过来的：要集成的应用——>服务和API——>此应用的许可密钥（自己去复制）
    //"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAh+LmGYNVmHE2oQD/nLseF1n0if5evGkc7/K+fAFFTUHXKTcHpJrmexlJ+4rg2TUa5be0o21VTFipy8oBfCbrek0eIEf3vzf1LwEfunA9SRljmhoBZ41vv5IxVLl1opS7kM9vFcF3ov2PzbngP1lI9Iy/5QQXCGcVmP4ohnJMQvgCsgE0LhFlaGSPZ5hZi5vzg7hDO6wdpAg9pyYJTPc3oOyeGTPZUTgWsj8RAIQBegaSnmkOYFQvi5e17SsDiYgs3awgtWFQJEcMcko8P3BAGKuuolwDDKyMxtBqkHz+rYNeEHqApWa1DDfu5SLaYCva8qaiacCU4wteP9d19Pn7EwIDAQAB";//"MIIBIjANBgkqh******************************DAQAB";
    private String base64EncodedPublicKey = "";

    public static void logPrint(String des){
        if(logSwitch){
            Log.d(GooglePayActivity.TAG,des);
        }
    }

    public GooglePay(Context act,String key){
        payAct = act;
        base64EncodedPublicKey = key;
        logPrint("GooglePay()..base64EncodedPublicKey.length()==" + base64EncodedPublicKey.length());
        mHelper = new IabHelper(payAct, base64EncodedPublicKey);

        if (base64EncodedPublicKey.length() < 50) {
            throw new RuntimeException("Please put your app's public key in MainActivity.java. See README.");
        }
        // Create the helper, passing it our context and the public key to verify signatures with
        logPrint("GooglePay()..Creating IAB helper.");

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        logPrint( "GooglePay()..Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                logPrint("onIabSetupFinished，Setup finished.");
                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    logPrint("onIabSetupFinished,Problem setting up in-app billing: " + result);
                    return;
                }
                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

//                mBroadcastReceiver = new IabBroadcastReceiver(payAct);
//                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
//                registerReceiver(mBroadcastReceiver, broadcastFilter);

                logPrint("Setup successful. querySkuDetailsAsync...");

                isPayServiceReady = true;
                try {
                    mHelper.querySkuDetailsAsync(mGotInventoryListener);
                } catch (IabAsyncInProgressException e) {
                    logPrint("GooglePay()... Another async operation in progress.");
                }
            }
        });


    }

    public void querySkuOnwed(){
        logPrint("queryOwned,onClick...mHelper=="+mHelper);
        if(mHelper==null){
            return;
        }
        try {
            mHelper.querySkuOnwed();
        } catch (IabException e) {
            e.printStackTrace();
        }
    }

    public void buyItemBySku(String sku){
        logPrint("buyItemBySku,onClick...sku=="+sku);
        if(mHelper==null){
            return;
        }
        boolean isItemsCanBuy = mHelper.mSkuPriceMap.containsKey(sku);
        logPrint("buyItemBySku,onClick...isItemsCanBuy=="+isItemsCanBuy);
//        if(!isItemsCanBuy){
//            return;
//        }
        mHelper.gotoBuyItemBySku(sku,requestCode,mPurchaseFinishedListener);
    }

    public void consumedNextOwnedItem(){
        logPrint("consumedAllItems...mHelper=="+mHelper);
        if(mHelper==null){
            return;
        }
        logPrint("consumedAllItems...mHelper.mInv=="+mHelper.mInv);
        if(mHelper.mInv!=null){
            int size = mHelper.mInv.mPurchaseMap.size();
            logPrint("consumedAllItems...mHelper.mInv.mPurchaseMap.size=="+size);
            if(size<=0){
                return;
            }
            for (Purchase p : mHelper.mInv.mPurchaseMap.values()) {
                logPrint("consumedAllItems...p.getSku()=="+p.getSku());
                try {
                    mHelper.consumeAsync(p, mConsumeFinishedListener);
                } catch (IabAsyncInProgressException e) {
                    logPrint("consumedAllItems...IabAsyncInProgressException,e=="+e.toString());
                    return;
                }
                return;
            }
        }
    }

    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        return true;
    }

    public void onDestroy() {
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(payAct);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        logPrint("Showing alert dialog: " + message);
        bld.create().show();
    }


}
