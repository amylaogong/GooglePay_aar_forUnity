package com.study.runactivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

public class MainActivity extends AppCompatActivity {

    public TextView logView = null;
    private Button btnlogin = null;
    private Button btnlogout = null;
    public static MainActivity mAct = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAct = this;
        setContentView(R.layout.activity_main);
        logView = findViewById(R.id.textView);
        btnlogin = findViewById(R.id.btnlogin);
        btnlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Runnable() {
                    @Override
                    public void run() {
                        googleLogin();
                    }
                }.run();

            }
        });
        btnlogout = findViewById(R.id.btnlogout);
        btnlogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Runnable() {
                    @Override
                    public void run() {
                        googleLogout();
                    }
                }.run();

            }
        });

        GoogleSignInHelper.printDebug("MainActivity  onCreate");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);

        GoogleSignInHelper.printDebug("MainActivity，onActivityResult requestCode== " + requestCode);
        GoogleSignInHelper.printDebug("MainActivity，onActivityResult resultCode== " + resultCode);
        GoogleSignInHelper.printDebug("MainActivity，onActivityResult data== " + data);

        if (requestCode == GoogleSignInHelper.RC_GET_AUTH_CODE) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            GoogleSignInHelper.getInstance().handleSignInResult(result);
        }
    }

    public void googleLogin(){
        String googleKey = "328571750760-gfgle265k00ou5hqp1knegsjcc5desbf.apps.googleusercontent.com";
        GoogleSignInHelper.getInstance().setGoogleKey(googleKey);
        GoogleSignInHelper.getInstance().checkState(this);

        GoogleSignInHelper.getInstance().requestLogin(this, false, new GoogleSignInHelper.GoogleSignInCallback() {

            @Override
            public void onSuccess(String authCode, String uid, String name) {
                GoogleSignInHelper.printDebug("MainActivity,googleLogin -> onSuccess():authCode = " + authCode);
                GoogleSignInHelper.printDebug("MainActivity,googleLogin -> onSuccess():uid = " + uid);

                GoogleSignInHelper.printDebug("MainActivity,googleLogin -> onSuccess():name = " + name);

                logView.setText("authCode:"+authCode+"\n"+"uid:"+uid+"\n"+"name:"+name+"\n");

                GoogleSignInHelper.printDebug("MainActivity,googleLogin -> onSuccess() 222:GoogleSignInHelper.mGoogleApiClient = " + GoogleSignInHelper.mGoogleApiClient);
                boolean isConnected = GoogleSignInHelper.mGoogleApiClient.isConnected();
                GoogleSignInHelper.printDebug("MainActivity,googleLogin,onSuccess(),222 mGoogleApiClient.isConnected=="+isConnected);

            }

            @Override
            public void onError() {
                GoogleSignInHelper.printDebug("GoogleSignInCallback -> onError()");
                logView.setText("GoogleSignInCallback -> onError()");
            }

            @Override
            public void onCancel() {
                GoogleSignInHelper.printDebug("GoogleSignInCallback -> onCancel()");
                logView.setText("GoogleSignInCallback -> onCancel()");
            }
        });


    }

    private void googleLogout(){
        GoogleSignInHelper.printDebug("MainActivity,googleLogin -> onSuccess():222 GoogleSignInHelper.mGoogleApiClient = " + GoogleSignInHelper.mGoogleApiClient);
        GoogleSignInHelper.signOut();
    }

}
