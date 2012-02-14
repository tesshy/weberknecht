weberknecht - Java WebSocket Client Library
===========================================

Weberknecht is a Java implementation of the client side of the IETF WebSocket Protocol Draft
draft-ietf-hybi-thewebsocketprotocol-00 (May 23, 2010) for use in Java SE or Android applications.

##Note
This fork is changed by tesshy.

Goal is satisfy with RFC 6455, like WebSocket Version 13.

Now, I tested Handshake, Sending Text and Binary.
Handshake is not full implementation, because not checking accept header.


Usage
-----
This short code snippet shows how to integrate weberknecht in your application:

	try {
			URI url = new URI("ws://127.0.0.1:8080/test");
			WebSocket websocket = new WebSocketConnection(url);
			
			// Register Event Handlers
			websocket.setEventHandler(new WebSocketEventHandler() {
					public void onOpen()
					{
							System.out.println("--open");
					}
									
					public void onMessage(WebSocketMessage message)
					{
							System.out.println("--received message: " + message.getText());
					}
									
					public void onClose()
					{
							System.out.println("--close");
					}
			});
			
			// Establish WebSocket Connection
			websocket.connect();
			
			// Send UTF-8 Text
			websocket.send("hello world");
			
			// Close WebSocket Connection
			websocket.close();
	}
	catch (WebSocketException wse) {
			wse.printStackTrace();
	}
	catch (URISyntaxException use) {
			use.printStackTrace();
	}