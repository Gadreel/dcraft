package dcraft.pgp;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.bcpg.ECDHPublicBCPGKey;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyPacket;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jcajce.spec.UserKeyingMaterialSpec;
import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyValidationException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.operator.PGPPad;
import org.bouncycastle.openpgp.operator.RFC6637Utils;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyConverter;

import dcraft.lang.Memory;
import dcraft.util.ArrayUtil;
import dcraft.util.HexUtil;

/**
 * basic packet for a PGP public key
 */
public class PublicKeyEncryptedSession {
    protected int version = 0;
    protected long keyID = 0;
    protected int algorithm = 0;
    protected byte[][] data = null;

    public void loadFrom(Memory mem) throws Exception {     
    	System.out.println("enc sess full: " + HexUtil.bufferToHex(mem.toArray()));
    	
        this.version = mem.readByte();
        
        this.keyID |= (long)mem.readByte() << 56;
        this.keyID |= (long)mem.readByte() << 48;
        this.keyID |= (long)mem.readByte() << 40;
        this.keyID |= (long)mem.readByte() << 32;
        this.keyID |= (long)mem.readByte() << 24;
        this.keyID |= (long)mem.readByte() << 16;
        this.keyID |= (long)mem.readByte() << 8;
        this.keyID |= mem.readByte();
        
        this.algorithm = mem.readByte();
        
        switch (this.algorithm)
        {
        case PublicKeyAlgorithmTags.RSA_ENCRYPT:
        case PublicKeyAlgorithmTags.RSA_GENERAL:
        	this.data = new byte[1][];
            
        	this.data[0] = this.clipMP(mem);
            break;
        case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT:
        case PublicKeyAlgorithmTags.ELGAMAL_GENERAL:
        	this.data = new byte[2][];
            
        	this.data[0] = this.clipMP(mem);
        	this.data[1] = this.clipMP(mem);
            break;
        case PublicKeyAlgorithmTags.ECDH:
        	this.data = new byte[1][];

            byte[] d = new byte[mem.readableBytes()];
            
            mem.read(d, 0, d.length);
            
            this.data[0] = d;
            break;
        default:
            throw new Exception("unknown PGP public key algorithm encountered");
        }
    }
    
    public byte[] clipMP(Memory mem) {
    	int b1 = mem.readByte();
    	int b2 = mem.readByte();
    	
        int length = (b1 << 8) | b2;
        byte[] bytes = new byte[((length + 7) / 8) + 2];
        
        bytes[0] = (byte) b1;
        bytes[1] = (byte) b2;
        
        for (int i = 2; i < bytes.length; i++)
        	bytes[i] = (byte) mem.readByte();
        
    	System.out.println("enc sess decode mp: " + HexUtil.bufferToHex(bytes));
        
        return bytes;
    }
    
    // this is more utility method, maybe move to PGPUtil
    public BigInteger decodeMP(Memory mem) {
        int length = (mem.readByte() << 8) | mem.readByte();
        byte[] bytes = new byte[(length + 7) / 8];
        
        for (int i = 0; i < bytes.length; i++)
        	bytes[i] = (byte) mem.readByte();
        
    	System.out.println("enc sess decode mp: " + HexUtil.bufferToHex(bytes));
        
        return new BigInteger(1, bytes);
    }
            
    // this is more utility method, maybe move to PGPUtil
    public byte[] encodeMP(BigInteger value) {
        int length = value.bitLength();
        byte[] out = new byte[((length + 7) / 8) + 2];
        out[0] = (byte) (length >> 8);
        out[1] = (byte) length;
        
        byte[] bytes = value.toByteArray();
        
    	System.out.println("enc sess enc mp ba: " + HexUtil.bufferToHex(bytes));
        
        if (bytes[0] == 0)
        	ArrayUtil.blockCopy(bytes, 1, out, 2, bytes.length - 1);
        else
        	ArrayUtil.blockCopy(bytes, 0, out, 2, bytes.length);
        
    	System.out.println("enc sess enc mp: " + HexUtil.bufferToHex(out));
        
        return out;
    }
        
    /*
    public PublicKeyEncryptedSession(
        long           keyID,
        int            algorithm,
        byte[][]       data)
    {
        this.version = 3;
        this.keyID = keyID;
        this.algorithm = algorithm;
        this.data = new byte[data.length][];

        for (int i = 0; i != data.length; i++)
        {
            this.data[i] = Arrays.clone(data[i]);
        }
    }
    */
    
