#
#### 特别提醒
```
此库需要js原生支持, js提供java端调用的方法, java提供js端调用的方法
```

#
#### 效果预览
![image](https://github.com/153437803/JsBridge/blob/master/Screenrecorder-2018-09-12.gif )


#
#### 使用方法
```
js调java方法

step1:java

BridgeWebView.setJsCallJava("方法名字111", new OnJsCallJavaChangeListener() {

            /**
             * @param data js回传数据
             * @param functionName js回传java端方法名
             * @param androidCallJsListener java回传js数据, eg: listener.onJavaCallJs("Java原生响应: " + data);
             */
            @Override
            public void onJsCallJava(String data, String functionName, OnJavaCallJsChangeListener listener) {
                // step1
                Toast.makeText(getApplicationContext(), "Java原生方法: " + data, Toast.LENGTH_SHORT).show();
                // step2, 响应js结果
                listener.onJavaCallJs("Java原生响应: " + data);
            }
        });

step2:js
bridge.jsCallJava('方法名字111', {'name': 'js => JsCallJava'}, function(response) {

			    <!-- Js打印方法 -->
			    log(response)
			})

```
