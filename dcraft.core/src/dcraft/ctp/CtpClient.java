package dcraft.ctp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import dcraft.ctp.CtpAdapter;
import dcraft.ctp.CtpCommand;
import dcraft.ctp.ICommandHandler;
import dcraft.ctp.cmd.EngageCommand;
import dcraft.ctp.net.CtpHandler;
import dcraft.hub.Hub;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationCallback;
import dcraft.struct.RecordStruct;
import dcraft.work.IWork;
import dcraft.work.Task;
import dcraft.work.TaskRun;

abstract public class CtpClient implements ICommandHandler {
	protected CtpAdapter adapter = null;
	protected RecordStruct startRecord = null;
	
	public void sendCommand(CtpCommand cmd) {
		try {
			this.adapter.sendCommand(cmd);
		} 
		catch (Exception x) {
			System.out.println("ctp client send error: " + x);
		}
	}
	
	// when using this be sure to issue "read()" in callback
	public void sendCommand(CtpCommand cmd, FuncCallback<RecordStruct> cb) {
		try {
			this.adapter.sendCommand(cmd, cb);
		} 
		catch (Exception x) {
			System.out.println("ctp client send error: " + x);
		}
	}
	
	public void read() {
		this.adapter.read();
	}
	
	public void connect(String host, int port, OperationCallback connCallback) {
		this.adapter = new CtpAdapter(); // TODO maybe from session one day?
											// OperationContext.get().getSession().allocateCtpAdapter();

		this.adapter.setHandler(this);

		EventLoopGroup el = Hub.instance.getEventLoopGroup();

		Bootstrap cb = new Bootstrap()
				.group(el)
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(
								//new SslHandler(SslContextFactory.getClientEngine()),		// TODO use surface context, not bus
          						// TODO put Zlib encoding directly into CtpHandler so "read" works better
								//new JdkZlibEncoder(ZlibWrapper.ZLIB),
								//new JdkZlibDecoder(ZlibWrapper.ZLIB),
								//new LoggingHandler(LogLevel.INFO),
								new CtpHandler(CtpClient.this.adapter, false));
					}
				});

		// Start the client.
		try {
			cb.connect(host, port).sync();
		} 
		catch (InterruptedException x) {
			System.out.println("Could not connect to Ctp Server");
			connCallback.error("Unable to connect: " + x);
			connCallback.callback();
			return;
		}
	    
	    System.out.println("Ctp Client - Connected");
	    
	    EngageCommand cmd = new EngageCommand();
	    cmd.setBody(this.startRecord);

	    this.startRecord = null;
	    
	    //System.out.println("Ctp Client - Init Sent");
	    
	    Task alive = Task.taskWithSubContext().withWork(new IWork() {			
			@Override
			public void run(TaskRun trun) {
	            //System.out.println("Ctp Client - Active: " + chx.isActive());
	            
	            try {
	            	CtpClient.this.adapter.sendCommand(CtpCommand.ALIVE);
				}
				catch (Exception x) {
					System.out.println("Ctp-F Client error: " + x);
				}
	            
	            trun.complete();
			}
		});
	    
	    this.sendCommand(cmd, new FuncCallback<RecordStruct>() {			
			@Override
			public void callback() {
				if (!this.hasErrors())				
					Hub.instance.getScheduler().runEvery(alive, 30);
			    
				// we are willing to process more messages
				adapter.read();
				
				// if errors then caller should close
				connCallback.complete();
			}
		});
	}

}
