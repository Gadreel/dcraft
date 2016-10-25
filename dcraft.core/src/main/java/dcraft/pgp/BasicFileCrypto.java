package dcraft.pgp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.io.Streams;

/**
 * copied from BC examples
 * A simple utility class that encrypts/decrypts public key based
 * encryption large files.
 * <p>
 * To encrypt a file: KeyBasedLargeFileProcessor -e [-a|-ai] fileName publicKeyFile.<br>
 * If -a is specified the output file will be "ascii-armored".
 * If -i is specified the output file will be have integrity checking added.
 * <p>
 * To decrypt: KeyBasedLargeFileProcessor -d fileName secretKeyFile passPhrase.
 * <p>
 * Note 1: this example will silently overwrite files, nor does it pay any attention to
 * the specification of "_CONSOLE" in the filename. It also expects that a single pass phrase
 * will have been used.
 * <p>
 * Note 2: this example generates partial packets to encode the file, the output it generates
 * will not be readable by older PGP products or products that don't support partial packet 
 * encoding.
 * <p>
 * Note 3: if an empty file name has been specified in the literal data object contained in the
 * encrypted packet a file with the name filename.out will be generated in the current working directory.
 */
public class BasicFileCrypto
{
    public static void decryptFile(
        Path inputFileName,
        KeyRingCollection rings,
        String passwd,
        Path defaultFileName)
        throws IOException, NoSuchProviderException
    {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(inputFileName))) {
        	decryptFile(in, rings, passwd, defaultFileName);
        }
    }
    
    /**
     * decrypt the passed in message stream
     */
    public static void decryptFile(
        InputStream in,
        KeyRingCollection rings,
        String      passwd,
        Path      defaultFileName)
        throws IOException, NoSuchProviderException
    {    
        in = PGPUtil.getDecoderStream(in);
        
        try
        {
            JcaPGPObjectFactory        pgpF = new JcaPGPObjectFactory(in);
            PGPEncryptedDataList    enc;

            Object                  o = pgpF.nextObject();
            //
            // the first object might be a PGP marker packet.
            //
            if (o instanceof PGPEncryptedDataList)
            {
                enc = (PGPEncryptedDataList)o;
            }
            else
            {
                enc = (PGPEncryptedDataList)pgpF.nextObject();
            }
            
            //
            // find the secret key
            //
			@SuppressWarnings("unchecked")
			Iterator<PGPPublicKeyEncryptedData> it = enc.getEncryptedDataObjects();
            PGPPrivateKey               sKey = null;
            PGPPublicKeyEncryptedData   pbe = null;
            
            while (sKey == null && it.hasNext()) {
                pbe = it.next();
                sKey = rings.findSecretKey(pbe.getKeyID(), passwd.toCharArray());
            }
            
            if (sKey == null)
                throw new IllegalArgumentException("secret key for message not found.");
            
            InputStream clear = pbe.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(sKey));
            
            JcaPGPObjectFactory plainFact = new JcaPGPObjectFactory(clear);
            
            Object              message = plainFact.nextObject();
    
            if (message instanceof PGPCompressedData) {
                PGPCompressedData   cData = (PGPCompressedData)message;
                JcaPGPObjectFactory    pgpFact = new JcaPGPObjectFactory(new BufferedInputStream(cData.getDataStream()));
                
                message = pgpFact.nextObject();
            }            
            
            if (message instanceof PGPLiteralData) {
                PGPLiteralData ld = (PGPLiteralData)message;
                
                /*
                String outFileName = ld.getFileName();
                if (outFileName.length() == 0)
                {
                    outFileName = defaultFileName;
                }
                */

                InputStream unc = ld.getInputStream();
                //OutputStream fOut =  new BufferedOutputStream(new FileOutputStream(outFileName));
                OutputStream fOut =  new BufferedOutputStream(Files.newOutputStream(defaultFileName));

                Streams.pipeAll(unc, fOut);

                fOut.close();
            }
            else if (message instanceof PGPOnePassSignatureList)
            {
                throw new PGPException("encrypted message contains a signed message - not literal data.");
            }
            else
            {
                throw new PGPException("message is not a simple encrypted file - type unknown.");
            }

            if (pbe.isIntegrityProtected())
            {
                if (!pbe.verify())
                {
                    System.err.println("message failed integrity check");
                }
                else
                {
                    System.err.println("message integrity check passed");
                }
            }
            else
            {
                System.err.println("no message integrity check");
            }
        }
        catch (PGPException e)
        {
            System.err.println(e);
            if (e.getUnderlyingException() != null)
            {
                e.getUnderlyingException().printStackTrace();
            }
        }
    }

    public static void encryptFile(
        Path          outputFileName,
        Path          inputFileName,
        PGPPublicKey 	pubEncKey,
        boolean         armor,
        boolean         withIntegrityCheck,
        boolean compress)
        throws IOException, NoSuchProviderException, PGPException
    {
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(outputFileName))) {
        	encryptFile(out, inputFileName, pubEncKey, armor, withIntegrityCheck, compress);
        }
    }

    public static void encryptFile(
        OutputStream    out,
        Path          fileName,
        PGPPublicKey 	pubEncKey,
        boolean         armor,
        boolean         withIntegrityCheck,
        boolean compress)
        throws IOException, NoSuchProviderException
    {    
        if (armor)
        {
            out = new ArmoredOutputStream(out);
        }
        
        try
        {    
            PGPEncryptedDataGenerator   cPk = new PGPEncryptedDataGenerator(new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(new SecureRandom()).setProvider("BC"));
                
            cPk.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(pubEncKey).setProvider("BC"));
            
            OutputStream                cOut = cPk.open(out, new byte[1 << 16]);
            
            if (compress) {
            	PGPCompressedDataGenerator  comData = new PGPCompressedDataGenerator(
                                                                    PGPCompressedData.ZIP);
                cOut = comData.open(cOut);
            }
            
            PGPUtil.writeFileToLiteralData(cOut, PGPLiteralData.BINARY, fileName.toFile(), new byte[1 << 16]);
            
            //comData.close();
            
            cOut.close();

            if (armor)
            {
                out.close();
            }
        }
        catch (PGPException e)
        {
            System.err.println(e);
            if (e.getUnderlyingException() != null)
            {
                e.getUnderlyingException().printStackTrace();
            }
        }
    }
}
