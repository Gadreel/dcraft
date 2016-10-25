package dcraft.pgp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.Features;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRing;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.bc.BcPGPPublicKeyRing;
import org.bouncycastle.openpgp.bc.BcPGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.bc.BcPGPSecretKeyRing;
import org.bouncycastle.openpgp.bc.BcPGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;

import dcraft.lang.chars.Utf8Encoder;
import dcraft.util.KeyUtil;
import dcraft.util.StringUtil;

public class KeyRingCollection {
    
    static public PGPPublicKey findEncryptKey(PGPPublicKeyRing ring) {
    	if (ring == null)
        	return null;
    		
		Iterator<PGPPublicKey> keyIter = ring.getPublicKeys();
        
        while (keyIter.hasNext()) {
            PGPPublicKey key = keyIter.next();

            if (key.isEncryptionKey())
            	return key;
        }

        return null;
    }
    
    static public PGPSecretKey findSignKey(PGPSecretKeyRing ring) {
    	if (ring == null)
        	return null;
    		
        Iterator<PGPSecretKey> keyIter = ring.getSecretKeys();
         
        while (keyIter.hasNext()) {
            PGPSecretKey key = keyIter.next();
            
            if (key.isSigningKey()) 
                return key;
        }

        return null;
    }
    
    public static String getUserId(PGPKeyRing ring) {
    	PGPPublicKey pk = ring.getPublicKey();
    	
    	if (pk == null)
    		return null;
    	
		@SuppressWarnings("rawtypes")
		Iterator uit = pk.getUserIDs();
		
		if (!uit.hasNext()) 
			return null;
		
		return uit.next().toString();
    }
    
    public static PGPPublicKeyRing importArmoredPublicKeyToRing(String key, Path ringpath) throws Exception {
    	KeyRingCollection krc = KeyRingCollection.load(ringpath);
    	
    	return krc.importArmoredPublicKeyToRing(key);
    }
	
    public static PGPPublicKeyRing importArmoredPublicKeyToRing(Path file, Path ringpath) throws Exception {
    	KeyRingCollection krc = KeyRingCollection.load(ringpath);
    	
    	return krc.importArmoredPublicKeyToRing(file);
	}
	
    public static PGPPublicKeyRing importBinPublicKeyToRing(Path file, Path ringpath) throws Exception {
    	KeyRingCollection krc = KeyRingCollection.load(ringpath);
    	
    	return krc.importBinPublicKeyToRing(file);
	}
	
	public static PGPPublicKeyRing importPublicKeyToRing(InputStream src, Path ringpath) throws Exception {
    	KeyRingCollection krc = KeyRingCollection.load(ringpath);
        
        return krc.importPublicKeyToRing(src);
    }    
    
	public static PGPPublicKeyRing createKeyPairAddToRing(String identity, String pass, Path ringpath) throws Exception {
    	KeyRingCollection krc = KeyRingCollection.load(ringpath);
    	
        return krc.createKeyPairAddToRing(identity, pass);
    }
    
    public static PGPKeyRingGenerator generateKeyRingGenerator(String userid, String pass) throws Exception{
        return generateKeyRingGenerator(userid, pass, 2048, 0x60); 
    }

    // Note: s2kcount is a number between 0 and 0xff that controls the number of times to iterate the password hash before use. More
    // iterations are useful against offline attacks, as it takes more time to check each password. The actual number of iterations is
    // rather complex, and also depends on the hash function in use. Refer to Section 3.7.1.3 in rfc4880.txt. Bigger numbers give
    // you more iterations.  As a rough rule of thumb, when using SHA256 as the hashing function, 0x10 gives you about 64
    // iterations, 0x20 about 128, 0x30 about 256 and so on till 0xf0, or about 1 million iterations. The maximum you can go to is
    // 0xff, or about 2 million iterations.  I'll use 0xc0 as a default -- about 130,000 iterations.

