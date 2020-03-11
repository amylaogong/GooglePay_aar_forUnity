/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlepay.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.text.format.Time;

import com.android.vending.billing.IInAppBillingService;
import com.google.pay.GooglePay;
import com.tools.listener.FunctionCalledListener;
import com.unity.callback.AndroidUnityInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides convenience methods for in-app billing. You can create one instance of this
 * class for your application and use it to process in-app billing operations.
 * It provides synchronous (blocking) and asynchronous (non-blocking) methods for
 * many common in-app billing operations, as well as automatic signature
 * verification.
 *
 * After instantiating, you must perform setup in order to start using the object.
 * To perform setup, call the {@link #startSetup} method and provide a listener;
 * that listener will be notified when setup is complete, after which (and not before)
 * you may call other methods.
 *
 * After setup is complete, you will typically want to request an inventory of owned
 * items and subscriptions. See {@link #queryInventory}, {@link #queryInventoryAsync}
 * and related methods.
 *
 * When you are done with this object, don't forget to call {@link #dispose}
 * to ensure proper cleanup. This object holds a binding to the in-app billing
 * service, which will leak unless you dispose of it correctly. If you created
 * the object on an Activity's onCreate method, then the recommended
 * place to dispose of it is the Activity's onDestroy method. It is invalid to
 * dispose the object while an asynchronous operation is in progress. You can
 * call {@link #disposeWhenFinished()} to ensure that any in-progress operation
 * completes before the object is disposed.
 *
 * A note about threading: When using this object from a background thread, you may
 * call the blocking versions of methods; when using from a UI thread, call
 * only the asynchronous versions and handle the results via callbacks.
 * Also, notice that you can only call one asynchronous operation at a time;
 * attempting to start a second asynchronous operation while the first one
 * has not yet completed will result in an exception being thrown.
 *
 */
public class IabHelper {
    // Is debug logging enabled?
    boolean mDebugLog = false;
    String mDebugTag = "IabHelper";

    // Is setup done?
    boolean mSetupDone = false;

    // Has this object been disposed of? (If so, we should ignore callbacks, etc)
    boolean mDisposed = false;

    // Do we need to dispose this object after an in-progress asynchronous operation?
    boolean mDisposeAfterAsync = false;

    // Are subscriptions supported?
    boolean mSubscriptionsSupported = false;

    // Is subscription update supported?
    boolean mSubscriptionUpdateSupported = false;

    // Is an asynchronous operation in progress?
    // (only one at a time can be in progress)
    boolean mAsyncInProgress = false;

    // Ensure atomic access to mAsyncInProgress and mDisposeAfterAsync.
    private final Object mAsyncInProgressLock = new Object();

    // (for logging/debugging)
    // if mAsyncInProgress == true, what asynchronous operation is in progress?
    String mAsyncOperation = "";

    // Context we were passed during initialization
    Context mContext;
    Activity mPayActivity;

    // Connection to the service
    IInAppBillingService mService;
    ServiceConnection mServiceConn;

    // The request code used to launch purchase flow
    public int mRequestCode;

    // The item type of the current purchase flow
    String mPurchasingItemType;

    // Public key for verifying signature, in base64 encoding
    String mSignatureBase64 = null;

    public Map<String,String> mSkuPriceMap = new HashMap<String,String>();//each item and its price
    public Inventory mInv;


    // Billing response codes
    public static final int BILLING_RESPONSE_RESULT_OK = 0;
    public static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;
    public static final int BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE = 2;
    public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
    public static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4;
    public static final int BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5;
    public static final int BILLING_RESPONSE_RESULT_ERROR = 6;
    public static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;
    public static final int BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8;

    // IAB Helper error codes
    public static final int IABHELPER_ERROR_BASE = -1000;
    public static final int IABHELPER_REMOTE_EXCEPTION = -1001;
    public static final int IABHELPER_BAD_RESPONSE = -1002;
    public static final int IABHELPER_VERIFICATION_FAILED = -1003;
    public static final int IABHELPER_SEND_INTENT_FAILED = -1004;
    public static final int IABHELPER_USER_CANCELLED = -1005;
    public static final int IABHELPER_UNKNOWN_PURCHASE_RESPONSE = -1006;
    public static final int IABHELPER_MISSING_TOKEN = -1007;
    public static final int IABHELPER_UNKNOWN_ERROR = -1008;
    public static final int IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE = -1009;
    public static final int IABHELPER_INVALID_CONSUMPTION = -1010;
    public static final int IABHELPER_SUBSCRIPTION_UPDATE_NOT_AVAILABLE = -1011;

    // Keys for the responses from InAppBillingService
    public static final String RESPONSE_CODE = "RESPONSE_CODE";
    public static final String RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST";
    public static final String RESPONSE_BUY_INTENT = "BUY_INTENT";
    public static final String RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
    public static final String RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE";
    public static final String RESPONSE_INAPP_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
    public static final String RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
    public static final String RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";
    public static final String INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";

    // Item types
    public static final String ITEM_TYPE_INAPP = "inapp";
    public static final String ITEM_TYPE_SUBS = "subs";

    // some fields on the getSkuDetails response bundle
    public static final String GET_SKU_DETAILS_ITEM_LIST = "ITEM_ID_LIST";
    public static final String GET_SKU_DETAILS_ITEM_TYPE_LIST = "ITEM_TYPE_LIST";



    /**
     * Creates an instance. After creation, it will not yet be ready to use. You must perform
     * setup by calling {@link #startSetup} and wait for setup to complete. This constructor does not
     * block and is safe to call from a UI thread.
     *
     * @param ctx Your application or Activity context. Needed to bind to the in-app billing service.
     * @param base64PublicKey Your application's public key, encoded in base64.
     *     This is used for verification of purchase signatures. You can find your app's base64-encoded
     *     public key in your application's page on Google Play Developer Console. Note that this
     *     is NOT your "developer public key".
     */
    public IabHelper(Context ctx, String base64PublicKey) {
        mPayActivity = (Activity) ctx;
        mContext = ctx.getApplicationContext();
        mSignatureBase64 = base64PublicKey;
        logDebug("IAB helper created.");

    }

    /**
     * Enables or disable debug logging through LogCat.
     */
    public void enableDebugLogging(boolean enable, String tag) {
        checkNotDisposed();
        mDebugLog = enable;
        mDebugTag = tag;
    }

    public void enableDebugLogging(boolean enable) {
        checkNotDisposed();
        mDebugLog = enable;
    }

    /**
     * Callback for setup process. This listener's {@link #onIabSetupFinished} method is called
     * when the setup process is complete.
     */
    public interface OnIabSetupFinishedListener {
        /**
         * Called to notify that setup is complete.
         *
         * @param result The result of the setup process.
         */
        void onIabSetupFinished(com.googlepay.util.IabResult result);
    }

    /**
     * Starts the setup process. This will start up the setup process asynchronously.
     * You will be notified through the listener when the setup process is complete.
     * This method is safe to call from a UI thread.
     *
     * @param listener The listener to notify when the setup process is complete.
     */
    public void startSetup(final OnIabSetupFinishedListener listener) {
        // If already set up, can't do it again.
        mDisposed = false;
        mPurchasingItemType = "inapp";
        mSkuPriceMap.clear();
        checkNotDisposed();
        if (mSetupDone) throw new IllegalStateException("IAB helper is already set up.");

        // Connection to IAB service
        logDebug("Starting in-app billing setup.");
        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                logDebug("Billing service disconnected.");
                mService = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                logDebug("Billing service connected.");
                if (mDisposed) return;

                mService = IInAppBillingService.Stub.asInterface(service);
                String packageName = mContext.getPackageName();
                try {
                    logDebug("Checking for in-app billing 3 support.");

                    // check for in-app billing v3 support
                    int response = mService.isBillingSupported(3, packageName, ITEM_TYPE_INAPP);
                    logDebug("Billing service connected.response=="+response);
                    if (response != BILLING_RESPONSE_RESULT_OK) {
                        if (listener != null) listener.onIabSetupFinished(new com.googlepay.util.IabResult(response,
                                "Error checking for billing v3 support."));

                        // if in-app purchases aren't supported, neither are subscriptions
                        mSubscriptionsSupported = false;
                        mSubscriptionUpdateSupported = false;
                        return;
                    } else {
                        logDebug("In-app billing version 3 supported for " + packageName);
                    }

                    // Check for v5 subscriptions support. This is needed for
                    // getBuyIntentToReplaceSku which allows for subscription update
                    response = mService.isBillingSupported(5, packageName, ITEM_TYPE_SUBS);
                    if (response == BILLING_RESPONSE_RESULT_OK) {
                        logDebug("Subscription re-signup AVAILABLE.");
                        mSubscriptionUpdateSupported = true;
                    } else {
                        logDebug("Subscription re-signup not available.");
                        mSubscriptionUpdateSupported = false;
                    }

                    if (mSubscriptionUpdateSupported) {
                        mSubscriptionsSupported = true;
                    } else {
                        // check for v3 subscriptions support
                        response = mService.isBillingSupported(3, packageName, ITEM_TYPE_SUBS);
                        if (response == BILLING_RESPONSE_RESULT_OK) {
                            logDebug("Subscriptions AVAILABLE.");
                            mSubscriptionsSupported = true;
                        } else {
                            logDebug("Subscriptions NOT AVAILABLE. Response: " + response);
                            mSubscriptionsSupported = false;
                            mSubscriptionUpdateSupported = false;
                        }
                    }

                    mSetupDone = true;
                }
                catch (RemoteException e) {
                    if (listener != null) {
                        listener.onIabSetupFinished(new com.googlepay.util.IabResult(IABHELPER_REMOTE_EXCEPTION,
                                "RemoteException while setting up in-app billing."));
                    }
                    e.printStackTrace();
                    return;
                }

                if (listener != null) {
                    logDebug("startSetup(),onServiceConnected succeed,listener=="+listener);
                    listener.onIabSetupFinished(new com.googlepay.util.IabResult(BILLING_RESPONSE_RESULT_OK, "Setup successful."));

                }
            }
        };

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        List<ResolveInfo> intentServices = mContext.getPackageManager().queryIntentServices(serviceIntent, 0);
        logDebug("startSetup(),intentServices=="+intentServices);
        String packageName = mContext.getPackageName();
        logDebug("startSetup(),packageName=="+packageName);

        //mContext.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);

        if (intentServices != null && !intentServices.isEmpty()) {
            // service available to handle that Intent
            logDebug("startSetup(),mContext.bindService...");
            mContext.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
        }
        else {
            // no service available to handle that Intent
            if (listener != null) {
                logDebug("startSetup(), listener.onIabSetupFinished");
                listener.onIabSetupFinished(
                        new com.googlepay.util.IabResult(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE,
                                "Billing service unavailable on device."));
            }
        }
    }

    /**
     * Dispose of object, releasing resources. It's very important to call this
     * method when you are done with this object. It will release any resources
     * used by it such as service connections. Naturally, once the object is
     * disposed of, it can't be used again.
     */
    public void dispose() throws IabAsyncInProgressException {
        synchronized (mAsyncInProgressLock) {
            if (mAsyncInProgress) {
                throw new IabAsyncInProgressException("Can't dispose because an async operation " +
                    "(" + mAsyncOperation + ") is in progress.");
            }
        }
        logDebug("Disposing.");
        mSetupDone = false;
        if (mServiceConn != null) {
            logDebug("Unbinding from service.");
            if (mContext != null) mContext.unbindService(mServiceConn);
        }
        mDisposed = true;
        mContext = null;
        mServiceConn = null;
        mService = null;
        mPurchaseListener = null;
    }

    /**
     * Disposes of object, releasing resources. If there is an in-progress async operation, this
     * method will queue the dispose to occur after the operation has finished.
     */
    public void disposeWhenFinished() {
        synchronized (mAsyncInProgressLock) {
            if (mAsyncInProgress) {
                logDebug("Will dispose after async operation finishes.");
                mDisposeAfterAsync = true;
            } else {
                try {
                    dispose();
                } catch (IabAsyncInProgressException e) {
                    // Should never be thrown, because we call dispose() only after checking that
                    // there's not already an async operation in progress.
                }
            }
        }
    }

    private void checkNotDisposed() {
        if (mDisposed) throw new IllegalStateException("IabHelper was disposed of, so it cannot be used.");
    }

    /** Returns whether subscriptions are supported. */
    public boolean subscriptionsSupported() {
        checkNotDisposed();
        return mSubscriptionsSupported;
    }


    /**
     * Callback that notifies when a purchase is finished.
     */
    public interface OnIabPurchaseFinishedListener {
        /**
         * Called to notify that an in-app purchase finished. If the purchase was successful,
         * then the sku parameter specifies which item was purchased. If the purchase failed,
         * the sku and extraData parameters may or may not be null, depending on how far the purchase
         * process went.
         *
         * @param result The result of the purchase.
         * @param info The purchase information (null if purchase failed)
         */
        void onIabPurchaseFinished(com.googlepay.util.IabResult result, com.googlepay.util.Purchase info);
    }

    // The listener registered on launchPurchaseFlow, which we have to call back when
    // the purchase finishes
    public OnIabPurchaseFinishedListener mPurchaseListener;

    public void launchPurchaseFlow(Activity act, String sku, int requestCode, OnIabPurchaseFinishedListener listener)
        throws IabAsyncInProgressException {
        launchPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchPurchaseFlow(Activity act, String sku, int requestCode,
            OnIabPurchaseFinishedListener listener, String extraData)
        throws IabAsyncInProgressException {
        launchPurchaseFlow(act, sku, ITEM_TYPE_INAPP, null, requestCode, listener, extraData);
    }

    public void launchSubscriptionPurchaseFlow(Activity act, String sku, int requestCode,
            OnIabPurchaseFinishedListener listener) throws IabAsyncInProgressException {
        launchSubscriptionPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchSubscriptionPurchaseFlow(Activity act, String sku, int requestCode,
            OnIabPurchaseFinishedListener listener, String extraData)
        throws IabAsyncInProgressException {
        launchPurchaseFlow(act, sku, ITEM_TYPE_SUBS, null, requestCode, listener, extraData);
    }

    /**
     * Initiate the UI flow for an in-app purchase. Call this method to initiate an in-app purchase,
     * which will involve bringing up the Google Play screen. The calling activity will be paused
     * while the user interacts with Google Play, and the result will be delivered via the
     * activity's {@link android.app.Activity#onActivityResult} method, at which point you must call
     * this object's {@link #handleActivityResult} method to continue the purchase flow. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param act The calling activity.
     * @param sku The sku of the item to purchase.
     * @param itemType indicates if it's a product or a subscription (ITEM_TYPE_INAPP or
     *      ITEM_TYPE_SUBS)
     * @param oldSkus A list of SKUs which the new SKU is replacing or null if there are none
     * @param requestCode A request code (to differentiate from other responses -- as in
     *      {@link android.app.Activity#startActivityForResult}).
     * @param listener The listener to notify when the purchase process finishes
     * @param extraData Extra data (developer payload), which will be returned with the purchase
     *      data when the purchase completes. This extra data will be permanently bound to that
     *      purchase and will always be returned when the purchase is queried.
     */
    public void launchPurchaseFlow(Activity act, String sku, String itemType, List<String> oldSkus,
            int requestCode, OnIabPurchaseFinishedListener listener, String extraData)
        throws IabAsyncInProgressException {
        //flagEndAsync();
        logDebug("launchPurchaseFlow begin:(sku,itemType,requestCode,extraData)==(" + sku + ", " + itemType+","+requestCode+","+extraData);

        checkNotDisposed();
        checkSetupDone("launchPurchaseFlow");
        flagStartAsync("launchPurchaseFlow");
        com.googlepay.util.IabResult result;

        if (itemType.equals(ITEM_TYPE_SUBS) && !mSubscriptionsSupported) {
            com.googlepay.util.IabResult r = new com.googlepay.util.IabResult(IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE,
                    "Subscriptions are not available.");
            flagEndAsync();
            if (listener != null) listener.onIabPurchaseFinished(r, null);
            return;
        }

        try {
            logDebug("Constructing buy intent for " + sku + ", item type: " + itemType);
            Bundle buyIntentBundle;

            logDebug("launchPurchaseFlow begin:oldSkus==" + oldSkus);
            logDebug("launchPurchaseFlow begin:mSubscriptionUpdateSupported==" + mSubscriptionUpdateSupported);

            if (oldSkus == null || oldSkus.isEmpty()) {
                // Purchasing a new item or subscription re-signup
                buyIntentBundle = mService.getBuyIntent(3, mContext.getPackageName(), sku, itemType,
                        extraData);

                logDebug("launchPurchaseFlow mService.getBuyIntent(),sku==" + sku);
            } else {
                // Subscription upgrade/downgrade
                if (!mSubscriptionUpdateSupported) {
                    com.googlepay.util.IabResult r = new com.googlepay.util.IabResult(IABHELPER_SUBSCRIPTION_UPDATE_NOT_AVAILABLE,
                            "Subscription updates are not available.");
                    flagEndAsync();
                    if (listener != null) listener.onIabPurchaseFinished(r, null);
                    return;
                }
                buyIntentBundle = mService.getBuyIntentToReplaceSkus(5, mContext.getPackageName(),
                        oldSkus, sku, itemType, extraData);
                logDebug("launchPurchaseFlow begin:buyIntentBundle==" + buyIntentBundle);
            }
            int response = getResponseCodeFromBundle(buyIntentBundle);
            logDebug("launchPurchaseFlow begin:222response==" + response);
            if (response != BILLING_RESPONSE_RESULT_OK) {
                logDebug("Unable to buy item, Error response: " + getResponseDesc(response));
                flagEndAsync();
                result = new com.googlepay.util.IabResult(response, "Unable to buy item");
                if (listener != null) listener.onIabPurchaseFinished(result, null);
                return;
            }

            PendingIntent pendingIntent = buyIntentBundle.getParcelable(RESPONSE_BUY_INTENT);
            logDebug("Launching buy intent for " + sku + ". Request code: " + requestCode);
            logDebug("launchPurchaseFlow mService.getParcelable(),pendingIntent==" + pendingIntent);
            mRequestCode = requestCode;
            mPurchaseListener = listener;
            mPurchasingItemType = itemType;
            logDebug("launchPurchaseFlow begin:pendingIntent==" + pendingIntent);
            logDebug("launchPurchaseFlow pendingIntent.getIntentSender()==" + pendingIntent.getIntentSender());
//            act.startIntentSenderForResult(pendingIntent.getIntentSender(),
//                    requestCode, new Intent(),
//                    Integer.valueOf(0), Integer.valueOf(0),
//                    Integer.valueOf(0));
        }
//        catch (SendIntentException e) {
//            logDebug("3333SendIntentException while launching purchase flow for sku " + sku);
//            e.printStackTrace();
//            flagEndAsync();
//            logDebug("launchPurchaseFlow SendIntentException e==" + e);
//            result = new com.googlepay.util.IabResult(IABHELPER_SEND_INTENT_FAILED, "Failed to send intent.");
//            if (listener != null) listener.onIabPurchaseFinished(result, null);
//        }
        catch (RemoteException e) {
            logDebug("4444RemoteException while launching purchase flow for sku " + sku);
            e.printStackTrace();
            flagEndAsync();

            result = new com.googlepay.util.IabResult(IABHELPER_REMOTE_EXCEPTION, "Remote exception while starting purchase flow");
            if (listener != null) listener.onIabPurchaseFinished(result, null);
        }
    }

    /**
     * Handles an activity result that's part of the purchase flow in in-app billing. If you
     * are calling {@link #launchPurchaseFlow}, then you must call this method from your
     * Activity's {@link android.app.Activity@onActivityResult} method. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param requestCode The requestCode as you received it.
     * @param resultCode The resultCode as you received it.
     * @param data The data (Intent) as you received it.
     * @return Returns true if the result was related to a purchase flow and was handled;
     *     false if the result was not related to a purchase, in which case you should
     *     handle it normally.
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        com.googlepay.util.IabResult result;
        if (requestCode != mRequestCode) return false;

        checkNotDisposed();
        checkSetupDone("handleActivityResult");

        // end of async purchase operation that started on launchPurchaseFlow
        flagEndAsync();
        logDebug("handleActivityResult,data=="+data);
        if (data == null) {
            logDebug("Null data in IAB activity result.");
            result = new com.googlepay.util.IabResult(IABHELPER_BAD_RESPONSE, "Null data in IAB result");
            if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, null);
            return true;
        }

        int responseCode = getResponseCodeFromIntent(data);
        String purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA);
        String dataSignature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE);

        logDebug("handleActivityResult,responseCode=="+responseCode);
        logDebug("handleActivityResult,purchaseData=="+purchaseData);
        logDebug("handleActivityResult,dataSignature=="+dataSignature);

        logDebug("handleActivityResult,resultCode=="+resultCode);

        if (resultCode == Activity.RESULT_OK && responseCode == BILLING_RESPONSE_RESULT_OK) {
            logDebug("Successful resultcode from purchase activity.");
            logDebug("Purchase data: " + purchaseData);
            logDebug("Data signature: " + dataSignature);
            logDebug("Extras: " + data.getExtras());
            logDebug("Expected item type: " + mPurchasingItemType);

            if (purchaseData == null || dataSignature == null) {
                logDebug("BUG: either purchaseData or dataSignature is null.");
                logDebug("Extras: " + data.getExtras().toString());
                result = new com.googlepay.util.IabResult(IABHELPER_UNKNOWN_ERROR, "IAB returned null purchaseData or dataSignature");
                if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, null);
                return true;
            }

            com.googlepay.util.Purchase purchase = null;
            try {
                purchase = new com.googlepay.util.Purchase(mPurchasingItemType, purchaseData, dataSignature);
                String sku = purchase.getSku();

                // Verify signature
                if (!com.googlepay.util.Security.verifyPurchase(mSignatureBase64, purchaseData, dataSignature)) {
                    logDebug("Purchase signature verification FAILED for sku " + sku);
                    result = new com.googlepay.util.IabResult(IABHELPER_VERIFICATION_FAILED, "Signature verification failed for sku " + sku);
                    if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, purchase);
                    return true;
                }
                logDebug("Purchase signature successfully verified.");
            }
            catch (JSONException e) {
                logDebug("Failed to parse purchase data.");
                e.printStackTrace();
                result = new com.googlepay.util.IabResult(IABHELPER_BAD_RESPONSE, "Failed to parse purchase data.");
                if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, null);
                return true;
            }

            if (mPurchaseListener != null) {
                mPurchaseListener.onIabPurchaseFinished(new com.googlepay.util.IabResult(BILLING_RESPONSE_RESULT_OK, "Success"), purchase);
            }
        }
        else if (resultCode == Activity.RESULT_OK) {
            // result code was OK, but in-app billing response was not OK.
            logDebug("Result code was OK but in-app billing response was not OK: " + getResponseDesc(responseCode));
            if (mPurchaseListener != null) {
                result = new com.googlepay.util.IabResult(responseCode, "Problem purchashing item.");
                mPurchaseListener.onIabPurchaseFinished(result, null);
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED) {
            logDebug("Purchase canceled - Response: " + getResponseDesc(responseCode));
            result = new com.googlepay.util.IabResult(IABHELPER_USER_CANCELLED, "User canceled.");
            if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, null);
        }
        else {
            logDebug("Purchase failed. Result code: " + Integer.toString(resultCode)
                    + ". Response: " + getResponseDesc(responseCode));
            result = new com.googlepay.util.IabResult(IABHELPER_UNKNOWN_PURCHASE_RESPONSE, "Unknown purchase response.");
            if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, null);
        }
        return true;
    }

    public com.googlepay.util.Inventory queryInventory() throws com.googlepay.util.IabException {
        return queryInventory(false, null, null);
    }

    public Inventory querySkuOnwed()throws com.googlepay.util.IabException{
        if(mInv==null){
            mInv = new com.googlepay.util.Inventory();
        }
        int r = 0;
        try {
            r = queryPurchases(mInv, ITEM_TYPE_INAPP);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (r != BILLING_RESPONSE_RESULT_OK) {
            throw new com.googlepay.util.IabException(r, "Error refreshing inventory (querying owned items).");
        }
        return mInv;
    }

    /**
     * Queries the inventory. This will query all owned items from the server, as well as
     * information on additional skus, if specified. This method may block or take long to execute.
     * Do not call from a UI thread. For that, use the non-blocking version {@link #queryInventoryAsync}.
     *
     * @param querySkuDetails if true, SKU details (price, description, etc) will be queried as well
     *     as purchase information.
     * @param moreItemSkus additional PRODUCT skus to query information on, regardless of ownership.
     *     Ignored if null or if querySkuDetails is false.
     * @param moreSubsSkus additional SUBSCRIPTIONS skus to query information on, regardless of ownership.
     *     Ignored if null or if querySkuDetails is false.
     * @throws IabException if a problem occurs while refreshing the inventory.
     */
    public com.googlepay.util.Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus,
                                                                                List<String> moreSubsSkus) throws com.googlepay.util.IabException {
        checkNotDisposed();
        checkSetupDone("queryInventory");
        try {
            com.googlepay.util.Inventory inv = new com.googlepay.util.Inventory();
            int r = queryPurchases(inv, ITEM_TYPE_INAPP);
            if (r != BILLING_RESPONSE_RESULT_OK) {
                throw new com.googlepay.util.IabException(r, "Error refreshing inventory (querying owned items).");
            }

            if (querySkuDetails) {
                r = querySkuDetails(ITEM_TYPE_INAPP, inv, moreItemSkus);
                if (r != BILLING_RESPONSE_RESULT_OK) {
                    throw new com.googlepay.util.IabException(r, "Error refreshing inventory (querying prices of items).");
                }
            }

            // if subscriptions are supported, then also query for subscriptions
            if (mSubscriptionsSupported) {
                r = queryPurchases(inv, ITEM_TYPE_SUBS);
                if (r != BILLING_RESPONSE_RESULT_OK) {
                    throw new com.googlepay.util.IabException(r, "Error refreshing inventory (querying owned subscriptions).");
                }

                if (querySkuDetails) {
                    r = querySkuDetails(ITEM_TYPE_SUBS, inv, moreSubsSkus);
                    if (r != BILLING_RESPONSE_RESULT_OK) {
                        throw new com.googlepay.util.IabException(r, "Error refreshing inventory (querying prices of subscriptions).");
                    }
                }
            }

            return inv;
        }
        catch (RemoteException e) {
            throw new com.googlepay.util.IabException(IABHELPER_REMOTE_EXCEPTION, "Remote exception while refreshing inventory.", e);
        }
        catch (JSONException e) {
            throw new com.googlepay.util.IabException(IABHELPER_BAD_RESPONSE, "Error parsing JSON response while refreshing inventory.", e);
        }
    }

    /**
     * Listener that notifies when an inventory query operation completes.
     */
    public interface QueryInventoryFinishedListener {
        /**
         * Called to notify that an inventory query operation completed.
         *
         * @param result The result of the operation.
         * @param inv The inventory.
         */
        void onQueryInventoryFinished(com.googlepay.util.IabResult result, com.googlepay.util.Inventory inv);
    }

    public PendingIntent getPendingIntent(String chargeSku){
        Time time = new Time();
        time.setToNow();
        String payload = "order_"+time.format2445();
        logDebug("IabHelper.java,queryInventoryAsync,getSkuDetails,payload=="+payload);
        Bundle buyIntentBundle = null;
        PendingIntent pending = null;
        try {
            buyIntentBundle = mService.getBuyIntent(3,mContext.getPackageName(),chargeSku,"inapp",payload);
            int resCode = buyIntentBundle.getInt("RESPONSE_CODE");//RESPONSE_CODE
            logDebug("IabHelper.java,queryInventoryAsync,getBuyIntent,resCode=="+resCode);
            if(resCode==0){
                pending = buyIntentBundle.getParcelable("BUY_INTENT");
            }
        }catch (RemoteException e){
            logDebug("IabHelper.java,queryInventoryAsync,getSkuDetails RemoteException_error,e=="+e.toString());
        }

        logDebug("IabHelper.java,queryInventoryAsync,getBuyIntent,pending=="+pending);
        return pending;
    }


    /**
     * Asynchronous wrapper for inventory query. This will perform an inventory
     * query as described in {@link #queryInventory}, but will do so asynchronously
     * and call back the specified listener upon completion. This method is safe to
     * call from a UI thread.
     *
     * @param querySkuDetails as in {@link #queryInventory}
     * @param moreItemSkus as in {@link #queryInventory}
     * @param moreSubsSkus as in {@link #queryInventory}
     * @param listener The listener to notify when the refresh operation completes.
     */
    public void queryInventoryAsync(final boolean querySkuDetails, final List<String> moreItemSkus,
            final List<String> moreSubsSkus, final QueryInventoryFinishedListener listener)
        throws IabAsyncInProgressException {
        final Handler handler = new Handler();
        checkNotDisposed();
        checkSetupDone("queryInventory");
        flagStartAsync("refresh inventory");

        logDebug("queryInventoryAsync,begin...");

        (new Thread(new Runnable() {
            public void run() {
                com.googlepay.util.IabResult result = new com.googlepay.util.IabResult(BILLING_RESPONSE_RESULT_OK, "Inventory refresh successful.");
                com.googlepay.util.Inventory inv = null;
                try {
                    inv = queryInventory(querySkuDetails, moreItemSkus, moreSubsSkus);
                }
                catch (com.googlepay.util.IabException ex) {
                    result = ex.getResult();
                }

                flagEndAsync();

                final com.googlepay.util.IabResult result_f = result;
                final com.googlepay.util.Inventory inv_f = inv;
                if (!mDisposed && listener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            listener.onQueryInventoryFinished(result_f, inv_f);
                        }
                    });
                }
            }
        })).start();
    }

    public void querySkuDetails(final boolean querySkuDetails, final List<String> moreItemSkus,
                                    final List<String> moreSubsSkus, final QueryInventoryFinishedListener listener)
            throws IabAsyncInProgressException {
        final Handler handler = new Handler();
        checkNotDisposed();
        checkSetupDone("querySkuDetails");
        flagStartAsync("refresh inventory");

        logDebug("querySkuDetails,begin...");
        (new Thread(new Runnable(){
            public void run(){
                ArrayList<String> skuList = new ArrayList<String>();
                skuList.add("item_charge_1");
                skuList.add("item_charge_2");
                skuList.add("item_charge_30");
                Bundle querySkus = new Bundle();
                querySkus.putStringArrayList("ITEM_ID_LIST",skuList);
                logDebug("querySkuDetails,begin...mService=="+mService);
                String chargeType = "inapp";
                String chargeSku = "";

                try {
                    Bundle skuDetails = mService.getSkuDetails(3,mContext.getPackageName(),chargeType,querySkus);
                    logDebug("queryInventoryAsync,getSkuDetails,skuDetails=="+skuDetails.toString());
                    int response = skuDetails.getInt("RESPONSE_CODE");
                    logDebug("querySkuDetails,getSkuDetails,response=="+response);
                    if(response == 0){
                        ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");
                        for(String thisRes:responseList){
                            JSONObject obj = new JSONObject(thisRes);
                            String sku = obj.getString("productId");
                            String price = obj.getString("price");
                            logDebug("querySkuDetails,getSkuDetails,(sku.price)=="+sku+","+price);
                            mSkuPriceMap.put(sku,price);
                            chargeSku = sku;
                        }

                        //then find player owned items that not consumed...

                        try {
                            querySkuOnwed();
                        } catch (IabException e) {
                            e.printStackTrace();
                        }
                    }
                }catch (RemoteException e){
                    logDebug("querySkuDetails,getSkuDetails RemoteException_error,e=="+e.toString());
                }catch (JSONException e){
                    logDebug("querySkuDetails,getSkuDetails JSONException_error,e=="+e.toString());
                }
            }
        })).start();
    }

    public void gotoBuyItemBySku(String sku,int requestCode,OnIabPurchaseFinishedListener listener){
        mPurchaseListener = listener;
        PendingIntent pending = getPendingIntent(sku);
        logDebug("IabHelper.java,gotoBuyItemBySku(),pending=="+pending);
        if(pending==null){
            return;
        }
        mRequestCode = requestCode;
        try {
            mPayActivity.startIntentSenderForResult(pending.getIntentSender(), mRequestCode, new Intent(), Integer.valueOf(0),
                    Integer.valueOf(0), Integer.valueOf(0));
        }catch (android.content.IntentSender.SendIntentException e){
            logDebug("onBuyGasButtonClicked,startIntentSenderForResult,SendIntentException,e=="+e.toString());
        }
    }

    public void queryInventoryAsync(QueryInventoryFinishedListener listener)
        throws IabAsyncInProgressException{
        queryInventoryAsync(false, null, null, listener);
    }
    public void querySkuDetailsAsync(QueryInventoryFinishedListener listener)
            throws IabAsyncInProgressException{
        flagEndAsync();
        querySkuDetails(false, null, null, listener);
    }
    /**
     * Consumes a given in-app product. Consuming can only be done on an item
     * that's owned, and as a result of consumption, the user will no longer own it.
     * This method may block or take long to return. Do not call from the UI thread.
     * For that, see {@link #consumeAsync}.
     *
     * @param itemInfo The PurchaseInfo that represents the item to consume.
     * @throws IabException if there is a problem during consumption.
     */
    void consume(com.googlepay.util.Purchase itemInfo) throws com.googlepay.util.IabException {
        checkNotDisposed();
        checkSetupDone("consume");
        logDebug("consume()...itemInfo==" + itemInfo);
        logDebug("consume()...itemInfo.mItemType==" + itemInfo.mItemType);

        if (!itemInfo.mItemType.equals(ITEM_TYPE_INAPP)) {
            throw new com.googlepay.util.IabException(IABHELPER_INVALID_CONSUMPTION,
                    "Items of type '" + itemInfo.mItemType + "' can't be consumed.");
        }

        try {
            String token = itemInfo.getToken();
            String sku = itemInfo.getSku();
            logDebug("consume()...token==" + token);
            logDebug("consume()...sku==" + sku);
            if (token == null || token.equals("")) {
                logDebug("Can't consume "+ sku + ". No token.");
                throw new com.googlepay.util.IabException(IABHELPER_MISSING_TOKEN, "PurchaseInfo is missing token for sku: "
                        + sku + " " + itemInfo);
            }

            logDebug("Consuming sku: " + sku + ", token: " + token);
            int response = mService.consumePurchase(3, mContext.getPackageName(), token);
            logDebug("consume()...response==" + response);
            if (response == BILLING_RESPONSE_RESULT_OK) {
                logDebug("Successfully consumed sku: " + sku);
            }
            else {
                logDebug("Error consuming consuming sku " + sku + ". " + getResponseDesc(response));
                throw new com.googlepay.util.IabException(response, "Error consuming sku " + sku);
            }
        }
        catch (RemoteException e) {
            throw new com.googlepay.util.IabException(IABHELPER_REMOTE_EXCEPTION, "Remote exception while consuming. PurchaseInfo: " + itemInfo, e);
        }
    }

    /**
     * Callback that notifies when a consumption operation finishes.
     */
    public interface OnConsumeFinishedListener {
        /**
         * Called to notify that a consumption has finished.
         *
         * @param purchase The purchase that was (or was to be) consumed.
         * @param result The result of the consumption operation.
         */
        void onConsumeFinished(com.googlepay.util.Purchase purchase, com.googlepay.util.IabResult result);
    }

    /**
     * Callback that notifies when a multi-item consumption operation finishes.
     */
    public interface OnConsumeMultiFinishedListener {
        /**
         * Called to notify that a consumption of multiple items has finished.
         *
         * @param purchases The purchases that were (or were to be) consumed.
         * @param results The results of each consumption operation, corresponding to each
         *     sku.
         */
        void onConsumeMultiFinished(List<com.googlepay.util.Purchase> purchases, List<com.googlepay.util.IabResult> results);
    }

    /**
     * Asynchronous wrapper to item consumption. Works like {@link #consume}, but
     * performs the consumption in the background and notifies completion through
     * the provided listener. This method is safe to call from a UI thread.
     *
     * @param purchase The purchase to be consumed.
     * @param listener The listener to notify when the consumption operation finishes.
     */
    public void consumeAsync(com.googlepay.util.Purchase purchase, OnConsumeFinishedListener listener)
        throws IabAsyncInProgressException {
        checkNotDisposed();
        checkSetupDone("consume");
        List<com.googlepay.util.Purchase> purchases = new ArrayList<com.googlepay.util.Purchase>();
        purchases.add(purchase);
        logDebug("consumeAsync(),purchases.size=="+purchases.size()+",purchase.sku=="+purchase.getSku());
        flagEndAsync();
        consumeAsyncInternal(purchases, listener, null);
    }

    /**
     * Same as {@link #consumeAsync}, but for multiple items at once.
     * @param purchases The list of PurchaseInfo objects representing the purchases to consume.
     * @param listener The listener to notify when the consumption operation finishes.
     */
    public void consumeAsync(List<com.googlepay.util.Purchase> purchases, OnConsumeMultiFinishedListener listener)
        throws IabAsyncInProgressException {
        checkNotDisposed();
        checkSetupDone("consume");
        consumeAsyncInternal(purchases, null, listener);
    }

    /**
     * Returns a human-readable description for the given response code.
     *
     * @param code The response code
     * @return A human-readable string explaining the result code.
     *     It also includes the result code numerically.
     */
    public static String getResponseDesc(int code) {
        String[] iab_msgs = ("0:OK/1:User Canceled/2:Unknown/" +
                "3:Billing Unavailable/4:Item unavailable/" +
                "5:Developer Error/6:Error/7:Item Already Owned/" +
                "8:Item not owned").split("/");
        String[] iabhelper_msgs = ("0:OK/-1001:Remote exception during initialization/" +
                                   "-1002:Bad response received/" +
                                   "-1003:Purchase signature verification failed/" +
                                   "-1004:Send intent failed/" +
                                   "-1005:User cancelled/" +
                                   "-1006:Unknown purchase response/" +
                                   "-1007:Missing token/" +
                                   "-1008:Unknown error/" +
                                   "-1009:Subscriptions not available/" +
                                   "-1010:Invalid consumption attempt").split("/");

        if (code <= IABHELPER_ERROR_BASE) {
            int index = IABHELPER_ERROR_BASE - code;
            if (index >= 0 && index < iabhelper_msgs.length) return iabhelper_msgs[index];
            else return String.valueOf(code) + ":Unknown IAB Helper Error";
        }
        else if (code < 0 || code >= iab_msgs.length)
            return String.valueOf(code) + ":Unknown";
        else
            return iab_msgs[code];
    }


    // Checks that setup was done; if not, throws an exception.
    void checkSetupDone(String operation) {
        if (!mSetupDone) {
            logDebug("Illegal state for operation (" + operation + "): IAB helper is not set up.");
            throw new IllegalStateException("IAB helper is not set up. Can't perform operation: " + operation);
        }
    }

    // Workaround to bug where sometimes response codes come as Long instead of Integer
    int getResponseCodeFromBundle(Bundle b) {
        Object o = b.get(RESPONSE_CODE);
        if (o == null) {
            logDebug("Bundle with null response code, assuming OK (known issue)");
            return BILLING_RESPONSE_RESULT_OK;
        }
        else if (o instanceof Integer) return ((Integer)o).intValue();
        else if (o instanceof Long) return (int)((Long)o).longValue();
        else {
            logDebug("Unexpected type for bundle response code.");
            logDebug(o.getClass().getName());
            throw new RuntimeException("Unexpected type for bundle response code: " + o.getClass().getName());
        }
    }

    // Workaround to bug where sometimes response codes come as Long instead of Integer
    int getResponseCodeFromIntent(Intent i) {
        Object o = i.getExtras().get(RESPONSE_CODE);
        if (o == null) {
            logDebug("Intent with no response code, assuming OK (known issue)");
            return BILLING_RESPONSE_RESULT_OK;
        }
        else if (o instanceof Integer) return ((Integer)o).intValue();
        else if (o instanceof Long) return (int)((Long)o).longValue();
        else {
            logDebug("Unexpected type for intent response code.");
            logDebug(o.getClass().getName());
            throw new RuntimeException("Unexpected type for intent response code: " + o.getClass().getName());
        }
    }

    void flagStartAsync(String operation) throws IabAsyncInProgressException {
        synchronized (mAsyncInProgressLock) {
            if (mAsyncInProgress) {
                throw new IabAsyncInProgressException("Can't start async operation (" +
                    operation + ") because another async operation (" + mAsyncOperation +
                    ") is in progress.");
            }
            mAsyncOperation = operation;
            mAsyncInProgress = true;
            logDebug("Starting async operation: " + operation);
        }
    }

    public void flagEndAsync() {
        synchronized (mAsyncInProgressLock) {
            logDebug("flagEndAsync() mAsyncOperation== " + mAsyncOperation);
            mAsyncOperation = "";
            mAsyncInProgress = false;
            logDebug("flagEndAsync(), mDisposeAfterAsync==" + mDisposeAfterAsync);
            if (mDisposeAfterAsync) {
                try {
                    dispose();
                } catch (IabAsyncInProgressException e) {
                    // Should not be thrown, because we reset mAsyncInProgress immediately before
                    // calling dispose().
                }
            }
        }
    }

    /**
     * Exception thrown when the requested operation cannot be started because an async operation
     * is still in progress.
     */
    public static class IabAsyncInProgressException extends Exception {
        public IabAsyncInProgressException(String message) {
            super(message);
        }
    }

    int queryPurchases(com.googlepay.util.Inventory inv, String itemType) throws JSONException, RemoteException {
        // Query purchases
        logDebug("Querying owned items, item type: " + itemType);
        logDebug("Package name: " + mContext.getPackageName());
        boolean verificationFailed = false;
        String continueToken = null;

        do {
            logDebug("Calling getPurchases with continuation token: " + continueToken);
            Bundle ownedItems = mService.getPurchases(3, mContext.getPackageName(),
                    itemType, continueToken);

            int response = getResponseCodeFromBundle(ownedItems);
            logDebug("Owned items response: " + String.valueOf(response));
            if (response != BILLING_RESPONSE_RESULT_OK) {
                logDebug("getPurchases() failed: " + getResponseDesc(response));
                return response;
            }
            if (!ownedItems.containsKey(RESPONSE_INAPP_ITEM_LIST)
                    || !ownedItems.containsKey(RESPONSE_INAPP_PURCHASE_DATA_LIST)
                    || !ownedItems.containsKey(RESPONSE_INAPP_SIGNATURE_LIST)) {
                logDebug("Bundle returned from getPurchases() doesn't contain required fields.");
                return IABHELPER_BAD_RESPONSE;
            }

            ArrayList<String> ownedSkus = ownedItems.getStringArrayList(
                    RESPONSE_INAPP_ITEM_LIST);
            ArrayList<String> purchaseDataList = ownedItems.getStringArrayList(
                    RESPONSE_INAPP_PURCHASE_DATA_LIST);
            ArrayList<String> signatureList = ownedItems.getStringArrayList(
                    RESPONSE_INAPP_SIGNATURE_LIST);

            logDebug("getPurchases() ownedSkus.size()==: " + ownedSkus.size());
            logDebug("getPurchases() purchaseDataList.size()==: " + purchaseDataList.size());
            logDebug("getPurchases() signatureList.size()==: " + signatureList.size());

            AndroidUnityInterface.querryOwnedSkuListener.onQuerryOwnedSku(FunctionCalledListener.PAY_STATE_QUERRY_OWNEDSKU_SUCCESS,ownedSkus);

            for (int i = 0; i < purchaseDataList.size(); ++i) {
                String purchaseData = purchaseDataList.get(i);
                String signature = signatureList.get(i);
                String sku = ownedSkus.get(i);
                if (com.googlepay.util.Security.verifyPurchase(mSignatureBase64, purchaseData, signature)) {
                    logDebug("Sku is owned: " + sku);
                    com.googlepay.util.Purchase purchase = new com.googlepay.util.Purchase(itemType, purchaseData, signature);

                    if (TextUtils.isEmpty(purchase.getToken())) {
                        logDebug("BUG: empty/null token!");
                        logDebug("Purchase data: " + purchaseData);
                    }

                    // Record ownership and token
                    inv.addPurchase(purchase);
                }
                else {
                    logDebug("Purchase signature verification **FAILED**. Not adding item.");
                    logDebug("   Purchase data: " + purchaseData);
                    logDebug("   Signature: " + signature);
                    verificationFailed = true;
                }
            }

            continueToken = ownedItems.getString(INAPP_CONTINUATION_TOKEN);
            logDebug("Continuation token: " + continueToken);
        } while (!TextUtils.isEmpty(continueToken));

        return verificationFailed ? IABHELPER_VERIFICATION_FAILED : BILLING_RESPONSE_RESULT_OK;
    }

    int querySkuDetails(String itemType, com.googlepay.util.Inventory inv, List<String> moreSkus)
            throws RemoteException, JSONException {
        logDebug("Querying SKU details.");
        ArrayList<String> skuList = new ArrayList<String>();
        skuList.addAll(inv.getAllOwnedSkus(itemType));
        if (moreSkus != null) {
            for (String sku : moreSkus) {
                if (!skuList.contains(sku)) {
                    skuList.add(sku);
                    logDebug("querySkuDetails sku=="+sku);
                }
            }
        }

        if (skuList.size() == 0) {
            logDebug("queryPrices: nothing to do because there are no SKUs.");
            return BILLING_RESPONSE_RESULT_OK;
        }

        // Split the sku list in blocks of no more than 20 elements.
        ArrayList<ArrayList<String>> packs = new ArrayList<ArrayList<String>>();
        ArrayList<String> tempList;
        int n = skuList.size() / 20;
        int mod = skuList.size() % 20;
        for (int i = 0; i < n; i++) {
            tempList = new ArrayList<String>();
            for (String s : skuList.subList(i * 20, i * 20 + 20)) {
                tempList.add(s);
            }
            packs.add(tempList);
        }
        if (mod != 0) {
            tempList = new ArrayList<String>();
            for (String s : skuList.subList(n * 20, n * 20 + mod)) {
                tempList.add(s);
            }
            packs.add(tempList);
        }

        for (ArrayList<String> skuPartList : packs) {
            Bundle querySkus = new Bundle();
            querySkus.putStringArrayList(GET_SKU_DETAILS_ITEM_LIST, skuPartList);
            Bundle skuDetails = mService.getSkuDetails(3, mContext.getPackageName(),
                    itemType, querySkus);

            if (!skuDetails.containsKey(RESPONSE_GET_SKU_DETAILS_LIST)) {
                int response = getResponseCodeFromBundle(skuDetails);
                if (response != BILLING_RESPONSE_RESULT_OK) {
                    logDebug("getSkuDetails() failed: " + getResponseDesc(response));
                    return response;
                } else {
                    logDebug("getSkuDetails() returned a bundle with neither an error nor a detail list.");
                    return IABHELPER_BAD_RESPONSE;
                }
            }

            ArrayList<String> responseList = skuDetails.getStringArrayList(
                    RESPONSE_GET_SKU_DETAILS_LIST);

            for (String thisResponse : responseList) {
                com.googlepay.util.SkuDetails d = new com.googlepay.util.SkuDetails(itemType, thisResponse);
                logDebug("Got sku details: " + d);
                inv.addSkuDetails(d);
            }
        }

        return BILLING_RESPONSE_RESULT_OK;
    }

    void consumeAsyncInternal(final List<com.googlepay.util.Purchase> purchases,
                              final OnConsumeFinishedListener singleListener,
                              final OnConsumeMultiFinishedListener multiListener)
        throws IabAsyncInProgressException {
        final Handler handler = new Handler();
        flagStartAsync("consume");

        (new Thread(new Runnable() {
            public void run() {
                final List<com.googlepay.util.IabResult> results = new ArrayList<com.googlepay.util.IabResult>();

                logDebug("consumeAsyncInternal(),Thread begin,purchases.size=="+purchases.size());

                for (com.googlepay.util.Purchase purchase : purchases) {
                    try {
                        logDebug("consumeAsyncInternal(),Thread run consume()");
                        consume(purchase);
                        results.add(new com.googlepay.util.IabResult(BILLING_RESPONSE_RESULT_OK, "Successful consume of sku " + purchase.getSku()));
                    }
                    catch (com.googlepay.util.IabException ex) {
                        results.add(ex.getResult());
                    }
                }

                flagEndAsync();
                if (!mDisposed && singleListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            singleListener.onConsumeFinished(purchases.get(0), results.get(0));
                        }
                    });
                }
                if (!mDisposed && multiListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            multiListener.onConsumeMultiFinished(purchases, results);
                        }
                    });
                }
            }
        })).start();
    }

    void logDebug(String msg) {
        //System.out.println("msg=="+msg);
        GooglePay.logPrint(msg);
    }

}
