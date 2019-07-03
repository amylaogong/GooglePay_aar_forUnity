package com.unity.andorid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
    }

    public void test(String str)
    {
        Log.d("MainActivity","MainActivity.java,test(),str=="+str);

    }
}