    public static PGPKeyRingGenerator generateKeyRingGenerator(String userid, String pass, int keysize, int s2kcount) throws Exception {
        // This object generates individual key-pairs.
        RSAKeyPairGenerator  kpg = new RSAKeyPairGenerator();

        // Boilerplate RSA parameters, no need to change anything
        // except for the RSA key-size (2048). You can use whatever key-size makes sense for you -- 4096, etc.
        kpg.init(new RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), KeyUtil.random, keysize, 12));

        // First create the master (signing) key with the generator.
        PGPKeyPair rsakp_sign = new BcPGPKeyPair(PGPPublicKey.RSA_SIGN, kpg.generateKeyPair(), new Date());
        // Then an encryption subkey.
        PGPKeyPair rsakp_enc = new BcPGPKeyPair(PGPPublicKey.RSA_ENCRYPT, kpg.generateKeyPair(), new Date());

        // Add a self-signature on the id
        PGPSignatureSubpacketGenerator signhashgen = new PGPSignatureSubpacketGenerator();

        // Add signed metadata on the signature.
        // 1) Declare its purpose
        signhashgen.setKeyFlags(false, KeyFlags.SIGN_DATA|KeyFlags.CERTIFY_OTHER);
        // 2) Set preferences for secondary crypto algorithms to use when sending messages to this key.
        signhashgen.setPreferredSymmetricAlgorithms
            (false, new int[] {
                SymmetricKeyAlgorithmTags.AES_256,
                SymmetricKeyAlgorithmTags.AES_192,
                SymmetricKeyAlgorithmTags.AES_128
            });
        signhashgen.setPreferredHashAlgorithms
            (false, new int[] {
                HashAlgorithmTags.SHA256,
                HashAlgorithmTags.SHA1,
                HashAlgorithmTags.SHA384,
                HashAlgorithmTags.SHA512,
                HashAlgorithmTags.SHA224,
            });
        // 3) Request senders add additional checksums to the message (useful when verifying unsigned messages.)
        signhashgen.setFeature(false, Features.FEATURE_MODIFICATION_DETECTION);

        // Create a signature on the encryption subkey.
        PGPSignatureSubpacketGenerator enchashgen = new PGPSignatureSubpacketGenerator();
        // Add metadata to declare its purpose
        enchashgen.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS|KeyFlags.ENCRYPT_STORAGE);

        // Objects used to encrypt the secret key.
        PGPDigestCalculator sha1Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1);
        PGPDigestCalculator sha256Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA256);

        // bcpg 1.48 exposes this API that includes s2kcount. Earlier versions use a default of 0x60.
        PBESecretKeyEncryptor pske = (new BcPBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha256Calc, s2kcount)).build(pass.toCharArray());

        // Finally, create the keyring itself. The constructor takes parameters that allow it to generate the self signature.
        PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(
    		PGPSignature.POSITIVE_CERTIFICATION, 
    		rsakp_sign,
    		userid, 
    		sha1Calc, 
    		signhashgen.generate(), 
    		null,
    		new BcPGPContentSignerBuilder(rsakp_sign.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1), 
    		pske
       );

        // Add our encryption subkey, together with its signature.
        keyRingGen.addSubKey(rsakp_enc, enchashgen.generate(), null);
        
        return keyRingGen;
    }
	
	@SuppressWarnings("rawtypes")
	static public KeyRingCollection load(Path ringpath) throws Exception {
		KeyRingCollection rings = new KeyRingCollection();
		
    	// make sure the path is present
    	Files.createDirectories(ringpath);
    	
    	// point to the public and private files
    	rings.pubcoll = ringpath.resolve("pubring.gpg");
    	rings.seccoll = ringpath.resolve("secring.gpg");
    	
    	if (Files.exists(rings.pubcoll)) {
    		// load existing collection
	        try (InputStream keyIn = new BufferedInputStream(Files.newInputStream(rings.pubcoll))) {
	        	rings.pubring = new BcPGPPublicKeyRingCollection(org.bouncycastle.openpgp.PGPUtil.getDecoderStream(keyIn));
	        }
    	}
    	else {
    		// create an empty collection
    		rings.pubring = new BcPGPPublicKeyRingCollection(new ArrayList());
    	}
    	
    	if (Files.exists(rings.seccoll)) {
    		// load existing collection
	        try (InputStream keyIn = new BufferedInputStream(Files.newInputStream(rings.seccoll))) {
	        	rings.secring = new BcPGPSecretKeyRingCollection(org.bouncycastle.openpgp.PGPUtil.getDecoderStream(keyIn));
	        }
    	}
    	else {
    		// create an empty collection
    		rings.secring = new BcPGPSecretKeyRingCollection(new ArrayList());
    	}
		
    	return rings;
	}

	// instance
	
	protected Path pubcoll = null;
	protected Path seccoll = null;
	
	protected PGPPublicKeyRingCollection pubring = null;
	protected PGPSecretKeyRingCollection secring = null;
	
	public Path getFolder() {
		return this.pubcoll.getParent();
	}
	
    public PGPPublicKeyRing importArmoredPublicKeyToRing(String key) throws Exception {
    	return this.importPublicKeyToRing(new ArmoredInputStream(new ByteArrayInputStream(Utf8Encoder.encode(key)))); 
    }
	
    public PGPPublicKeyRing importArmoredPublicKeyToRing(Path file) throws Exception {
        try (InputStream keyIn = new BufferedInputStream(Files.newInputStream(file))) {
        	return this.importPublicKeyToRing(new ArmoredInputStream(keyIn)); 
        }
	}
	
    public PGPPublicKeyRing importBinPublicKeyToRing(Path file) throws Exception {
        try (InputStream keyIn = new BufferedInputStream(Files.newInputStream(file))) {
        	return this.importPublicKeyToRing(keyIn); 
        }
	}
	
	public PGPPublicKeyRing importPublicKeyToRing(InputStream src) throws Exception {
        // Generate public key ring, dump to file.
        PGPPublicKeyRing pkr = new BcPGPPublicKeyRing(src);
        
    	String uid = KeyRingCollection.getUserId(pkr);
    	
    	if (StringUtil.isNotEmpty(uid))
    		this.removeUser(uid);;
        
        this.addPublicKey(pkr);
        
        return pkr;
    }    
	
    public PGPSecretKeyRing importArmoredSecretKeyToRing(String key) throws Exception {
    	return this.importSecretKeyToRing(new ArmoredInputStream(new ByteArrayInputStream(Utf8Encoder.encode(key)))); 
    }
	
    public PGPSecretKeyRing importArmoredSecretKeyToRing(Path file) throws Exception {
        try (InputStream keyIn = new BufferedInputStream(Files.newInputStream(file))) {
        	return this.importSecretKeyToRing(new ArmoredInputStream(keyIn)); 
        }
	}
	
    public PGPSecretKeyRing importBinSecretKeyToRing(Path file) throws Exception {
        try (InputStream keyIn = new BufferedInputStream(Files.newInputStream(file))) {
        	return this.importSecretKeyToRing(keyIn); 
        }
	}
	
	// unlike import public, secret does not remove the old user first - so call import public first
	public PGPSecretKeyRing importSecretKeyToRing(InputStream src) throws Exception {
        // Generate public key ring, dump to file.
        PGPSecretKeyRing pkr = new BcPGPSecretKeyRing(src);
        
        this.addSecretKey(pkr);
        
        return pkr;
    }    
    
	public PGPPublicKeyRing createKeyPairAddToRing(String userid, String pass) throws Exception {
		this.removeUser(userid);
		
        PGPKeyRingGenerator krgen = KeyRingCollection.generateKeyRingGenerator(userid, pass, 2048, 0x40);

        // Generate public key ring, dump to file.
        PGPPublicKeyRing pkr = krgen.generatePublicKeyRing();
        this.addPublicKey(pkr);
        
        // Generate private key, dump to file.
        PGPSecretKeyRing skr = krgen.generateSecretKeyRing();
        this.addSecretKey(skr);
        
        return pkr;
    }
	
	public void addPublicKey(PGPPublicKeyRing pub) throws Exception {
		this.addPublicKey(pub, true);
    }
	
	public void addPublicKey(PGPPublicKeyRing pub, boolean save) throws Exception {
		// ensure the same id is not listed twice - remove original if so
		PGPPublicKeyRing oldkey = this.pubring.getPublicKeyRing(pub.getPublicKey().getKeyID());
		
		if (oldkey != null) 
			this.pubring = PGPPublicKeyRingCollection.removePublicKeyRing(this.pubring, oldkey);
		
		this.pubring = PGPPublicKeyRingCollection.addPublicKeyRing(this.pubring, pub);
        
		if (save)
			this.savePublicKeys();
	}

	public void addSecretKey(PGPSecretKeyRing skr) throws Exception {
		this.addSecretKey(skr, true);
	}

	public void addSecretKey(PGPSecretKeyRing skr, boolean save) throws Exception {
		// ensure the same id is not listed twice - remove original if so  (Id is always based on public key)
		PGPSecretKeyRing oldkey = this.secring.getSecretKeyRing(skr.getPublicKey().getKeyID());
		
		if (oldkey != null) 
			this.secring = PGPSecretKeyRingCollection.removeSecretKeyRing(this.secring, oldkey);
		
		this.secring = PGPSecretKeyRingCollection.addSecretKeyRing(this.secring, skr);
        
		if (save)
			this.saveSecretKeys();
	}
	
	public void save() throws Exception {
		this.savePublicKeys();
		this.saveSecretKeys();
	}
	
	public void savePublicKeys() throws Exception {
        try (OutputStream keyOut = new BufferedOutputStream(Files.newOutputStream(this.pubcoll))) {
        	this.pubring.encode(keyOut);
        }
	}
	
	public void saveSecretKeys() throws Exception {
        try (OutputStream keyOut = new BufferedOutputStream(Files.newOutputStream(this.seccoll))) {
        	this.secring.encode(keyOut);
        }
	}
	
	// all keys containing this identity, whether it be the name, the email or full (case insensitive match)
	@SuppressWarnings("rawtypes")
	public void removeUser(String userid) throws Exception {
		if (StringUtil.isEmpty(userid))
			return;
		
		userid = userid.toLowerCase();
    	boolean fndpub = false;
    	boolean fndsec = false;

    	// remove from public keys - don't use findUserKey, it only matches first
    	
		Iterator krit = this.pubring.iterator();
        
        while (krit.hasNext()) {
        	PGPPublicKeyRing pkr = (PGPPublicKeyRing) krit.next();
        	
        	String uid = KeyRingCollection.getUserId(pkr);
        	
        	if (StringUtil.isEmpty(uid))
        		continue;
        	
			if (uid.toLowerCase().contains(userid)) {
				this.pubring = PGPPublicKeyRingCollection.removePublicKeyRing(this.pubring, pkr);
				fndpub = true;
			}
        }
        
        if (fndpub)
        	this.savePublicKeys();

    	// remove from secret keys - don't use findUserKey, it only matches first
    	
		krit = this.secring.iterator();
        
        while (krit.hasNext()) {
        	PGPSecretKeyRing skr = (PGPSecretKeyRing) krit.next();
        	
        	String uid = KeyRingCollection.getUserId(skr);
        	
        	if (StringUtil.isEmpty(uid))
        		continue;
        	
			if (uid.toLowerCase().contains(userid)) {
				this.secring = PGPSecretKeyRingCollection.removeSecretKeyRing(this.secring, skr);
				fndsec = true;
			}
        }
        
        if (fndsec)
        	this.saveSecretKeys();
	}
	
	public PGPPublicKeyRing findUserPublicKey(String userid) {
		if (StringUtil.isEmpty(userid))
			return null;
		
		userid = userid.toLowerCase();
    	
		@SuppressWarnings("rawtypes")
		Iterator krit = this.pubring.iterator();
        
        while (krit.hasNext()) {
        	PGPPublicKeyRing pkr = (PGPPublicKeyRing) krit.next();
        	
        	String uid = KeyRingCollection.getUserId(pkr);
        	
        	if (StringUtil.isEmpty(uid))
        		continue;
        	
			if (uid.toLowerCase().contains(userid)) 
				return pkr;
        }		
        
        return null;
	}
	
	public PGPSecretKeyRing findUserSecretKey(String userid) {
		if (StringUtil.isEmpty(userid))
			return null;
		
		userid = userid.toLowerCase();
    	
		@SuppressWarnings("rawtypes")
		Iterator krit = this.secring.iterator();
        
        while (krit.hasNext()) {
        	PGPSecretKeyRing pkr = (PGPSecretKeyRing) krit.next();
        	
        	String uid = KeyRingCollection.getUserId(pkr);
        	
        	if (StringUtil.isEmpty(uid))
        		continue;
        	
			if (uid.toLowerCase().contains(userid)) 
				return pkr;
        }		
        
        return null;
	}
	
	public PGPPrivateKey findSecretKey(long keyid, char[] pass) throws PGPException {
		@SuppressWarnings("rawtypes")
		Iterator krit = this.secring.iterator();
        
        while (krit.hasNext()) {
        	PGPSecretKeyRing pkr = (PGPSecretKeyRing) krit.next();
        	
            PGPSecretKey pgpSecKey = pkr.getSecretKey(keyid);
        	
        	if (pgpSecKey == null)
        		continue;

            return pgpSecKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(pass));
        }		
        
        return null;
	}
	
	public PGPPublicKey findPublicKey(long keyid) throws PGPException {
		Iterator<PGPPublicKeyRing> krit = this.pubring.iterator();
        
        while (krit.hasNext()) {
        	PGPPublicKeyRing pkr = krit.next();
        	
            PGPPublicKey pgpPubKey = pkr.getPublicKey(keyid);
        	
        	if (pgpPubKey != null)
        		return pgpPubKey;
        }
        
        return null;
	}
	
	// export first key containing this identity, whether it be the name, the email or full (case insensitive match)
	public void exportUser(String userid, String fname) throws Exception {
		this.exportUser(userid, this.pubcoll.getParent(), fname, "sec");
	}
	
	// export first key containing this identity, whether it be the name, the email or full (case insensitive match)
	public void exportUser(String userid, Path dest, String fname) throws Exception {
		this.exportUser(userid, dest, fname, "sec");
	}
	
	// export first key containing this identity, whether it be the name, the email or full (case insensitive match)
	public void exportUser(String userid, Path dest, String fname, String secExt) throws Exception {
    	PGPPublicKeyRing pkr = this.findUserPublicKey(userid);
        	
    	if (pkr == null)
    		return;
    	
        try (OutputStream keyOut = new ArmoredOutputStream(new BufferedOutputStream(Files.newOutputStream(dest.resolve(fname + ".pub"))))) {
        	pkr.encode(keyOut);
        }
        
		PGPSecretKeyRing sec = this.secring.getSecretKeyRing(pkr.getPublicKey().getKeyID());
		
		if (sec == null) 
			return;
    	
        try (OutputStream keyOut = new ArmoredOutputStream(new BufferedOutputStream(Files.newOutputStream(dest.resolve(fname + "." + secExt))))) {
        	sec.encode(keyOut);
        }
	}

	public List<PGPPublicKeyRing> getPublicKeys() {
		List<PGPPublicKeyRing> keys = new ArrayList<>();
		
        @SuppressWarnings("rawtypes")
		Iterator it = this.pubring.iterator();
        
        while (it.hasNext()) 
        	keys.add((PGPPublicKeyRing)it.next());
		
        return keys;
	}

	public List<PGPSecretKeyRing> getSecretKeys() {
		List<PGPSecretKeyRing> keys = new ArrayList<>();
		
        @SuppressWarnings("rawtypes")
		Iterator it = this.secring.iterator();
        
        while (it.hasNext()) 
        	keys.add((PGPSecretKeyRing)it.next());
		
        return keys;
	}
}
