package com.pyinal.coldweather.util;


import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by admin on 2018/6/10.
 */

public class HttpUtil {
    public static void sendOkhttpResquest(String address,okhttp3.Callback callback){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }
}
