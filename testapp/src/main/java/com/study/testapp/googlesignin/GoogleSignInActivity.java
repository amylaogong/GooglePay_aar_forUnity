package com.study.testapp.googlesignin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

public class GoogleSignInActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final int RC_GET_AUTH_CODE = 9003;
    //
    private static GoogleApiClient mGoogleApiClient;
    //
    private ProgressDialog mProgressDialog;
    private String mGoogleClientId;
    //
    private static boolean isNeedLogout = false;
    private boolean isStartFirst = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isNeedLogout = getIntent().getBooleanExtra("isNeedLogout", false);
        mGoogleClientId = "968066674538-veuevr5topvuk9tvdvo6t87facrc9qv4.apps.googleusercontent.com";
//        AGSTester.printDebug("GoogleSignInActivity -> onCreate():mGoogleClientId = " + mGoogleClientId);
        if (mGoogleClientId.length()<1) {
            if (null != GoogleSignInHelper.getInstance().getGoogleSignInCallback()) {
                GoogleSignInHelper.getInstance().getGoogleSignInCallback().onError();
            }
            finish();
        }
        getSignInReady();
    }

    @Override
    protected void onStart() {
        super.onStart();
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"and the GoogleSignInResult will be available instantly.
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired, this asynchronous branch will attempt to sign in the user silently. Cross-device single sign-on will occur in this branch.
            showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {

                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    hideProgressDialog();
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideProgressDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_GET_AUTH_CODE) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (null != GoogleSignInHelper.getInstance().getGoogleSignInCallback()) {
                GoogleSignInHelper.getInstance().getGoogleSignInCallback().onCancel();
            }
            finish();
        }
        return true;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not be available.
        if (null != GoogleSignInHelper.getInstance().getGoogleSignInCallback()) {
            GoogleSignInTest.printDebug("GoogleSignInActivity -> onConnectionFailed():errCode = " + connectionResult.getErrorCode());
            GoogleSignInTest.printDebug("GoogleSignInActivity -> onConnectionFailed():errMessage = " + connectionResult.getErrorMessage());
            GoogleSignInHelper.getInstance().getGoogleSignInCallback().onError();
        }
        finish();
    }

    private void signIn() {
        GoogleSignInTest.printDebug("signIn(),begin...");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_GET_AUTH_CODE);
    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {

            @Override
            public void onResult(Status status) {
                isNeedLogout = false;
//                AGSHelper.getInstance().setSignInWithGoogle(false);

                GoogleSignInTest.printDebug("signOut(),onResult,status=="+status);

//                signIn();
            }
        });
    }


    private void handleSignInResult(GoogleSignInResult result) {

        GoogleSignInTest.printDebug("handleSignInResult(),result=="+result);

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
                GoogleSignInAccount acct = result.getSignInAccount();
                final String uid = acct.getId();
                final String authCode = acct.getServerAuthCode();
                final String name = acct.getAccount().name;

                GoogleSignInTest.printDebug("handleSignInResult(),uid=="+uid);
                GoogleSignInTest.printDebug("handleSignInResult(),authCode=="+authCode);
                GoogleSignInTest.printDebug("handleSignInResult(),name=="+name);

                if (null != GoogleSignInHelper.getInstance().getGoogleSignInCallback()) {
                    GoogleSignInHelper.getInstance().getGoogleSignInCallback().onSuccess(authCode, uid, name);
                }
                isNeedLogout = false;
//                AGSHelper.getInstance().setSignInWithGoogle(true);
                finish();
            }
        } else {
            if (isStartFirst) {
                isStartFirst = false;
                signIn();
            } else {
                int statusCode = result.getStatus().getStatusCode();
                String statusString = result.getStatus().getStatusMessage();
                GoogleSignInTest.printDebug("GoogleSignInActivity -> handleSignInResult():statusCode = " + statusCode);
                GoogleSignInTest.printDebug("GoogleSignInActivity -> handleSignInResult():statusString = " + statusString);
                if (statusCode == 12501) {
                    if (null != GoogleSignInHelper.getInstance().getGoogleSignInCallback()) {
                        GoogleSignInHelper.getInstance().getGoogleSignInCallback().onCancel();
                    }
                } else {
                    if (null != GoogleSignInHelper.getInstance().getGoogleSignInCallback()) {
                        GoogleSignInHelper.getInstance().getGoogleSignInCallback().onError();
                    }
                }
                finish();
            }
        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            // mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    private void getSignInReady() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                .requestServerAuthCode(mGoogleClientId).requestEmail()
                .build();
        // GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
        // GoogleSignInOptions.DEFAULT_SIGN_IN)
        // .requestIdToken(googleServerClientId).requestEmail().build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();
    }

}
