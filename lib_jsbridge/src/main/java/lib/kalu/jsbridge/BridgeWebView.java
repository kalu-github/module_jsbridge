package lib.kalu.jsbridge;

import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BridgeWebView extends WebView {

    public static final String toLoadJs = "jsbridge.js";
    private final Map<String, OnJavaCallJsChangeListener> javaCallJs = Collections.synchronizedMap(new HashMap<String, OnJavaCallJsChangeListener>());
    private final Map<String, OnJsCallJavaChangeListener> jsCallJava = Collections.synchronizedMap(new HashMap<String, OnJsCallJavaChangeListener>());

    private long uniqueId = 0;
    private List<BridgeMessage> bridgeMessageQueue = Collections.synchronizedList(new ArrayList<BridgeMessage>());

    /*********************************************************************/

    public BridgeWebView(Context context) {
        this(context, null, 0);
    }

    public BridgeWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BridgeWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        setWebViewClient(BridgeWebViewClient.getInstance());
    }

    /*********************************************************************/

    void handlerReturnData(String url) {

        final String callName = BridgeUtil.getFunctionFromReturnUrl(url);
        if (TextUtils.isEmpty(callName)) return;

        final OnJavaCallJsChangeListener listener = javaCallJs.get(callName);
        if (null == listener) return;

        final String data = BridgeUtil.getDataFromReturnUrl(url);
        if (TextUtils.isEmpty(data)) return;

        listener.onJavaCallJs(data);
        javaCallJs.remove(callName);
    }

    private void doSend(String handlerName, String data, OnJavaCallJsChangeListener responseCallback) {
        BridgeMessage m = new BridgeMessage();
        if (!TextUtils.isEmpty(data)) {
            m.setData(data);
        }
        if (responseCallback != null) {
            String callbackStr = String.format(BridgeUtil.CALLBACK_ID_FORMAT, ++uniqueId + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
            javaCallJs.put(callbackStr, responseCallback);
            m.setCallbackId(callbackStr);
        }
        if (!TextUtils.isEmpty(handlerName)) {
            m.setHandlerName(handlerName);
        }
        addMessageQueue(m);
    }

    void dispatchMessage(BridgeMessage m) {
        String messageJson = m.toJson();
        //escape special characters for json string
        messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
        String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            this.loadUrl(javascriptCommand);
        }
    }

    void flushMessageQueue() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            loadUrl(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, new OnJavaCallJsChangeListener() {

                @Override
                public void onJavaCallJs(String data) {
                    // deserializeMessage
                    List<BridgeMessage> list;
                    try {
                        list = BridgeMessage.toArrayList(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    if (list == null || list.size() == 0) {
                        return;
                    }
                    for (int i = 0; i < list.size(); i++) {
                        BridgeMessage m = list.get(i);
                        String responseId = m.getResponseId();
                        // 是否是response
                        if (!TextUtils.isEmpty(responseId)) {
                            OnJavaCallJsChangeListener function = javaCallJs.get(responseId);
                            String responseData = m.getResponseData();
                            function.onJavaCallJs(responseData);
                            javaCallJs.remove(responseId);
                        } else {
                            OnJavaCallJsChangeListener responseFunction;
                            // if had callbackId
                            final String callbackId = m.getCallbackId();
                            if (!TextUtils.isEmpty(callbackId)) {
                                responseFunction = new OnJavaCallJsChangeListener() {

                                    @Override
                                    public void onJavaCallJs(String data) {
                                        BridgeMessage responseMsg = new BridgeMessage();
                                        responseMsg.setResponseId(callbackId);
                                        responseMsg.setResponseData(data);
                                        addMessageQueue(responseMsg);
                                    }
                                };
                            } else {
                                responseFunction = new OnJavaCallJsChangeListener() {
                                    @Override
                                    public void onJavaCallJs(String data) {
                                        // do nothing
                                    }
                                };
                            }
                            OnJsCallJavaChangeListener handler = null;
                            if (!TextUtils.isEmpty(m.getHandlerName())) {
                                handler = jsCallJava.get(m.getHandlerName());
                            } else {
                                // no handler found
                            }
                            if (handler != null) {
                                handler.onJsCallJava(m.getData(), m.getHandlerName(), responseFunction);
                            }
                        }
                    }
                }
            });
        }
    }

    public void loadUrl(String jsUrl, OnJavaCallJsChangeListener returnCallback) {
        this.loadUrl(jsUrl);
        javaCallJs.put(BridgeUtil.parseFunctionName(jsUrl), returnCallback);
    }

    /*********************************************************************/

    /**
     * 添加消息
     */
    private void addMessageQueue(BridgeMessage m) {
        if (bridgeMessageQueue != null) {
            bridgeMessageQueue.add(m);
        } else {
            dispatchMessage(m);
        }
    }

    /**
     * 清空消息
     */
    protected void clearMessageQueue() {
        if (null == bridgeMessageQueue) return;
        bridgeMessageQueue.clear();
        bridgeMessageQueue = null;
    }

    /**
     * 获取消息队列
     */
    protected List<BridgeMessage> getBridgeMessageQueue() {
        if (null != bridgeMessageQueue) {
            return bridgeMessageQueue;
        } else {
            return null;
        }
    }

    /**
     * js 调用 android
     */
    public void setJsCallJava(String handlerName, OnJsCallJavaChangeListener handler) {
        if (null == handler) return;
        jsCallJava.put(handlerName, handler);
    }

    /**
     * android 调用 js
     */
    public void setJavaCallJs(String handlerName, String data, OnJavaCallJsChangeListener callBack) {
        doSend(handlerName, data, callBack);
    }
}