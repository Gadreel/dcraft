/* ************************************************************************
#
#  designCraft.io
#
#  http://designcraft.io/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package dcraft.util;

import java.security.InvalidKeyException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import dcraft.lang.chars.Utf8Decoder;
import dcraft.lang.chars.Utf8Encoder;
import dcraft.log.Logger;
import dcraft.xml.XElement;

/**
 * This is the default settings obfuscator, see ISettingsObfuscator for hints on 
 * how to build your own.  If a customer obfuscator is not provided then all
 * encrypted configuration settings use this implementation.
 * 
 *  This is good enough to keep the casual hacker at bay.  The encryption key is
 *  created by a combination of settings in config.xml and a hard coded default salt.
 *  So to break an encryption the hacker needs both the code and the config file.
 * 
 * @author Andy
 *
 */
public class BasicSettingsObfuscator implements ISettingsObfuscator {
	public static final byte[] DEFAULT_SALT = {
		(byte)201, (byte) 15, (byte)218, (byte)162, (byte) 33, (byte)104, (byte)194, (byte) 52,
		(byte)196, (byte)198, (byte) 98, (byte)139, (byte)128, (byte)220, (byte) 28, (byte)209,
		(byte) 41, (byte)  2, (byte) 78, (byte)  8, (byte)138, (byte)103, (byte)204, (byte)116,
		(byte)  2, (byte) 11, (byte)190, (byte)166, (byte) 59, (byte) 19, (byte)155, (byte) 34,
		(byte) 81, (byte) 74, (byte)  8, (byte)121, (byte)142, (byte) 52, (byte)  4, (byte)221,
		(byte)239, (byte)149, (byte) 25, (byte)179, (byte)205, (byte) 58, (byte) 67, (byte) 27,
		(byte) 48, (byte) 43, (byte) 10, (byte)109, (byte)242, (byte) 95, (byte) 20, (byte) 55,
		(byte) 79, (byte)225, (byte) 53, (byte)109, (byte)109, (byte) 81, (byte)194, (byte) 69
	};
	
	protected byte[] masterkey = null;
	protected SecretKeySpec aeskey = null;
	protected SecretKeySpec hmackey = null;
	
	@Override
	public void init(XElement config) {
		String key1 = null;
		String key2 = null;
		
		if (config != null) {
			key1 = config.getAttribute("Id");
			key2 = config.getAttribute("Feed");
		}
		
		byte[] skey = new byte[128];
		
		if (StringUtil.isEmpty(key1)) 
			key1 = "48656c6c6f";
		else if (key1.length() > 128)
			key1 = key1.substring(key1.length() - 128);
	
		byte[] bkey1 = HexUtil.decodeHex(key1);
		
		if (bkey1 == null)
			bkey1 = DEFAULT_SALT;
		
		ArrayUtil.blockCopy(bkey1, 0, skey, 128 - bkey1.length, bkey1.length);
		
		if (bkey1.length < 64) 
			ArrayUtil.blockCopy(DEFAULT_SALT, 0, skey, 64, 64 - bkey1.length);
		
		if (StringUtil.isEmpty(key2)) 
			key2 = "576f726c64";
		else if (key2.length() > 128)
			key2 = key2.substring(key2.length() - 128);
		
		byte[] bkey2 = HexUtil.decodeHex(key2);
		
		if (bkey2 == null)
			bkey2 = DEFAULT_SALT;
		
		ArrayUtil.blockCopy(bkey2, 0, skey, 0, bkey2.length);
		
		if (bkey2.length < 64) 
			ArrayUtil.blockCopy(DEFAULT_SALT, bkey2.length, skey, bkey2.length, 64 - bkey2.length);
		
		this.masterkey = skey;
		
		byte[] akey = new byte[16];
		ArrayUtil.blockCopy(skey, bkey2.length - 10, akey, 0, 16);
		
		this.aeskey = new SecretKeySpec(akey, "AES");
		this.hmackey = new SecretKeySpec(skey, "hmacSHA512");
	}

	@Override
	public void configure(XElement config) {
		byte[] idbuff = new byte[64];
		byte[] feedbuff = new byte[64];
		
		SecureRandom rnd = new SecureRandom();
		rnd.nextBytes(idbuff);
		rnd.nextBytes(feedbuff);
		
		config.setAttribute("Id", HexUtil.bufferToHex(idbuff));
		config.setAttribute("Feed", HexUtil.bufferToHex(feedbuff));
	}

	@Override
	public String decryptHexToString(CharSequence v) {
		return this.decryptString(HexUtil.decodeHex(v));
	}

	@Override
	public String decryptString(byte[] v) {
		if (v == null)
			return null;
		
		try {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.DECRYPT_MODE, this.aeskey);

			//System.out.println("a1: " + c.getAlgorithm());
			//System.out.println("a2: " + c.getBlockSize());
			
			return Utf8Decoder.decode(c.doFinal(v)).toString();			
		}
		catch(InvalidKeyException x) {
			Logger.warn("Invalid settings key: " + x, "Code", "202");
		}
		catch(Exception x) {
			Logger.info("Failed decryption: " + x, "Code", "203");
		}
		
		return null;
	}

	@Override
	public String encryptStringToHex(CharSequence v) {
    	return HexUtil.bufferToHex(this.encryptString(v));
	}

	@Override
	public byte[] encryptString(CharSequence v) {
		if (StringUtil.isEmpty(v))
			return null;
		
		try {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, this.aeskey);
			return c.doFinal(Utf8Encoder.encode(v));				
		}
		catch(InvalidKeyException x) {
			Logger.warn("Invalid settings key: " + x, "Code", "204");
		}
		catch(Exception x) {
			Logger.info("Failed decryption: " + x, "Code", "205");
		}
		
		return null;
	}

	@Override
	public String hashStringToHex(CharSequence v) {
    	return HexUtil.bufferToHex(this.hashString(v));
	}

	@Override
	public byte[] hashString(CharSequence v) {
		if (StringUtil.isEmpty(v))
			return null;
		
		try {
			Mac mac = Mac.getInstance("hmacSHA512");
			mac.init(this.hmackey);			
			return mac.doFinal(Utf8Encoder.encode(v));
		} 
		catch (Exception x) {
			Logger.info("Failed hash: " + x, "Code", "206");
		}	
		
		return null;
	}
	
	@Override
	public String hashPassword(CharSequence v) {
		return this.hashStringToHex(v);
	}
	
	@Override
	public boolean checkHexPassword(CharSequence candidate, CharSequence hashed) {
		if (StringUtil.isEmpty(candidate))
			return StringUtil.isEmpty(hashed);
		
		return this.hashStringToHex(candidate).equals(hashed);
	}
}
