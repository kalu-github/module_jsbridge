(function() {
	if (window.WebViewJavascriptBridge) {
		return;
	}

	if (!window.onerror) {
		window.onerror = function(msg, url, line) {
			console.log("WebViewJavascriptBridge: ERROR:" + msg + "@" + url + ":" + line);
		}
	}
	window.WebViewJavascriptBridge = {
		javaCallJs: javaCallJs,
		jsCallJava: jsCallJava,
		disableJavscriptAlertBoxSafetyTimeout: disableJavscriptAlertBoxSafetyTimeout,
		_h5FetchQueue: _h5FetchQueue,
		_handleMessageFromObjC: _handleMessageFromObjC
	};

	var messagingIframe;
	var sendMessageQueue = [];
	var messageHandlers = {};

	var CUSTOM_PROTOCOL_SCHEME = 'bridgescheme';
	var QUEUE_HAS_MESSAGE = 'h5message';

	var responseCallbacks = {};
	var uniqueId = 1;
	var dispatchMessagesWithTimeoutSafety = true;

	function javaCallJs(handlerName, handler) {
		messageHandlers[handlerName] = handler;
	}

	function jsCallJava(handlerName, data, responseCallback) {
		if (arguments.length == 2 && typeof data == 'function') {
			responseCallback = data;
			data = null;
		}
		_doSend({ handlerName:handlerName, data:data }, responseCallback);
	}
	function disableJavscriptAlertBoxSafetyTimeout() {
		dispatchMessagesWithTimeoutSafety = false;
	}

	function _doSend(message, responseCallback) {
		if (responseCallback) {
			var callbackId = 'cb_'+(uniqueId++)+'_'+new Date().getTime();
			responseCallbacks[callbackId] = responseCallback;
			message['callbackId'] = callbackId;
		}
		sendMessageQueue.push(message);
		messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE;
	}

	function _h5FetchQueue() {
		var messageQueueString = JSON.stringify(sendMessageQueue);
		sendMessageQueue = [];
//		return messageQueueString; // Android无法直接返回数据, 这是与iOS最大的区别; 所以, 需要使用自定义url形式返回数据。
        messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://h5return/_h5FetchQueue/' + encodeURIComponent(messageQueueString);
	}

	function _dispatchMessageFromObjC(messageJSON) {
		if (dispatchMessagesWithTimeoutSafety) {
			setTimeout(_doDispatchMessageFromObjC);
		} else {
			 _doDispatchMessageFromObjC();
		}

		function _doDispatchMessageFromObjC() {
			var message = JSON.parse(messageJSON);
			var messageHandler;
			var responseCallback;
			if (message.responseId) {
				responseCallback = responseCallbacks[message.responseId];
				if (!responseCallback) {
					return;
				}
				responseCallback(message.responseData);
				delete responseCallbacks[message.responseId];
			} else {
				if (message.callbackId) {
					var callbackResponseId = message.callbackId;
					responseCallback = function(responseData) {
						_doSend({ handlerName:message.handlerName, responseId:callbackResponseId, responseData:responseData });
					};
				}

				var handler = messageHandlers[message.handlerName];
				if (!handler) {
					console.log("WebViewJavascriptBridge: WARNING: no handler for message from ObjC:", message);
				} else {
					handler(message.data, responseCallback);
				}
			}
		}
	}

	function _handleMessageFromObjC(messageJSON) {
        _dispatchMessageFromObjC(messageJSON);
	}

	messagingIframe = document.createElement('iframe');
	messagingIframe.style.display = 'none';
	messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE;
	document.documentElement.appendChild(messagingIframe);

	javaCallJs("_disableJavascriptAlertBoxSafetyTimeout", disableJavscriptAlertBoxSafetyTimeout);

	setTimeout(_callWVJBCallbacks, 0);
	function _callWVJBCallbacks() {
		var callbacks = window.WVJBCallbacks;
		delete window.WVJBCallbacks;
		for (var i=0; i<callbacks.length; i++) {
			callbacks[i](WebViewJavascriptBridge);
		}
	}
})();