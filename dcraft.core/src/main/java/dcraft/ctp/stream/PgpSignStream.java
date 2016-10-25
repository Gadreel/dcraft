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
package dcraft.ctp.stream;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;

import dcraft.ctp.f.FileDescriptor;
import dcraft.lang.op.OperationContext;
import dcraft.pgp.KeyRingCollection;
import dcraft.script.StackEntry;
import dcraft.xml.XElement;

public class PgpSignStream extends BaseStream implements IStreamUp {
	protected FileDescriptor current = null;
	protected PGPSignature sig = null;
	protected PGPSecretKey signkey = null;
	protected char[] passphrase = null;
	protected PGPSignatureGenerator signgen = null; 
	protected Path outputfile = null;

	@Override
	public void init(StackEntry stack, XElement el) {
	}
    
    public PgpSignStream withSignKey(PGPSecretKeyRing v) {
    	this.signkey = KeyRingCollection.findSignKey(v);
    	
    	return this;
	}
    
    public PgpSignStream withPassphrase(String v) {
    	this.passphrase = v.toCharArray();
    	return this;
	}
    
    public PgpSignStream withOutputFile(Path v) {
    	this.outputfile = v;
    	return this;
	}
	
	public PGPSignatureGenerator getSigner() {
		if (this.signgen != null)
			return this.signgen;
		
		try {
			PGPPublicKey pubkey = this.signkey.getPublicKey();
	        PGPPrivateKey pgpPrivateKey = this.signkey.extractPrivateKey(
	        	new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(this.passphrase)
	        );
	
	        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(
	        	new BcPGPContentSignerBuilder(pubkey.getAlgorithm(), org.bouncycastle.openpgp.PGPUtil.SHA256)
	        );
	        
	        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, pgpPrivateKey);
	 
	        @SuppressWarnings("rawtypes")
			Iterator it = pubkey.getUserIDs();
	        
	        if (it.hasNext()) {
	            PGPSignatureSubpacketGenerator  spGen = new PGPSignatureSubpacketGenerator();
	            spGen.setSignerUserID(false, it.next().toString());
	            signatureGenerator.setHashedSubpackets(spGen.generate());
		        
		        this.signgen = signatureGenerator;
	        }
			
			return this.signgen;
		}
		catch (Exception x) {
			OperationContext.get().error("Unable to intialize file signing: " + x);
		}
		
		return null;
    }
	
	@Override
	public ReturnOption handle(FileSlice slice) {
		PGPSignatureGenerator signer = this.getSigner();
		
		// TODO someday detect change of fdesc and compute separate signs for each file passed through
		
		if (signer != null) {
	    	if (slice == FileSlice.FINAL) {
	    		try {
					this.sig = signer.generate();
					
					if (this.outputfile != null) {
				        try (OutputStream keyOut = new BufferedOutputStream(Files.newOutputStream(this.outputfile))) {
				        	this.sig.encode(keyOut);
				        }
					}
				} 
	    		catch (Exception x) {
					// TODO Auto-generated catch block
					x.printStackTrace();
					
					OperationContext.get().error("Unable to sign file: " + x);
				}
	    	}
	    	else if (slice.data != null) {
	    		signer.update(slice.data.array(), slice.data.arrayOffset() + slice.data.readerIndex(), slice.data.readableBytes());
	    	}
		}
		
   		return this.downstream.handle(slice);
	}

	@Override
	public void read() {
    	this.upstream.read();
	}
}
