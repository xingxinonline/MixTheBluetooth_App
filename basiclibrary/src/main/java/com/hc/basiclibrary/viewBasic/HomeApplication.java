package com.hc.basiclibrary.viewBasic;

import android.app.Application;

import com.jeremyliao.liveeventbus.LiveEventBus;

public class HomeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LiveEventBus.config().autoClear(true)
                .lifecycleObserverAlwaysActive(true).enableLogger(false);
    }
}
