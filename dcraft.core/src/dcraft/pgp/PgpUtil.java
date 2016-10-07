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
package dcraft.pgp;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.bcpg.PacketTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;

import dcraft.db.util.ByteUtil;
import dcraft.lang.Memory;
import dcraft.lang.chars.Utf8Decoder;
import dcraft.util.HexUtil;

public class PgpUtil {
    
    public static String getSymmetricCipherName(int algorithm) {
        switch (algorithm) {
        case SymmetricKeyAlgorithmTags.NULL:
            return null;
        case SymmetricKeyAlgorithmTags.TRIPLE_DES:
            return "DESEDE";
        case SymmetricKeyAlgorithmTags.IDEA:
            return "IDEA";
        case SymmetricKeyAlgorithmTags.CAST5:
            return "CAST5";
        case SymmetricKeyAlgorithmTags.BLOWFISH:
            return "Blowfish";
        case SymmetricKeyAlgorithmTags.SAFER:
            return "SAFER";
        case SymmetricKeyAlgorithmTags.DES:
            return "DES";
        case SymmetricKeyAlgorithmTags.AES_128:
            return "AES";
        case SymmetricKeyAlgorithmTags.AES_192:
            return "AES";
        case SymmetricKeyAlgorithmTags.AES_256:
            return "AES";
        case SymmetricKeyAlgorithmTags.CAMELLIA_128:
            return "Camellia";
        case SymmetricKeyAlgorithmTags.CAMELLIA_192:
            return "Camellia";
        case SymmetricKeyAlgorithmTags.CAMELLIA_256:
            return "Camellia";
        case SymmetricKeyAlgorithmTags.TWOFISH:
            return "Twofish";
        default:
            throw new IllegalArgumentException("unknown symmetric algorithm: " + algorithm);
        }
    }
    
    static public SecretKey makeSymmetricKey(int algorithm, byte[] keyBytes) throws PGPException {
        String algName = getSymmetricCipherName(algorithm);

        if (algName == null)
            throw new PGPException("unknown symmetric algorithm: " + algorithm);

        return new SecretKeySpec(keyBytes, algName);
    }
    
    static public void encrypt(InputStream in, String name, OutputStream out, PGPPublicKey key) throws Exception {
        try {    
        	EncryptedFileStream pw = new EncryptedFileStream(); 
        	
        	pw.setFileName(name);
        	pw.addPublicKey(key);
        	pw.init();
        	
            byte[] ibuf = new byte[31 * 1024];

            int len;
            
            while ((len = in.read(ibuf)) > 0) {
                pw.writeData(ibuf, 0, len);
                
                ByteBuf buf = pw.nextReadyBuffer();
                
                while (buf != null) {
                	out.write(buf.array(), buf.arrayOffset(), buf.readableBytes()); 
                
                	buf.release();
                	
                	buf = pw.nextReadyBuffer();
                }
            }
            
            // TODO protect close
            in.close();
            pw.close();
            
            ByteBuf buf = pw.nextReadyBuffer();
            
            while (buf != null) {
            	out.write(buf.array(), buf.arrayOffset(), buf.readableBytes()); 
            
            	buf.release();
            	
            	buf = pw.nextReadyBuffer();
            }
            
            // TODO protect close
            out.close();
        }
        catch (Exception e) {
            System.err.println(e);
            
            e.printStackTrace();
        }
    }
    
