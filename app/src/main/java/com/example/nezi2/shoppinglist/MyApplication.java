package com.example.nezi2.shoppinglist;

import android.app.Application;

import com.firebase.client.Firebase;
import com.flurry.android.FlurryAgent;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        //Init Firebase
        Firebase.setAndroidContext(this);

        // configure Flurry
        FlurryAgent.setLogEnabled(false);

        // init Flurry
        FlurryAgent.init(this, "3WBQKR6MCGYQNGQN4B5X");
    }
}