package lib.kalu.jsbridge;


/**
 * description: js调用java, java响应内容
 * created by kalu on 2017/7/5 01:17
 */
public interface OnJsCallJavaChangeListener {

    /**
     * @param jsData         js返回值
     * @param jsFunctionName js方法名
     */
    void onJsCallJava(String jsData, String jsFunctionName, OnJavaCallJsChangeListener androidCallJsListener);
}