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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.mindrot.BCrypt;

import dcraft.lang.chars.Utf8Decoder;
import dcraft.lang.chars.Utf8Encoder;
import dcraft.log.Logger;
import dcraft.xml.XElement;

/*
 * This difference over BSO is that we use an IV (stored with value) and use padding 
 */
public class StandardSettingsObfuscator extends BasicSettingsObfuscator {
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
		
		// standardize at 64 bytes
		if (bkey1.length > 64) {
			byte[] b1 = new byte[64];
			ArrayUtil.blockCopy(bkey1, bkey1.length - 64, b1, 0, 64);
			bkey1 = b1;
		}
		else if (bkey1.length < 64) {
			byte[] b1 = new byte[64];
			ArrayUtil.blockCopy(bkey1, 0, b1, 64 - bkey1.length, bkey1.length);
			ArrayUtil.blockCopy(DEFAULT_SALT, 0, b1, 0, 64 - bkey1.length);
			bkey1 = b1;
		}
			
		ArrayUtil.blockCopy(bkey1, 0, skey, 64, 64);
		
		if (StringUtil.isEmpty(key2)) 
			key2 = "576f726c64";
		else if (key2.length() > 128)
			key2 = key2.substring(key2.length() - 128);
		
		byte[] bkey2 = HexUtil.decodeHex(key2);
		
		if (bkey2 == null)
			bkey2 = DEFAULT_SALT;
		
		// standardize at 64 bytes
		if (bkey2.length > 64) {
			byte[] b2 = new byte[64];
			ArrayUtil.blockCopy(bkey2, bkey1.length - 64, b2, 0, 64);
			bkey2 = b2;
		}
		else if (bkey2.length < 64) {
			byte[] b2 = new byte[64];
			ArrayUtil.blockCopy(bkey2, 0, b2, 64 - bkey2.length, bkey2.length);
			ArrayUtil.blockCopy(DEFAULT_SALT, 0, b2, 0, 64 - bkey2.length);
			bkey2 = b2;
		}
		
		ArrayUtil.blockCopy(bkey2, 0, skey, 0, 64);
		
		this.masterkey = skey;
		
		byte[] akey = new byte[32];
		ArrayUtil.blockCopy(skey, 72, akey, 0, 32);
		
		this.aeskey = new SecretKeySpec(akey, "AES");
		this.hmackey = new SecretKeySpec(skey, "hmacSHA512");
	}

	@Override
	public String decryptString(byte[] v) {
		if (v == null)
			return null;
		
		try {
            IvParameterSpec ivspec = new IvParameterSpec(v, 0, 16);		//load 16 byte IV 
			
			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
			c.init(Cipher.DECRYPT_MODE, this.aeskey, ivspec);

			System.out.println("a1: " + c.getAlgorithm());
			System.out.println("a2: " + c.getBlockSize());
			
			byte[] encrypted = new byte[v.length - 16];
			
			ArrayUtil.blockCopy(v, 16, encrypted, 0, encrypted.length);
			
			//System.out.println("");
			
			return Utf8Decoder.decode(c.doFinal(encrypted)).toString();			
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
	public byte[] encryptString(CharSequence v) {
		if (StringUtil.isEmpty(v))
			return null;
		
		try {
            byte iv[] = new byte[16];		//generate random 16 byte IV 
            KeyUtil.random.nextBytes(iv);
            IvParameterSpec ivspec = new IvParameterSpec(iv);			
			
			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
			c.init(Cipher.ENCRYPT_MODE, this.aeskey, ivspec);
			
			byte[] encrypted = c.doFinal(Utf8Encoder.encode(v));
			
			byte[] res = new byte[encrypted.length + 16];
			
			ArrayUtil.blockCopy(iv, 0, res, 0, 16);
			ArrayUtil.blockCopy(encrypted, 0, res, 16, encrypted.length);
			
			return res;
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
	public String hashPassword(CharSequence v) {
		return BCrypt.hashpw(v.toString(), BCrypt.gensalt());
	}
	
	@Override
	public boolean checkHexPassword(CharSequence candidate, CharSequence hashed) {
		if (StringUtil.isEmpty(candidate))
			return StringUtil.isEmpty(hashed);
		
		try {
			return BCrypt.checkpw(candidate.toString(), hashed.toString());
		}
		catch (Exception x) {
		}
		
		return false;
	}
}
