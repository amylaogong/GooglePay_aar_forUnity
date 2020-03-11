package com.google.login;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;

public class GoogleSignInHelper {

    private static final int RESULT_INIT_GOOOGLE_SIGNIN_SUCCESS = 0;
    private static final int RESULT_INIT_GOOOGLE_SIGNIN_WITHOUT_CLIENT_ID = 1;
    private static final int RESULT_INIT_GOOOGLE_SIGNIN_WITHOUT_SECRET = 2;
    private static final int RESULT_INIT_GOOOGLE_SIGNIN_WITHOUT_SERVICES = 3;
    //

    public static final int RC_GET_AUTH_CODE = 9003;
    //
    public static GoogleApiClient mGoogleApiClient = null;
    //
    private ProgressDialog mProgressDialog;
    public static String mGoogleClientId;//web_client_id


    public static Activity mSignInActivity = null;
    //
    private static boolean isNeedLogout = false;
    private boolean isStartFirst = true;

    private static boolean isCommitLeadboard = false;
    private static boolean isGetLeadboard = false;
    private static String curLeadboardID;

    private GoogleSignInCallback mGoogleSignInCallback;
    //
//    public static String mGoogleClientId;
    private boolean checkState = false;
    //
    private static GoogleSignInHelper instance;

    private GoogleSignInHelper() {
    }

    public static void printDebug(String str){
        Log.i("TestGoogleSignIn",""+str);
    }

    public static synchronized GoogleSignInHelper getInstance() {
        if (null == instance) {
            instance = new GoogleSignInHelper();
        }
        return instance;
    }

    public void checkState(Activity activity) {
        checkState = true;
        //mGoogleClientId = "328571750760-gfgle265k00ou5hqp1knegsjcc5desbf.apps.googleusercontent.com";//webclientid
        mSignInActivity = activity;
    }

    public void setGoogleKey(String key){
        mGoogleClientId = key;
    }

    public boolean isReady() {
        return checkState;
    }

    public void requestLogin(Activity activity, boolean isNeedLogout, GoogleSignInCallback callback) {
        if (isReady()) {
            GoogleSignInHelper.printDebug("GoogleSignInHelper,requestLogin -> isReady()= " + isReady());
            mGoogleSignInCallback = callback;

            if (mGoogleClientId.length()<1) {
                if (null != GoogleSignInHelper.getInstance().getGoogleSignInCallback()) {
                    GoogleSignInHelper.getInstance().getGoogleSignInCallback().onError();
                }
                return;
            }
            GoogleSignInHelper.printDebug("GoogleSignInHelper，onCreate mGoogleClientId= " + mGoogleClientId);
            getSignInReady();

        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mSignInActivity);
            // mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
        GoogleSignInHelper.printDebug("GoogleSignInHelper,showProgressDialog()");
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
        GoogleSignInHelper.printDebug("GoogleSignInHelper,hideProgressDialog()");
    }


    public String getGoogleClientId() {
        return mGoogleClientId;
    }

    public GoogleSignInCallback getGoogleSignInCallback() {
        return mGoogleSignInCallback;
    }

    public interface GoogleSignInCallback {

        void onSuccess(String data, String uid, String name);// 旧版Google登录，data为AccessToken，新版Google登录，data为AuthCode

        void onCancel();

        void onError();
    }

    public void handleSignInResult(GoogleSignInResult result) {

        GoogleSignInHelper.printDebug("GoogleSignInHelper,handleSignInResult(),result=="+result);
        GoogleSignInHelper.printDebug("GoogleSignInHelper,handleSignInResult(),result.getStatus()=="+result.getStatus());
        GoogleSignInHelper.printDebug("GoogleSignInHelper,handleSignInResult(),result.isSuccess()=="+result.isSuccess());


        if (null != result && result.isSuccess()) {
            if (isNeedLogout) {
                isNeedLogout = false;
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            signOut();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } else { // Signed in successfully, show authenticated UI.
                hideProgressDialog();

                GoogleSignInAccount acct = result.getSignInAccount();
                final String uid = acct.getId();
                final String authCode = acct.getServerAuthCode();
                final String name = acct.getAccount().name;

                GoogleSignInHelper.printDebug("GoogleSignInHelper,handleSignInResult(),uid=="+uid);
                GoogleSignInHelper.printDebug("GoogleSignInHelper,handleSignInResult(),authCode=="+authCode);
                GoogleSignInHelper.printDebug("GoogleSignInHelper,handleSignInResult(),name=="+name);


                if (null != GoogleSignInHelper.getInstance().getGoogleSignInCallback()) {
                    GoogleSignInHelper.getInstance().getGoogleSignInCallback().onSuccess(authCode, uid, name);
                }
                isNeedLogout = false;
                boolean isConnected = mGoogleApiClient.isConnected();
                GoogleSignInHelper.printDebug("GoogleSignInHelper,handleSignInResult(),run,isConnected=="+isConnected);
//                AGSHelper.getInstance().setSignInWithGoogle(true);
//                finish();
            }
        } else {
            if (isStartFirst) {
                isStartFirst = false;
                signIn();
            } else {
                int statusCode = result.getStatus().getStatusCode();
                String statusString = result.getStatus().getStatusMessage();
                GoogleSignInHelper.printDebug("GoogleSignInHelper -> handleSignInResult():statusCode = " + statusCode);
                GoogleSignInHelper.printDebug("GoogleSignInHelper -> handleSignInResult():statusString = " + statusString);
                if (statusCode == Status.RESULT_CANCELED.getStatusCode() || 12501==statusCode) {
                    if (null != GoogleSignInHelper.getInstance().getGoogleSignInCallback()) {
                        GoogleSignInHelper.getInstance().getGoogleSignInCallback().onCancel();
                    }
                } else {
                    if (null != GoogleSignInHelper.getInstance().getGoogleSignInCallback()) {
                        GoogleSignInHelper.getInstance().getGoogleSignInCallback().onError();
                    }
                }
//                finish();
            }
        }
    }

