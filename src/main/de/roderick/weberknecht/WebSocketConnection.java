/*
 *  Copyright (C) 2010 Roderick Baier
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package de.roderick.weberknecht;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;


public class WebSocketConnection
		implements WebSocket
{
	private URI url = null;
	private WebSocketEventHandler eventHandler = null;
	
	private boolean connected = false;
	
	private Socket socket = null;
	private InputStream input = null;
	private PrintStream output = null;
	
	private WebSocketReceiver receiver = null;
	private WebSocketHandshake handshake = null;
	
	
	public WebSocketConnection(URI url)
			throws WebSocketException
	{
		this(url, null);
	}
	

	public WebSocketConnection(URI url, String protocol)
			throws WebSocketException
	{
		this.url = url;
		handshake = new WebSocketHandshake(url, protocol);
	}
	

	public void setEventHandler(WebSocketEventHandler eventHandler)
	{
		this.eventHandler = eventHandler;
	}
	
	
	public WebSocketEventHandler getEventHandler()
	{
		return this.eventHandler;
	}
	

	public void connect()
			throws WebSocketException
	{
		try {
			socket = createSocket();
			input = socket.getInputStream();
			output = new PrintStream(socket.getOutputStream());
			
			System.out.println(handshake.getHandshake());
			output.write(handshake.getHandshake().getBytes());
						
			boolean handshakeComplete = false;
			boolean header = true;
			int len = 500;
			byte[] buffer = new byte[len];
			int pos = 0;
			ArrayList<String> handshakeLines = new ArrayList<String>();
			
			byte[] serverResponse = new byte[16];
			
			while (!handshakeComplete) {
				int b = input.read();
				buffer[pos] = (byte) b;
				pos += 1;
				
				if (!header) {
					serverResponse[pos-1] = (byte)b;
					if (pos == 16) {
						handshakeComplete = true;
					}
				}
				else if (buffer[pos-1] == 0x0A && buffer[pos-2] == 0x0D) {
					String line = new String(buffer, Charset.forName("UTF-8"));
					System.out.println(line);
					if (line.trim().equals("")) {
						header = false;
					}
					else {
						handshakeLines.add(line.trim());
					}
					
					buffer = new byte[len];
					pos = 0;
				}
			}
			
			// FIXME in draft-ietf-hybi-thewebsocketprotocol-00 wird "WebSocket" zusammen geschrieben
			if (!handshakeLines.get(0).equals("HTTP/1.1 101 Web Socket Protocol Handshake")) {
				throw new WebSocketException("unable to connect to server");
			}
			
			handshakeLines.remove(0);
			
			HashMap<String, String> headers = new HashMap<String, String>();
			for (String line : handshakeLines) {
				String[] keyValue = line.split(": ", 2);
				headers.put(keyValue[0], keyValue[1]);
			}
			
			// TODO check header
			
			receiver = new WebSocketReceiver(input, this);
			receiver.start();
			connected = true;
			eventHandler.onOpen();
		}
		catch (IOException e) {
			throw new WebSocketException("connect failed - " + e.getMessage());
		}
	}
	

	public void send(String data)
			throws WebSocketException
	{
		if (!connected) {
			throw new WebSocketException("not connected");
		}
		
		try {
			output.write(0x00);
			output.write(data.getBytes(("UTF-8")));
			output.write(0xff);
			output.write("\r\n".getBytes());
		}
		catch (UnsupportedEncodingException uee) {
			throw new WebSocketException("unable to send data: unsupported encoding " + uee);
		}
		catch (IOException ioe) {
			throw new WebSocketException("unable to send data: " + ioe);
		}
	}
	

	public void send(byte[] data)
			throws WebSocketException
	{
		if (!connected) {
			throw new WebSocketException("not connected");
		}
		
		try {
			output.write(0x80);
			output.write(data.length);
			output.write(data);
			output.write("\r\n".getBytes());
		}
		catch (IOException ioe) {
			throw new WebSocketException("unable to send data: " + ioe);
		}
	}
	
	
	public void handleReceiverError()
	{
		try {
			if (connected) {
				close();
			}
		}
		catch (WebSocketException wse) {
			wse.printStackTrace();
		}
	}
	

	public void close()
		throws WebSocketException
	{
		if (!connected) {
			throw new WebSocketException("not connected");
		}
		
		sendCloseHandshake();
				
		if (receiver.isRunning()) {
			receiver.stopit();
		}
		
		try {
			input.close();
			output.close();
			socket.close();
		}
		catch (IOException ioe) {
			throw new WebSocketException("error while closing websocket connection: " + ioe);
		}

		eventHandler.onClose();
	}
	
	
	private void sendCloseHandshake()
		throws WebSocketException
	{
		if (!connected) {
			throw new WebSocketException("not connected");
		}
		
		try {
			output.write(0xff00);
			output.write("\r\n".getBytes());
		}
		catch (IOException ioe) {
			throw new WebSocketException("unable to send close handshake: " + ioe);
		}

		connected = false;
	}
	

	private Socket createSocket()
			throws WebSocketException
	{
		String scheme = url.getScheme();
		String host = url.getHost();
		int port = url.getPort();
		
		Socket socket = null;
		
		if (scheme != null && scheme.equals("ws")) {
			if (port == -1) {
				port = 80;
			}
			try {
				socket = new Socket(host, port);
			}
			catch (UnknownHostException uhe) {
				throw new WebSocketException("unknown host: " + host);
			}
			catch (IOException ioe) {
				throw new WebSocketException("unable to create socket to " + url);
			}
		}
		else if (scheme != null && scheme.equals("wss")) {
			if (port == -1) {
				port = 443;
			}
			try {
				SocketFactory factory = SSLSocketFactory.getDefault();
				socket = factory.createSocket(host, port);
			}
			catch (UnknownHostException uhe) {
				throw new WebSocketException("unknown host: " + host);
			}
			catch (IOException ioe) {
				throw new WebSocketException("unable to create secure socket to " + url);
			}
		}
		else {
			throw new WebSocketException("unsupported protocol: " + scheme);
		}
		
		return socket;
	}
}
