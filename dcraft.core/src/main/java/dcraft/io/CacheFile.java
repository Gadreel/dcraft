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
package dcraft.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import dcraft.hub.Hub;
import dcraft.lang.chars.Utf8Decoder;
import dcraft.struct.CompositeStruct;
import dcraft.struct.Struct;
import dcraft.util.FileUtil;
import dcraft.xml.XElement;
import dcraft.xml.XmlReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;

public class CacheFile {
	static public CacheFile fromFile(String innerpath, Path path) {
		try {
			if (Files.isDirectory(path) || Files.isHidden(path) || !Files.isReadable(path))
				return null;
		}
		catch (Exception x){
			// TODO trace
			return null;
		}
		
		CacheFile asset = new CacheFile();
		
		try {
			asset.when = Files.getLastModifiedTime(path).toMillis();
		} 
		catch (IOException x) {
			asset.when = System.currentTimeMillis();
		}
		
		asset.path = innerpath;
		asset.fpath = path;
		asset.ext = FileUtil.getFileExtension(innerpath);
		
		return asset;
	}
	
	static public CacheFile fromBuffer(String innerpath, Path path, ByteBuf content) {
		CacheFile asset = new CacheFile();
		
		asset.buffer = content;
		asset.when = System.currentTimeMillis();
		asset.path = innerpath;
		asset.fpath = path;
		asset.ext = FileUtil.getFileExtension(innerpath);
		
		return asset;
	}
	
	// instance
	// we cannot lookup mime at this level since cache is shared between all domains
	// instead note the ext here which then translates to a mime per domain/site
	protected Path fpath = null;
	protected long when = 0;
	protected String ext = null;
	protected String path = null;
	
	protected ByteBuf buffer = null;
	
	/*
	protected CharSequence chars = null;
	protected CompositeStruct json = null;
	protected XElement xml = null;
	*/
	
	public String getExt() {
		return this.ext;
	}
	
	public String getPath() {
		return this.path;
	}
	
	// this is for debugging, do not rely on or expect this value
	public Path getFilePath() {
		return this.fpath;
	}
	
	public long getWhen() {
		if (this.fpath != null)
			try {
				this.when = Files.getLastModifiedTime(this.fpath).toMillis();
			} 
			catch (IOException x) {
			}
		
		return this.when;
	}
	
	protected CacheFile() {
	}
	
	public XElement asXml() {
		/*
		if (this.xml != null)
			return this.xml;
		
		this.asChars();
		
		if (this.chars == null)
			return null;

		this.xml = XmlReader.parse(this.chars, false).getResult();
		
		return this.xml;
		*/
		
		return XmlReader.parse(this.asChars(), false).getResult();
	}
	
	public CompositeStruct asJson() {
		/*
		if (this.json != null)
			return this.json;
		
		this.asChars();
		
		if (this.chars == null)
			return null;

		this.json = Struct.objectToComposite(this.chars);
		
		return this.json;
		*/
		
		return Struct.objectToComposite(this.asChars());
	}
	
	
	public String asString() {
		/*
		if (this.chars != null)
			return this.chars.toString();
			
		this.asChars();
		
		if (this.chars == null)
			return null;

		return this.chars.toString();
		*/
		
		CharSequence c = this.asChars();
		
		if (c != null)
			return c.toString();
		
		return null;
	}
	
	public CharSequence asChars() {
		/*
		if (this.chars != null)
			return this.chars;
			
		this.asBuffer();
		
		if (this.buffer == null)
			return null;
		
		if (this.chars == null)
			this.chars = Utf8Decoder.decode(this.buffer);
		
		return this.chars;
		*/
		
		ByteBuf b = this.asBuffer();

		if (b != null) {
			CharSequence ret = Utf8Decoder.decode(b);
			b.release();
			return ret;
		}
		
		return null;
	}
	
	// calls to this should do a release
	public ByteBuf asBuffer() {
		if ((this.buffer == null) && (this.fpath != null))
			try {
				long fsize = Files.size(this.fpath);
				
				ByteBuf buff = Hub.instance.getBufferAllocator().heapBuffer((int) fsize);
				
				ByteBufOutputStream out = new ByteBufOutputStream(buff);
				
				long csize = Files.copy(this.fpath, out);
				
				if (csize != fsize) 
					System.out.println("Issue with RAW ASSET buffer - unable to load complete file.");
				else
					return buff;   //this.buffer = buff;
			} 
			catch (IOException x) {
				// TODO improve support
				System.out.println("Issue with RAW ASSET buffer: " + x);
			}
		
		if (this.buffer != null) {
			this.buffer.retain();
			return this.buffer;
		}
		
		return null;
	}

	public int getSize() {
		if (this.buffer != null)
			return this.buffer.readableBytes();
		
		try {
			if (this.fpath != null)
				return (int) Files.size(this.fpath);
		}
		catch (Exception x) {
		}
		
		return 0;
	}
}