    private void signIn() {
        GoogleSignInHelper.printDebug("GoogleSignInHelper,signIn(),begin...");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        mSignInActivity.startActivityForResult(signInIntent, RC_GET_AUTH_CODE);
    }

    public static void signOut() {
        GoogleSignInHelper.printDebug("GoogleSignInHelper,signOut(),begin...");
        if(!GoogleSignInHelper.getInstance().isReady()){
            return;
        }
        boolean isConnected = mGoogleApiClient.isConnected();
        GoogleSignInHelper.printDebug("GoogleSignInHelper,signOut(),run,isConnected=="+isConnected);

        if(!isConnected){
            return;
        }

        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {

            @Override
            public void onResult(Status status) {
                isNeedLogout = false;
//                AGSHelper.getInstance().setSignInWithGoogle(false);
                instance.hideProgressDialog();
                GoogleSignInHelper.printDebug("GoogleSignInHelper,signOut(),onResult,status=="+status);
                GoogleSignInHelper.printDebug("GoogleSignInHelper,signOut(),onResult,status.getStatusMessage()=="+status.getStatusMessage());

                mGoogleApiClient = null;
                instance = null;
            }
        });
    }

    private static void getSignInReady() {
//        instance.showProgressDialog();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                .requestServerAuthCode(mGoogleClientId).requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(mSignInActivity).addConnectionCallbacks(
                new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        GoogleSignInHelper.printDebug("GoogleSignInHelper，getSignInReady,onConnected,2222,isConnected()=="+mGoogleApiClient.isConnected());
//                        String leaderID = "CgkI6JKcg8gJEAIQAg";
//                        Games.Leaderboards.submitScore(mGoogleApiClient,leaderID,12987);
//                        mSignInActivity.startActivityForResult(Games.Leaderboards.getLeaderboardIntent(mGoogleApiClient,leaderID),9336);


                        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

                        GoogleSignInHelper.printDebug("GoogleSignInHelper，opr.isDone()= " + opr.isDone());

                        if (opr.isDone()) {
                            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"and the GoogleSignInResult will be available instantly.
                            GoogleSignInResult result = opr.get();
                            instance.handleSignInResult(result);
                        } else {
                            // If the user has not previously signed in on this device or the sign-in has expired, this asynchronous branch will attempt to sign in the user silently. Cross-device single sign-on will occur in this branch.
                            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {

                                @Override
                                public void onResult(GoogleSignInResult googleSignInResult) {
                                    GoogleSignInHelper.printDebug("GoogleSignInActivity，onResult googleSignInResult== " + googleSignInResult);
                                    instance.hideProgressDialog();
                                    instance.handleSignInResult(googleSignInResult);
                                }
                            });
                        }



                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        GoogleSignInHelper.printDebug("GoogleSignInHelper，getSignInReady,onConnectionSuspended,isConnected()=="+mGoogleApiClient.isConnected());
                        mGoogleApiClient.connect();
                    }
                }
        ).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(com.google.android.gms.common.ConnectionResult connectionResult) {
                GoogleSignInHelper.printDebug("GoogleSignInHelper，getSignInReady,onConnectionFailed,getErrorCode=="+connectionResult.getErrorCode());
                GoogleSignInHelper.printDebug("GoogleSignInHelper，getSignInReady,onConnectionFailed,getErrorMessage=="+connectionResult.getErrorMessage());
                GoogleSignInHelper.printDebug("GoogleSignInHelper，getSignInReady,onConnectionFailed,hasResolution=="+connectionResult.hasResolution());
                GoogleSignInHelper.printDebug("GoogleSignInHelper，getSignInReady,onConnectionFailed,isSuccess=="+connectionResult.isSuccess());
                mGoogleApiClient.connect();
            }
        })
//                .enableAutoManage(mSignInActivity, mSignInActivity)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
//                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        mGoogleApiClient.connect();

        GoogleSignInHelper.printDebug("GoogleSignInHelper，getSignInReady gso= " + gso);
        GoogleSignInHelper.printDebug("GoogleSignInHelper，getSignInReady mGoogleApiClient= " + mGoogleApiClient);


    }

    public static GoogleApiClient getGoogleApiClient(){
        if(mGoogleApiClient == null){
            getSignInReady();
        }
        return mGoogleApiClient;
    }


    public static void commit(String leadboardID,long score)
    {
        if (!mGoogleApiClient.isConnected())
        {
            isCommitLeadboard = true;
            mGoogleApiClient.connect();
            return;
        }

        Games.Leaderboards.submitScore(mGoogleApiClient,leadboardID,score);
    }

}