    public Cipher getDataCipher(PGPPrivateKey pk, boolean withIntegrityPacket) throws PGPException {
        byte[] sessionData = this.recoverSessionData(pk, this.getAlgorithm(), this.getEncSessionKey());

        if (!confirmCheckSum(sessionData))
            throw new PGPKeyValidationException("key checksum failed");

        if (sessionData[0] == SymmetricKeyAlgorithmTags.NULL)
        	return null;
        
        try {
            byte[] sessionKey = new byte[sessionData.length - 3];

            System.arraycopy(sessionData, 1, sessionKey, 0, sessionKey.length);

            Cipher dataDecryptor = this.createDataDecryptor(withIntegrityPacket, sessionData[0] & 0xff, sessionKey);

            /*
             * see org.bouncycastle.openpgp.PGPPublicKeyEncryptedData
             * 
            encStream = new BCPGInputStream(dataDecryptor.getInputStream(encData.getInputStream()));

            if (withIntegrityPacket)
            {
                truncStream = new TruncatedStream(encStream);

                integrityCalculator = dataDecryptor.getIntegrityCalculator();

                encStream = new TeeInputStream(truncStream, integrityCalculator.getOutputStream());
            }

            byte[] iv = new byte[dataDecryptor.getBlockSize()];

            for (int i = 0; i != iv.length; i++)
            {
                int    ch = encStream.read();

                if (ch < 0)
                {
                    throw new EOFException("unexpected end of stream.");
                }

                iv[i] = (byte)ch;
            }

            int    v1 = encStream.read();
            int    v2 = encStream.read();

            if (v1 < 0 || v2 < 0)
            {
                throw new EOFException("unexpected end of stream.");
            }
            */

            //
            // some versions of PGP appear to produce 0 for the extra
            // bytes rather than repeating the two previous bytes
            //
            /*
                         * Commented out in the light of the oracle attack.
                        if (iv[iv.length - 2] != (byte)v1 && v1 != 0)
                        {
                            throw new PGPDataValidationException("data check failed.");
                        }

                        if (iv[iv.length - 1] != (byte)v2 && v2 != 0)
                        {
                            throw new PGPDataValidationException("data check failed.");
                        }
                        */

            return dataDecryptor;
        }
        catch (PGPException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new PGPException("Exception starting decryption", e);
        }
    }

    // withIntegrityPacket = packet instanceof SymmetricEncIntegrityPacket;
    Cipher createDataDecryptor(boolean withIntegrityPacket, int encAlgorithm, byte[] key)
        throws PGPException
    {
        try
        {
            SecretKey secretKey = new SecretKeySpec(key, PgpUtil.getSymmetricCipherName(encAlgorithm));

            Cipher c = createStreamCipher(encAlgorithm, withIntegrityPacket);

            if (withIntegrityPacket)
            {
                byte[] iv = new byte[c.getBlockSize()];

                c.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            }
            else
            {
                c.init(Cipher.DECRYPT_MODE, secretKey);
            }

            /*
            return new PGPDataDecryptor()
            {
                public InputStream getInputStream(InputStream in)
                {
                    return new CipherInputStream(in, c);
                }

                public int getBlockSize()
                {
                    return c.getBlockSize();
                }

                public PGPDigestCalculator getIntegrityCalculator()
                {
                    return new SHA1PGPDigestCalculator();
                }
            };
            */
            
            return c;
        }
        catch (PGPException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new PGPException("Exception creating cipher", e);
        }
    }

    Cipher createStreamCipher(int encAlgorithm, boolean withIntegrityPacket)
        throws PGPException
    {
        String mode = (withIntegrityPacket)
            ? "CFB"
            : "OpenPGPCFB";

        String cName = PgpUtil.getSymmetricCipherName(encAlgorithm)
            + "/" + mode + "/NoPadding";

        return createCipher(cName);
    }

    Cipher createCipher(String cipherName)
        throws PGPException
    {
        try
        {
            return new NamedJcaJceHelper("BC").createCipher(cipherName);
        }
        catch (GeneralSecurityException e)
        {
            throw new PGPException("cannot create cipher: " + e.getMessage(), e);
        }
    }
    
    public byte[] recoverSessionData(final PrivateKey privKey, int keyAlgorithm, byte[][] secKeyData)
                 throws PGPException
     {
         if (keyAlgorithm == PublicKeyAlgorithmTags.ECDH)
             throw new PGPException("ECDH requires use of PGPPrivateKey for decryption");

         return decryptSessionData(keyAlgorithm, privKey, secKeyData);
     }

