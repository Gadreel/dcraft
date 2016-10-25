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
package dcraft.filestore;

import org.joda.time.DateTime;

import dcraft.ctp.stream.IStreamDest;
import dcraft.ctp.stream.IStreamSource;
import dcraft.lang.Memory;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationCallback;
import dcraft.script.StackEntry;
import dcraft.session.DataStreamChannel;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.xml.XElement;

public interface IFileStoreFile {
	String getName();
	void setName(String v);
	String getPath();
	void setPath(String v);
	String getExtension();
	String getFullPath();
	DateTime getModificationTime();
	String getModification();
	long getSize();
	boolean isFolder();
	void isFolder(boolean b);
	boolean exists();
	
	CommonPath path();
	CommonPath resolvePath(CommonPath path);
	
	IFileStoreDriver driver();
	IFileStoreScanner scanner();
	
	// TODO use DataStreamChannel instead
	//void copyTo(OutputStream out, OperationCallback callback);
	
	void hash(String method, FuncCallback<String> callback);
	
	// TODO use DataStreamChannel instead
	//void getInputStream(FuncCallback<InputStream> callback);
	
	void rename(String name, OperationCallback callback);
	
	void remove(OperationCallback callback);
	
	void setModificationTime(DateTime time, OperationCallback callback);
	
	void getAttribute(String name, FuncCallback<Struct> callback);
	void setAttribute(String name, Struct value, OperationCallback callback);
	
	// scripts
	public void operation(final StackEntry stack, XElement code);
	
	// remote
	
	void openRead(DataStreamChannel channel, FuncCallback<RecordStruct> callback);
	void openWrite(DataStreamChannel channel, FuncCallback<RecordStruct> callback);
	
	IStreamDest allocDest();
	IStreamSource allocSrc();
	
	void readAllText(FuncCallback<String> callback);
	void readAllBinary(FuncCallback<Memory> callback);
	
	void writeAllText(String v, OperationCallback callback);
	void writeAllBinary(Memory v, OperationCallback callback);
	RecordStruct getExtra();
}
