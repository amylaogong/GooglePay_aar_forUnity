package com.unity.callback;

import com.example.helloworld.MainActivity;
import com.google.pay.GooglePay;
import com.googlepay.util.IabResult;
import com.googlepay.util.Purchase;
import com.sdk.test.SdkListener;
import com.tools.listener.FunctionCalledListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class AndroidUnityInterface {

    public static void SetUnityCache(String key,String value){
        JSONObject obj;
        try {
            obj = new JSONObject();
            obj.put("function","SetCache");//告诉Unity本次通知的功能
            obj.put("key",key);
            obj.put("value",value);
            NotifyUnityWithJson(obj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static FunctionCalledListener.OnPayProcessListener payProcessListener = new FunctionCalledListener.OnPayProcessListener() {
        @Override
        public void onProcess(int code, IabResult result, Purchase info) {
            JSONObject obj;
            try {
                obj = new JSONObject();
                obj.put("function","OnCallBackPayProcess");
                obj.put("code",code);

                if(result!=null){
                    obj.put("IabHelerCode",result.getResponse());
                    obj.put("resultMsg",result.getMessage());
                }
                if(info!=null){
                    obj.put("googleOrderId",info.getOrderId());
                    obj.put("getSku",info.getSku());
                    obj.put("selfOrderId",info.getDeveloperPayload());

                    obj.put("getPurchaseTime",""+info.getPurchaseTime());
                    obj.put("getItemType",info.getItemType());
                    obj.put("getOriginalJson",info.getOriginalJson());
                    obj.put("getPackageName",info.getPackageName());
                    obj.put("getPurchaseState",info.getPurchaseState());
                    obj.put("getSignature",info.getSignature());
                    obj.put("getToken",info.getToken());
                }
                NotifyUnityWithJson(obj.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static FunctionCalledListener.OnPaySuccessListener paySuccessListener = new FunctionCalledListener.OnPaySuccessListener() {
        @Override
        public void onSuccess(int code, Purchase info) {
            GooglePay.logPrint( "AndroidUnityInterface.java..paySuccessListener().info=="+info);
            JSONObject obj;
            try {
                obj = new JSONObject();
                obj.put("function","OnCallBackPaySuccess");
                obj.put("code",code);
                if(info!=null){
                    obj.put("googleOrderId",info.getOrderId());
                    obj.put("getSku",info.getSku());
                    obj.put("selfOrderId",info.getDeveloperPayload());

                    obj.put("getPurchaseTime",""+info.getPurchaseTime());
                    obj.put("getItemType",info.getItemType());
                    obj.put("getOriginalJson",info.getOriginalJson());
                    obj.put("getPackageName",info.getPackageName());
                    obj.put("getPurchaseState",info.getPurchaseState());
                    obj.put("getSignature",info.getSignature());
                    obj.put("getToken",info.getToken());

                    GooglePay.logPrint( "AndroidUnityInterface.java..paySuccessListener().returnUnity=="+obj.toString());
                    NotifyUnityWithJson(obj.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static FunctionCalledListener.OnQuerryOwnedSkuListener querryOwnedSkuListener = new FunctionCalledListener.OnQuerryOwnedSkuListener() {
        @Override
        public void onQuerryOwnedSku(int code, ArrayList<String> ownedSkus) {
            GooglePay.logPrint( "AndroidUnityInterface.java..querryOwnedSkuListener().code=="+code);
            JSONObject obj;
            try {
                obj = new JSONObject();
                obj.put("function","OnCallBackQuerryOwnedSku");
                obj.put("code",code);
                if(ownedSkus!=null){
                    int size = ownedSkus.size();
                    obj.put("ownedSkuSize",size);
                    for(int i=0;i<size;i++){
                        obj.put("getSku_"+i,ownedSkus.get(i));
                    }
                    GooglePay.logPrint( "AndroidUnityInterface.java..querryOwnedSkuListener().returnUnity=="+obj.toString());
                    NotifyUnityWithJson(obj.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static SdkListener.OnLoginListener loginListener = new SdkListener.OnLoginListener() {
        @Override
        public void onLoginFinished(String jsonStr) {
            GooglePay.logPrint( "AndroidUnityInterface.java..onLoginFinished().jsonStr=="+jsonStr);
            NotifyUnityWithJson(jsonStr);
        }
    };


    public static void NotifyUnityWithJson(String jsonStr){
        MainActivity.AndroidCallUnity("OnNotifyWithJson",jsonStr);
    }

    public static String loadJSON(String url) {
        StringBuilder json = new StringBuilder();
        try {
            URL oracle = new URL(url);
            URLConnection yc = oracle.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream(),"utf-8"));//防止乱码
            String inputLine = null;
            while ((inputLine = in.readLine()) != null) {
                json.append(inputLine);
            }
            in.close();
        }
        catch (MalformedURLException e) {

        }
        catch (IOException e) {

        }
        return json.toString();
    }
}