    static public void decryptFile(Memory mem, KeyRingCollection krc, MessageDigest digest) throws Exception {
		int hdr = mem.readByte();
		
        if ((hdr & 0x80) == 0)
        	throw new Exception("May not be binary");
		
        Cipher cipher = null;
        
        while (hdr >= 0) {
        	/*
            if (pbyte >= 0) {
				int maskB = pbyte & 0x3f;
	            
				if ((pbyte & 0x40) == 0)    // old
					maskB >>= 2;
				
				pbyte = maskB;
            }
            */
            
            boolean    newPacket = (hdr & 0x40) != 0;
            int        tag = 0;
            int        bodyLen = 0;
            boolean    partial = false;

            if (newPacket)
            {
                tag = hdr & 0x3f;

                int    l = mem.readByte();

                if (l == 0) {
                	partial = true;
                	// TODO len??
                }
                else if (l < 192)
                {
                    bodyLen = l;
                }
                else if (l <= 223)
                {
                    int b = mem.readByte();

                    bodyLen = ((l - 192) << 8) + (b) + 192;
                }
                else if (l == 255)
                {
                    bodyLen = (mem.readByte() << 24) | (mem.readByte() << 16) |  (mem.readByte() << 8)  | mem.readByte();
                }
                else
                {
                    partial = true;
                    bodyLen = 1 << (l & 0x1f);
                }
            }
            else
            {
                int lengthType = hdr & 0x3;

                tag = (hdr & 0x3f) >> 2;

                switch (lengthType)
                {
                case 0:
                    bodyLen = mem.readByte();
                    break;
                case 1:
                    bodyLen = (mem.readByte() << 8) | mem.readByte();
                    break;
                case 2:
                    bodyLen = (mem.readByte() << 24) | (mem.readByte() << 16) | (mem.readByte() << 8) | mem.readByte();
                    break;
                case 3:
                    partial = true;
                    break;
                default:
                    throw new IOException("unknown length type encountered");
                }
            }            
            
            switch (tag) {
                /*
            case PacketTags.SIGNATURE:
                l = new ArrayList();

                while (in.nextPacketTag() == PacketTags.SIGNATURE)
                {
                    try
                    {
                        l.add(new PGPSignature(in));
                    }
                    catch (PGPException e)
                    {
                        throw new IOException("can't create signature object: " + e);
                    }
                }

                return new PGPSignatureList((PGPSignature[])l.toArray(new PGPSignature[l.size()]));
            case PacketTags.COMPRESSED_DATA:
                return new PGPCompressedData(in);
                */
            case PacketTags.MOD_DETECTION_CODE:
            	//int ml = mem.readByte();
            	
            	if (bodyLen != 20) {
            		System.out.println("mod detect bad!");
            		return;
            	}
            	
            	byte[] dgres = digest.digest();
            	byte[] bl = new byte[20];
            	
            	mem.read(bl, 0, bl.length);
            	
            	System.out.println("calc mod: " + HexUtil.bufferToHex(dgres));
            	
            	System.out.println("found mod: " + HexUtil.bufferToHex(bl));
            	
            	if (ByteUtil.compareKeys(dgres, bl) != 0)
            		System.out.println("keys don't match!");
            	
            	break;
            	
            case PacketTags.LITERAL_DATA:
                //return new PGPLiteralData(in);
            	System.out.println("data: " + bodyLen + " with partial: " + partial);
            	
            	int offset = 6;
            	
            	int ftype = mem.readByte();
            	
            	// we only process binary type
            	if (ftype != 98) {
            		System.out.println("wrong data type!");
            		return;
            	}
            	
            	int fnlen = mem.readByte();
            	
            	offset += fnlen;
            	
            	byte[] fnbuf = new byte[fnlen];
            	mem.read(fnbuf, 0, fnbuf.length);
            	
            	System.out.println("got filename: " + Utf8Decoder.decode(fnbuf));
            	
            	
            	byte[] fmbuf = new byte[4];
            	mem.read(fmbuf, 0, fmbuf.length);
            	
            	System.out.println("got mod time: " + HexUtil.bufferToHex(fmbuf));
            	
            	byte[] buf = new byte[bodyLen];
            	mem.read(buf, 0, buf.length - offset);
            	
            	System.out.println("got content: " + Utf8Decoder.decode(buf));
            	
            	if (partial) {
            		boolean morepartial = true;
            		
            		while (morepartial) {
            			morepartial = false;
            			int l = mem.readByte();
            			
                        if (l < 192)
                        {
                            bodyLen = l;
                        }
                        else if (l <= 223)
                        {
                            int b = mem.readByte();

                            bodyLen = ((l - 192) << 8) + (b) + 192;
                        }
                        else if (l == 255)
                        {
                            bodyLen = (mem.readByte() << 24) | (mem.readByte() << 16) |  (mem.readByte() << 8)  | mem.readByte();
                        }
                        else
                        {
                            bodyLen = 1 << (l & 0x1f);
                            morepartial = true;
                        }
                        
                        buf = new byte[bodyLen];
                    	mem.read(buf, 0, buf.length);
                    	
                    	System.out.println("got partial content: " + Utf8Decoder.decode(buf));
            		}
            	}
            	
            	break;
            case PacketTags.PUBLIC_KEY_ENC_SESSION:
            	System.out.println("encrypt session: " + bodyLen);
            	
            	byte[] buf2 = new byte[bodyLen];
            	mem.read(buf2, 0, bodyLen);

            	Memory essessmem = new Memory(buf2);
            	essessmem.setPosition(0);
            	
            	PublicKeyEncryptedSession esession = new PublicKeyEncryptedSession();
            	esession.loadFrom(essessmem);
            	
            	System.out.println(" - ver: " + esession.getVersion() + " alg: " +  esession.getAlgorithm()
            		+ " key id: " + esession.getKeyID());

            	PGPPrivateKey skey = krc.findSecretKey(esession.getKeyID(), "a1s2d3".toCharArray());		// TODO don't hardcode
            	
            	System.out.println("secret private key: " + skey.getKeyID());

            	cipher = esession.getDataCipher(skey, true);
            	
            	// TODO we also need the integrity checker 

            	break;
            	
            case PacketTags.SYM_ENC_INTEGRITY_PRO:
        		int prover = mem.readByte();
            	
            	System.out.println("protected data ver: " + prover + " len: " + bodyLen + " with partial: " + partial);
            	
            	/*
            	byte[] buf3 = new byte[bodyLen - 1];
            	mem.read(buf3, 0, bodyLen - 1);
            	*/
            	
            	// rest of file is encrypted, just grab it all
            	
            	byte[] buf3 = new byte[mem.getLength() - mem.getPosition()];
            	mem.read(buf3, 0, buf3.length);
            	
            	System.out.println("crypt data: " + HexUtil.bufferToHex(buf3));
            	
            	byte[] plaindata = cipher.doFinal(buf3);
            	
            	System.out.println("plain data: " + HexUtil.bufferToHex(plaindata));
            	
            	// this is a cheat of course
            	MessageDigest md = MessageDigest.getInstance("SHA1");
            	
            	md.update(plaindata, 0, plaindata.length - 20);
            	
            	System.out.println("md bytes: " + (plaindata.length - 20));
            	
            	Memory dmem = new Memory(plaindata);
            	dmem.setPosition(0);
            	
            	// check packet is complete
            	
                byte[] iv = new byte[cipher.getBlockSize() + 2];
                
                dmem.read(iv, 0, iv.length);
                
                if ((iv[iv.length - 2] != iv[iv.length - 4]) && (iv[iv.length - 2] != 0))
                    System.out.println("data check 1 failed.");

                if ((iv[iv.length - 1] != iv[iv.length - 3]) && (iv[iv.length - 1] != 0))
                    System.out.println("data check 2 failed.");
            	
                PgpUtil.decryptFile(dmem, krc, md);
                
            	return;
            default:
            	throw new Exception("Unrecognized packet: " + tag);
            }            
        	
    		hdr = mem.readByte();
        }
    }
}
