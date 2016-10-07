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
package dcraft.ctp.f;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import dcraft.ctp.CtpConstants;
import dcraft.filestore.CommonPath;
import dcraft.filestore.IFileStoreFile;
import dcraft.lang.chars.Utf8Decoder;
import dcraft.lang.chars.Utf8Encoder;
import dcraft.util.IOUtil;

public class FileDescriptor {
	static public FileDescriptor fromFileStore(IFileStoreFile file) {
		FileDescriptor ref = new FileDescriptor();
		ref.setPath(file.getPath());
		ref.setIsFolder(file.isFolder());
		ref.setSize(file.getSize());
		ref.setModTime(file.getModificationTime().getMillis());
		
		// TODO permission
		
		return ref;
	}
	
	public Map<Integer, byte[]> headers = new HashMap<>();

	public void setIsFolder(boolean v) {
		this.headers.put(CtpConstants.CTP_F_ATTR_IS_FOLDER, v ? new byte[] { 0x01 } : new byte[] { 0x00 } );
	}
	
	public boolean isFolder() {
		byte[] attr = headers.get(CtpConstants.CTP_F_ATTR_IS_FOLDER);
		
		if (attr == null)
			return false;
		
		return attr[0] == 0x01;
	}

	public void setSize(long size) {
		this.headers.put(CtpConstants.CTP_F_ATTR_SIZE, IOUtil.longToByteArray(size));
	}
	
	public long getSize() {
		byte[] attr = headers.get(CtpConstants.CTP_F_ATTR_SIZE);
		
		if (attr == null)
			return 0;
		
		return IOUtil.byteArrayToLong(attr);
	}

	public void setModTime(long millis) {
		this.headers.put(CtpConstants.CTP_F_ATTR_MODTIME, IOUtil.longToByteArray(millis));
	}
	
	public long getModTime() {
		byte[] attr = headers.get(CtpConstants.CTP_F_ATTR_MODTIME);
		
		if (attr == null)
			return 0;
		
		return IOUtil.byteArrayToLong(attr);
	}

	public void setPermissions(int v) {
		this.headers.put(CtpConstants.CTP_F_ATTR_PERMISSIONS, IOUtil.intToByteArray(v));
	}
	
	public int getPermissions() {
		byte[] attr = headers.get(CtpConstants.CTP_F_ATTR_PERMISSIONS);
		
		if (attr == null)
			return 0;
		
		return IOUtil.byteArrayToInt(attr);
	}
	
	public void setPath(CharSequence v) {
		this.headers.put(CtpConstants.CTP_F_ATTR_PATH, Utf8Encoder.encode(v));
	}
	
	public void setPath(CommonPath v) {
		this.headers.put(CtpConstants.CTP_F_ATTR_PATH, Utf8Encoder.encode(v.toString()));
	}
	
	public boolean hasPath() {
		byte[] attr = headers.get(CtpConstants.CTP_F_ATTR_PATH);
		
		return (attr != null);
	}
	
	public String getPath() {
		byte[] attr = headers.get(CtpConstants.CTP_F_ATTR_PATH);
		
		if (attr == null)
			return null;
		
		return Utf8Decoder.decode(attr).toString();
	}
	
	public CommonPath path() {
		byte[] attr = headers.get(CtpConstants.CTP_F_ATTR_PATH);
		
		if (attr == null)
			return null;
		
		return new CommonPath(Utf8Decoder.decode(attr).toString());
	}

	public void copyAttributes(BlockCommand cmd) {
		for (Entry<Integer, byte[]> attr : cmd.headers.entrySet())
			this.headers.put(attr.getKey(), attr.getValue());
	}

	public void copyAttributes(FileDescriptor file) {
		for (Entry<Integer, byte[]> attr : file.headers.entrySet())
			this.headers.put(attr.getKey(), attr.getValue());
	}
}
