package com.tools.listener;

import java.util.ArrayList;

public class FunctionCalledListener {


    public static int PAY_STATE_CONSUME_SUCCESS = 0;
    public static int PAY_STATE_QUERRY_OWNEDSKU_SUCCESS = 0;

    public static int PAY_STATE_SERVICE_READY = 1999;
    public static int PAY_STATE_PROCESS_PURCHASE = 2000;//purchased
    public static int PAY_STATE_PROCESS_PURCHASE_CANCELLED = 2001;//
    public static int PAY_STATE_PROCESS_PURCHASE_DONE = 2002;//purcahse over

    public static int PAY_STATE_PROCESS_CONSUME = 3000;//consume

    public interface OnPayProcessListener {
        void onProcess(int processCode,com.googlepay.util.IabResult result, com.googlepay.util.Purchase info);
    }

    public interface OnPaySuccessListener{
        void onSuccess(int code, com.googlepay.util.Purchase info);
    }

    public interface OnQuerryOwnedSkuListener{
        void onQuerryOwnedSku(int code, ArrayList<String> ownedSkus);
    }

}
