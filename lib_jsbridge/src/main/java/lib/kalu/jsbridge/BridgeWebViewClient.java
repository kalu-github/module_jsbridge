package lib.kalu.jsbridge;

import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.URLDecoder;
import java.util.List;

/**
 * description: WebViewClient
 * created by kalu on 2018/9/12 13:24
 */
final class BridgeWebViewClient extends WebViewClient {

    /*********************************************************************/

    private static class SingletonHolder {
        private final static BridgeWebViewClient instance = new BridgeWebViewClient();
    }

    public static BridgeWebViewClient getInstance() {
        return SingletonHolder.instance;
    }

    private BridgeWebViewClient() {
    }

    /*********************************************************************/

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {

        if (null == view) {
            return super.shouldOverrideUrlLoading(view, url);
        } else {

            try {
                url = URLDecoder.decode(url, "UTF-8");
            } catch (Exception e) {
                Log.e("kalu", e.getMessage(), e);
            }

            final BridgeWebView webView = (BridgeWebView) view;

            if (url.startsWith(BridgeUtil.BRIDGE_LOAD)) {
                BridgeUtil.webViewLoadLocalJs(view, BridgeWebView.toLoadJs);
                List<BridgeMessage> bridgeMessageQueue = webView.getBridgeMessageQueue();
                if (null != bridgeMessageQueue) {

                    // 1.遍历分发消息
                    for (BridgeMessage m : bridgeMessageQueue) {
                        webView.dispatchMessage(m);
                    }

                    // 2.清空消息队列
                    webView.clearMessageQueue();
                }
                return true;
            }
            // 如果是返回数据
            else if (url.startsWith(BridgeUtil.BRIDGE_RETURN_DATA)) {
                webView.handlerReturnData(url);
                return true;
            }
            // 消息队列有数据
            else if (url.startsWith(BridgeUtil.BRIDGE_HAS_MESSAGE)) {
                webView.flushMessageQueue();
                return true;
            } else {
                return super.shouldOverrideUrlLoading(view, url);
            }
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
    }
}