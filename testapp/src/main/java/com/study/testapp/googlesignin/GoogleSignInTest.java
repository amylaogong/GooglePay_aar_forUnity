package com.study.testapp.googlesignin;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class GoogleSignInTest extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        googleLogin();
    }


    public static void printDebug(String str){
        Log.i("TestGoogleSignIn",""+str);
    }


    public void googleLogin(){
        GoogleSignInHelper.getInstance().checkState(this);

        GoogleSignInHelper.getInstance().requestLogin(this, false, new GoogleSignInHelper.GoogleSignInCallback() {

            @Override
            public void onSuccess(String data, String uid, String name) {
                printDebug("GoogleSignInCallback -> onSuccess():data = " + data);
                printDebug("GoogleSignInCallback -> onSuccess():uid = " + uid);

                printDebug("GoogleSignInCallback -> onSuccess():name = " + name);

            }

            @Override
            public void onError() {
                printDebug("GoogleSignInCallback -> onError()");

            }

            @Override
            public void onCancel() {
                printDebug("GoogleSignInCallback -> onCancel()");

            }
        });


    }


}
