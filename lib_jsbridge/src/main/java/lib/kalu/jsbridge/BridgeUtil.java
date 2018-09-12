package lib.kalu.jsbridge;

import android.content.Context;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

final class BridgeUtil {
    final static String BRIDGE_SCHEMA = "bridgescheme://";

    // BRIDGE加载
    final static String BRIDGE_LOAD = BRIDGE_SCHEMA + "h5bridge";
    // BRIDGE有消息
    final static String BRIDGE_HAS_MESSAGE = BRIDGE_SCHEMA + "h5message"; // queue has message
    // BRIDGE返回数据(yy://return/{function}/content)
    final static String BRIDGE_RETURN_DATA = BRIDGE_SCHEMA + "h5return/";
    // BRIDGE获取消息
    final static String BRIDGE_FETCH_QUEUE = BRIDGE_RETURN_DATA + "_h5FetchQueue/";

    final static String EMPTY_STR = "";
    final static String UNDERLINE_STR = "_";
    final static String SPLIT_MARK = "/";
    final static String CALLBACK_ID_FORMAT = "JAVA_CB_%s";
    final static String JS_HANDLE_MESSAGE_FROM_JAVA = "javascript:WebViewJavascriptBridge._handleMessageFromObjC('%s');";
    final static String JS_FETCH_QUEUE_FROM_JAVA = "javascript:WebViewJavascriptBridge._h5FetchQueue();";

    static String parseFunctionName(String jsUrl) {
        return jsUrl.replace("javascript:WebViewJavascriptBridge.", "").replaceAll("\\(.*\\);", "");
    }

    static String getDataFromReturnUrl(String url) {
        if (url.startsWith(BRIDGE_FETCH_QUEUE)) {
            return url.replace(BRIDGE_FETCH_QUEUE, EMPTY_STR);
        }

        String temp = url.replace(BRIDGE_RETURN_DATA, EMPTY_STR);
        String[] functionAndData = temp.split(SPLIT_MARK);

        if (functionAndData.length >= 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < functionAndData.length; i++) {
                sb.append(functionAndData[i]);
            }
            return sb.toString();
        }
        return null;
    }

    static String getFunctionFromReturnUrl(String url) {
        String temp = url.replace(BRIDGE_RETURN_DATA, EMPTY_STR);
        String[] functionAndData = temp.split(SPLIT_MARK);
        if (functionAndData.length >= 1) {
            return functionAndData[0];
        }
        return null;
    }

    static void webViewLoadLocalJs(WebView view, String path) {
        String jsContent = assetFile2Str(view.getContext(), path);
        view.loadUrl("javascript:" + jsContent);
    }

    static String assetFile2Str(Context c, String urlStr) {
        InputStream in = null;
        try {
            in = c.getAssets().open(urlStr);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line;
            StringBuilder sb = new StringBuilder();
            do {
                line = bufferedReader.readLine();
                if (line != null && !line.matches("^\\s*\\/\\/.*")) {
                    sb.append(line);
                }
            } while (line != null);

            bufferedReader.close();
            in.close();

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}