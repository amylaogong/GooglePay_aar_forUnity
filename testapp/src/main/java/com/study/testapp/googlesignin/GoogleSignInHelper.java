package com.study.testapp.googlesignin;

import android.app.Activity;
import android.content.Intent;

public class GoogleSignInHelper {

    private static final int RESULT_INIT_GOOOGLE_SIGNIN_SUCCESS = 0;
    private static final int RESULT_INIT_GOOOGLE_SIGNIN_WITHOUT_CLIENT_ID = 1;
    private static final int RESULT_INIT_GOOOGLE_SIGNIN_WITHOUT_SECRET = 2;
    private static final int RESULT_INIT_GOOOGLE_SIGNIN_WITHOUT_SERVICES = 3;
    //
    private GoogleSignInCallback mGoogleSignInCallback;
    //
    private String mGoogleClientId;
    private boolean checkState = false;
    //
    private static GoogleSignInHelper instance;

    private GoogleSignInHelper() {
    }

    public static synchronized GoogleSignInHelper getInstance() {
        if (null == instance) {
            instance = new GoogleSignInHelper();
        }
        return instance;
    }

    public void checkState(Activity activity) {
        checkState = true;
        mGoogleClientId = "968066674538-veuevr5topvuk9tvdvo6t87facrc9qv4.apps.googleusercontent.com";
    }

    public boolean isReady() {
        return checkState;
    }

    public void requestLogin(Activity activity, boolean isNeedLogout, GoogleSignInCallback callback) {
        if (isReady()) {
            mGoogleSignInCallback = callback;
            if (null != activity) {
                Intent intent = new Intent(activity, GoogleSignInActivity.class);
                intent.putExtra("isNeedLogout", isNeedLogout);
                activity.startActivity(intent);
            }
        }
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

}