    public byte[] recoverSessionData(final PGPPrivateKey privKey, int keyAlgorithm, byte[][] secKeyData) throws PGPException
     {
    	JcaPGPKeyConverter keyConverter = new JcaPGPKeyConverter();
    	
         if (keyAlgorithm == PublicKeyAlgorithmTags.ECDH)
             return decryptSessionData(keyConverter, privKey, secKeyData);

         return decryptSessionData(keyAlgorithm, keyConverter.getPrivateKey(privKey), secKeyData);
     }
    
    private byte[] decryptSessionData(JcaPGPKeyConverter converter, PGPPrivateKey privKey, byte[][] secKeyData)
        throws PGPException
    {
        PublicKeyPacket pubKeyData = privKey.getPublicKeyPacket();
        ECDHPublicBCPGKey ecKey = (ECDHPublicBCPGKey)pubKeyData.getKey();
        X9ECParameters x9Params = NISTNamedCurves.getByOID(ecKey.getCurveOID());

        byte[] enc = secKeyData[0];

        int pLen = ((((enc[0] & 0xff) << 8) + (enc[1] & 0xff)) + 7) / 8;
        byte[] pEnc = new byte[pLen];

        System.arraycopy(enc, 2, pEnc, 0, pLen);

        byte[] keyEnc = new byte[enc[pLen + 2]];

        System.arraycopy(enc, 2 + pLen + 1, keyEnc, 0, keyEnc.length);

        ECPoint publicPoint = x9Params.getCurve().decodePoint(pEnc);

        try
        {
        	JcaKeyFingerprintCalculator fingerprintCalculator = new JcaKeyFingerprintCalculator();
        	
            byte[] userKeyingMaterial = RFC6637Utils.createUserKeyingMaterial(pubKeyData, fingerprintCalculator);

            KeyAgreement agreement = new NamedJcaJceHelper("BC").createKeyAgreement(RFC6637Utils.getAgreementAlgorithm(pubKeyData));

            PrivateKey privateKey = converter.getPrivateKey(privKey);

            agreement.init(privateKey, new UserKeyingMaterialSpec(userKeyingMaterial));

            agreement.doPhase(converter.getPublicKey(new PGPPublicKey(new PublicKeyPacket(PublicKeyAlgorithmTags.ECDH, new Date(),
                new ECDHPublicBCPGKey(ecKey.getCurveOID(), publicPoint, ecKey.getHashAlgorithm(), ecKey.getSymmetricKeyAlgorithm())), fingerprintCalculator)), true);

            Key key = agreement.generateSecret(RFC6637Utils.getKeyEncryptionOID(ecKey.getSymmetricKeyAlgorithm()).getId());

            Cipher c = this.createKeyWrapper(ecKey.getSymmetricKeyAlgorithm());

            c.init(Cipher.UNWRAP_MODE, key);

            Key paddedSessionKey = c.unwrap(keyEnc, "Session", Cipher.SECRET_KEY);

            return PGPPad.unpadSessionData(paddedSessionKey.getEncoded());
        }
        catch (InvalidKeyException e)
        {
            throw new PGPException("error setting asymmetric cipher", e);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new PGPException("error setting asymmetric cipher", e);
        }
        catch (InvalidAlgorithmParameterException e)
        {
            throw new PGPException("error setting asymmetric cipher", e);
        }
        catch (GeneralSecurityException e)
        {
            throw new PGPException("error setting asymmetric cipher", e);
        }
        catch (IOException e)
        {
            throw new PGPException("error setting asymmetric cipher", e);
        }
    }

