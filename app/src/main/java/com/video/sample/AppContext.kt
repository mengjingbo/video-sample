package com.video.sample

import android.app.Application

/**
 * date        ：2020/7/31
 * author      ：蒙景博
 * description ：
 */
class AppContext: Application() {

    override fun onCreate() {
        super.onCreate()
        MediaPlayerManager.getDefault().init(this, packageName)
    }
}