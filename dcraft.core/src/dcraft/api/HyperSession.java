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
package dcraft.api;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.util.concurrent.Future;

import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import dcraft.api.ClientInfo.ConnectorKind;
import dcraft.api.internal.ApiSslContextFactory;
import dcraft.api.internal.ClientHandler;
import dcraft.api.internal.DownloadHandler;
import dcraft.api.internal.UploadPutHandler;
import dcraft.bus.Message;
import dcraft.hub.Hub;
import dcraft.lang.op.OperationCallback;
import dcraft.lang.op.OperationResult;
import dcraft.lang.op.UserContext;
import dcraft.net.ssl.SslHandler;
import dcraft.work.ISynchronousWork;
import dcraft.work.TaskRun;
import dcraft.xml.XElement;

public class HyperSession extends ApiSession {
	protected ClientInfo info = null;
	protected ApiSslContextFactory sslfac = null;
    
	protected ClientHandler handler = null;

	protected HashMap<String, UploadPutHandler> uploadstreams = new HashMap<>();
	protected HashMap<String, DownloadHandler> downloadstreams = new HashMap<>();
	
	public ClientInfo getInfo() {
		return this.info;
	}
	
	public ApiSslContextFactory getSsl() {
		return this.sslfac;
	}

	@Override
    public void init(XElement config) {
    	// run only once even if call multiple times
		if (this.info != null)
			return;

		if (config == null)
			return;
        
        this.info = new ClientInfo();
        this.info.loadConfig(config);
        this.info.kind = ConnectorKind.Http;
        
        this.sslfac = new ApiSslContextFactory();
        this.sslfac.init(config);
        
        this.user = UserContext.allocateGuest();
	}
	
	public OperationResult connect() {
		OperationResult or = new OperationResult();
		
		// only ever one connection for now
		if (this.handler != null)
			return or;

		this.handler = new ClientHandler(this, this.info);
		
		this.allocateHttpChannel(this.handler, or);
        
        return or;
    }
	
	public Channel allocateHttpChannel(final ChannelHandler handler, OperationResult or) {
		final AtomicReference<Future<Channel>> sslready = new AtomicReference<>();
		
        Bootstrap b = new Bootstrap();
        
        b.group(Hub.instance.getEventLoopGroup())
         .channel(NioSocketChannel.class)
         .option(ChannelOption.ALLOCATOR, Hub.instance.getBufferAllocator())
         .handler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) throws Exception {
                 ChannelPipeline pipeline = ch.pipeline();
                 
                 if (HyperSession.this.info.isSecurel()) {
                	 SslHandler sh = new SslHandler(HyperSession.this.sslfac.getClientEngine());
                	 sslready.set(sh.handshakeFuture());
                	 pipeline.addLast("ssl", sh);
                 }
	    	        
    	        pipeline.addLast("decoder", new HttpResponseDecoder());
    	        pipeline.addLast("encoder", new HttpRequestEncoder());
    	        
    	        // TODO maybe
    	        //pipeline.addLast("deflater", new HttpContentCompressor());
                 
                 pipeline.addLast("handler", handler);
             }
         });

        or.info("Web Client connecting");
        
        try {
        	// must wait here to make sure we don't release connectLock too soon
        	// we want channel init (above) to complete before we try connect again
        	ChannelFuture f = b.connect(this.info.getAddress()).sync();
        	
        	if (!f.isSuccess()) {
            	or.error(1, "Web Client unable to successfully connect: " + f.cause());
        	}
        	
        	// it has appeared that sometimes we "overshoot" the ssl handshake in code - to prevent
        	// that lets wait for the handshake to be done for sure
        	if (sslready.get() != null) {
        		Future<Channel> sf = sslready.get().sync();
            	
            	if (!sf.isSuccess()) {
                	or.error(1, "Web Client unable to securely connect: " + sf.cause());
            	}
        	}
        	
        	if (handler instanceof ClientHandler) 
        		((ClientHandler)handler).waitConnect();
        	
        	return f.channel();
        }
        catch (InterruptedException x) {
        	or.error(1, "Web Client interrupted while connecting: " + x);
        }
        catch (Exception x) {
        	or.error(1, "Web Client unable to connect: " + x);
        }
        
        return null;
	}
		
	@Override
	public void stopped() {
		if (this.handler != null)
			this.handler.close();
		
		this.replies.forgetReplyAll();
	}
	
	@Override
	public void receiveMessage(final Message msg) {
		// throw this work into another thread so socket reader can go back to work		
		Hub.instance.getWorkPool().submit(new ISynchronousWork() {
			@Override
			public void run(TaskRun task) {
				String to = msg.getFieldAsString("Service");
				
				if ("Replies".equals(to)) 
					HyperSession.this.replies.handle(msg);
				else
					HyperSession.super.receiveMessage(msg);
			}
		});
	}

	@Override
	public void sendForgetMessage(Message msg) {
    	msg.setField("RespondTag", "SendForget");
		
		if (this.connect().hasErrors()) 
			return;

    	this.handler.send(msg);
	}
	
	@Override
	public void sendMessage(final Message msg, final ServiceResult callback) {
		OperationResult or = this.connect();
		
		if (or.hasErrors()) {
			callback.complete();
			return;
		}
		
		callback.setSession(this);
    	
		this.replies.registerForReplySerial(msg, callback);

    	this.handler.send(msg);
    }
	
	@Override
	public void abortStream(String channelid) {
		UploadPutHandler uphandler = this.uploadstreams.get(channelid);
		
		if (uphandler != null)
			uphandler.closeDest();
		
		DownloadHandler dhandler = this.downloadstreams.get(channelid);
		
		if (dhandler != null)
			dhandler.closeSource();
	}
	
	@Override
	public void sendStream(ScatteringByteChannel in, long size, long offset, String chanid, OperationCallback callback) {
		// we also have UploadMultipartPostHandler...much slower
		UploadPutHandler uphandler = new UploadPutHandler();
		
		this.uploadstreams.put(chanid, uphandler);
		
		uphandler.start(this, in, chanid, this.handler.getCookies(), size, offset, callback);
	}
	
	@Override
	public void receiveStream(WritableByteChannel out, long size, long offset, String chanid, OperationCallback callback) {
		DownloadHandler dhandler = new DownloadHandler();
		
		this.downloadstreams.put(chanid, dhandler);
		
		dhandler.start(this, out, chanid, this.handler.getCookies(), size, offset, callback);
	}
	
	@Override
	public void freeDataChannel(String channelid, OperationCallback callback) {
		this.uploadstreams.remove(channelid);
		this.downloadstreams.remove(channelid);
		
		super.freeDataChannel(channelid, callback);
	}
}
