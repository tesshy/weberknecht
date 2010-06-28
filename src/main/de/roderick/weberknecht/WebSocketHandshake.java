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

import java.net.URI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class WebSocketHandshake
{	
	private String key1 = null;
	private String key2 = null;
	private String key3 = null;
	private String expectedServerResponse = null;
	
	private URI url = null;
	private String protocol = null;
	
	
	public WebSocketHandshake(URI url, String protocol)
	{
		this.url = url;
		this.protocol = null;
		generateKeys();
	}
	
	
	public String getHandshake()
	{
		String path = url.getPath();
		String host = url.getHost();
		String origin = "http://" + host;
		
		String handshake = "GET " + path + " HTTP/1.1\r\n" +
				"Host: " + host + "\r\n" +
				"Connection: Upgrade\r\n" +
				"Sec-WebSocket-Key2: " + key2 + "\r\n";
		
		if (protocol != null) {
			handshake += "Sec-WebSocket-Protocol: " + protocol + "\r\n";
		}
		
		handshake += "Upgrade: WebSocket\r\n" +
				"Sec-WebSocket-Key1: " + key1 + "\r\n" +
				"Sec-WebSocket-Origin: " + origin	+ "\r\n" +
				"\r\n" +
				key3;
		
		return handshake;
	}
	
	
	public boolean verifyServerResponse(String response)
	{
		return response.equals(expectedServerResponse);
	}
	
	
	public String getExpectedResponse()
	{
		return expectedServerResponse;
	}
	
	
	private void generateKeys()
	{
		int spaces1 = rand(1,12);
		int spaces2 = rand(1,12);
		
		int max1 = Integer.MAX_VALUE / spaces1;
		int max2 = Integer.MAX_VALUE / spaces2;
		
		int number1 = rand(0, max1);
		int number2 = rand(0, max2);
		
		int product1 = number1 * spaces1;
		int product2 = number2 * spaces2;
		
		key1 = Integer.toString(product1);
		key2 = Integer.toString(product2);
		
		key1 = insertRandomCharacters(key1);
		key2 = insertRandomCharacters(key2);
		
		key1 = insertSpaces(key1, spaces1);
		key2 = insertSpaces(key2, spaces2);
		
		key3 = createRandomBytes();
		
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(number1);
		byte[] number1Array = buffer.array();
		buffer = ByteBuffer.allocate(4);
		buffer.putInt(number2);
		byte[] number2Array = buffer.array();
		
		byte[] challenge = new byte[16];				
		System.arraycopy(number1Array, 0, challenge, 0, 4);
		System.arraycopy(number2Array, 0, challenge, 4, 4);
		System.arraycopy(key3.getBytes(), 0, challenge, 8, 8);

		expectedServerResponse = new String(md5(challenge));
	}
	
	
	private String insertRandomCharacters(String key)
	{
		int count = rand(1, 12);
		
		char[] randomChars = new char[count];
		int randCount = 0;
		while (randCount < count) {
			int rand = (int) (Math.random() * 0x7e + 0x21);
			if (((0x21 < rand) && (rand < 0x2f)) || ((0x3a < rand) && (rand < 0x7e))) {
				randomChars[randCount] = (char) rand;
				randCount += 1;
			}
		}
		
		for (int i = 0; i < count; i++) {
			int split = rand(0, key.length());
			String part1 = key.substring(0, split);
			String part2 = key.substring(split);
			key = part1 + randomChars[i] + part2;
		}
		
		return key;
	}
	
	
	private String insertSpaces(String key, int spaces)
	{
		for (int i = 0; i < spaces; i++) {
			int split = rand(0, key.length());
			String part1 = key.substring(0, split);
			String part2 = key.substring(split);
			key = part1 + " " + part2;
		}
		
		return key;
	}
	
	
	private String createRandomBytes()
	{
		int rand = rand(10000000, 99999999);
		return String.valueOf(rand);
	}
	
	
	private byte[] md5(byte[] text)
	{
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			return md.digest(text);
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	private int rand(int min, int max)
	{
		int rand = (int) (Math.random() * max + min);
		return rand;
	}
}