    private byte[] decryptSessionData(int keyAlgorithm, PrivateKey privKey, byte[][] secKeyData)
        throws PGPException
    {
        Cipher c1 = this.createPublicKeyCipher(keyAlgorithm);

        try
        {
            c1.init(Cipher.DECRYPT_MODE, privKey);
        }
        catch (InvalidKeyException e)
        {
            throw new PGPException("error setting asymmetric cipher", e);
        }

        if (keyAlgorithm == PGPPublicKey.RSA_ENCRYPT
            || keyAlgorithm == PGPPublicKey.RSA_GENERAL)
        {
            byte[] bi = secKeyData[0];  // encoded MPI

            c1.update(bi, 2, bi.length - 2);
        }
        else
        {
            DHKey k = (DHKey)privKey;
            int size = (k.getParams().getP().bitLength() + 7) / 8;
            byte[] tmp = new byte[size];

            byte[] bi = secKeyData[0]; // encoded MPI
            if (bi.length - 2 > size)  // leading Zero? Shouldn't happen but...
            {
                c1.update(bi, 3, bi.length - 3);
            }
            else
            {
                System.arraycopy(bi, 2, tmp, tmp.length - (bi.length - 2), bi.length - 2);
                c1.update(tmp);
            }

            bi = secKeyData[1];  // encoded MPI
            for (int i = 0; i != tmp.length; i++)
            {
                tmp[i] = 0;
            }

            if (bi.length - 2 > size) // leading Zero? Shouldn't happen but...
            {
                c1.update(bi, 3, bi.length - 3);
            }
            else
            {
                System.arraycopy(bi, 2, tmp, tmp.length - (bi.length - 2), bi.length - 2);
                c1.update(tmp);
            }
        }

        try
        {
            return c1.doFinal();
        }
        catch (Exception e)
        {
            throw new PGPException("exception decrypting session data", e);
        }
    }

    Cipher createPublicKeyCipher(int encAlgorithm)
        throws PGPException
    {
        switch (encAlgorithm)
        {
        case PGPPublicKey.RSA_ENCRYPT:
        case PGPPublicKey.RSA_GENERAL:
            return createCipher("RSA/ECB/PKCS1Padding");
        case PGPPublicKey.ELGAMAL_ENCRYPT:
        case PGPPublicKey.ELGAMAL_GENERAL:
            return createCipher("ElGamal/ECB/PKCS1Padding");
        case PGPPublicKey.DSA:
            throw new PGPException("Can't use DSA for encryption.");
        case PGPPublicKey.ECDSA:
            throw new PGPException("Can't use ECDSA for encryption.");
        default:
            throw new PGPException("unknown asymmetric algorithm: " + encAlgorithm);
        }
    }
    
    Cipher createKeyWrapper(int encAlgorithm) throws PGPException {
        try
        {
            switch (encAlgorithm)
            {
            case SymmetricKeyAlgorithmTags.AES_128:
            case SymmetricKeyAlgorithmTags.AES_192:
            case SymmetricKeyAlgorithmTags.AES_256:
                return new NamedJcaJceHelper("BC").createCipher("AESWrap");
            case SymmetricKeyAlgorithmTags.CAMELLIA_128:
            case SymmetricKeyAlgorithmTags.CAMELLIA_192:
            case SymmetricKeyAlgorithmTags.CAMELLIA_256:
                return new NamedJcaJceHelper("BC").createCipher("CamelliaWrap");
            default:
                throw new PGPException("unknown wrap algorithm: " + encAlgorithm);
            }
        }
        catch (GeneralSecurityException e)
        {
            throw new PGPException("cannot create cipher: " + e.getMessage(), e);
        }
    }
    
    private boolean confirmCheckSum(
        byte[]    sessionInfo)
    {
        int    check = 0;

        for (int i = 1; i != sessionInfo.length - 2; i++)
        {
            check += sessionInfo[i] & 0xff;
        }

        return (sessionInfo[sessionInfo.length - 2] == (byte)(check >> 8))
                    && (sessionInfo[sessionInfo.length - 1] == (byte)(check));
    }

    public int getVersion() {
        return this.version;
    }
    
    public long getKeyID() {
        return this.keyID;
    }
    
    public int getAlgorithm() {
        return this.algorithm;
    }
    
    public byte[][] getEncSessionKey() {
        return this.data;
    }

    /*
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        ByteArrayOutputStream  bOut = new ByteArrayOutputStream();
        BCPGOutputStream       pOut = new BCPGOutputStream(bOut);
  
          pOut.write(version);
          
        pOut.write((byte)(keyID >> 56));
        pOut.write((byte)(keyID >> 48));
        pOut.write((byte)(keyID >> 40));
        pOut.write((byte)(keyID >> 32));
        pOut.write((byte)(keyID >> 24));
        pOut.write((byte)(keyID >> 16));
        pOut.write((byte)(keyID >> 8));
        pOut.write((byte)(keyID));
        
        pOut.write(algorithm);
        
        for (int i = 0; i != data.length; i++)
        {
            pOut.write(data[i]);
        }

        pOut.close();

        out.writePacket(PUBLIC_KEY_ENC_SESSION , bOut.toByteArray(), true);
    }
    */
}