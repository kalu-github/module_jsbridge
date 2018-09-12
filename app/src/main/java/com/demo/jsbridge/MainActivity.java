package com.demo.jsbridge;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.widget.Toast;

import lib.kalu.jsbridge.BridgeWebView;
import lib.kalu.jsbridge.OnJavaCallJsChangeListener;
import lib.kalu.jsbridge.OnJsCallJavaChangeListener;

public class MainActivity extends Activity {

    BridgeWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (BridgeWebView) findViewById(R.id.webView);

        webView.setWebChromeClient(new WebChromeClient());

        webView.loadUrl("file:///android_asset/demo.html");

        webView.setJsCallJava("JsCallJava", new OnJsCallJavaChangeListener() {

            @Override
            public void onJsCallJava(String data, String functionName, OnJavaCallJsChangeListener androidCallJsListener) {
                // step1
                Toast.makeText(getApplicationContext(), "Java原生方法: " + data, Toast.LENGTH_SHORT).show();
                // step2, 响应js结果
                androidCallJsListener.onJavaCallJs("Java原生响应: " + data);
            }
        });
        webView.setJavaCallJs("JavaCallJs", "{初始化成功}", new OnJavaCallJsChangeListener() {

            @Override
            public void onJavaCallJs(String jsData) {
                Toast.makeText(getApplicationContext(), "Java原生方法: " + jsData, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.android_call_js).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.setJavaCallJs("JavaCallJs", "{name: java => JavaCallJs}", new OnJavaCallJsChangeListener() {

                    @Override
                    public void onJavaCallJs(String data) {
                        Toast.makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
