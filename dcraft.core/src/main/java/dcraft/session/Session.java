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
package dcraft.session;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import dcraft.bus.Message;
import dcraft.bus.MessageUtil;
import dcraft.bus.ServiceResult;
import dcraft.ctp.CtpAdapter;
import dcraft.hub.Hub;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.IOperationObserver;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationContextBuilder;
import dcraft.lang.op.OperationObserver;
import dcraft.lang.op.OperationResult;
import dcraft.lang.op.UserContext;
import dcraft.log.DebugLevel;
import dcraft.log.HubLog;
import dcraft.log.Logger;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;
import dcraft.util.TimeUtil;
import dcraft.work.ISynchronousWork;
import dcraft.work.Task;
import dcraft.work.TaskRun;

// TODO needs a plan system for what to do when session ends/times out/etc 
public class Session {
	static protected SecureRandom random = new SecureRandom();
	static protected AtomicLong taskid = new AtomicLong();
	
	static public String nextSessionId() {
		  return new BigInteger(130, Session.random).toString(32);
	}	
	
	static public String nextUUId() {
		  return UUID.randomUUID().toString().replace("-", "");
	}	
	
	protected String id = null;
	protected String key = null;
	protected long lastAccess = 0;
	protected long lastReauthAccess = System.currentTimeMillis();
	protected UserContext user = null;
    protected AtomicReference<String> authtokenupdate = new AtomicReference<>();
    protected List<String> pastauthtokens = new ArrayList<>();
	protected DebugLevel level = null;
	protected String originalOrigin = null;

	protected HashMap<String, Struct> cache = new HashMap<>();
	protected HashMap<String, IComponent> components = new HashMap<>();
	
	protected ReentrantLock tasklock = new ReentrantLock();
	protected HashMap<String, TaskRun> tasks = new HashMap<>();
	
	protected ReentrantLock channellock = new ReentrantLock();
	protected HashMap<String, DataStreamChannel> channels = new HashMap<>();
	
	protected ISessionAdapter adapter = null;
	protected HashMap<String, SendWaitInfo> sendwaits = new HashMap<>();
	
	protected boolean keep = false;
	
	/* add interactive debugging - for root only (config), gateway prohibited - base on code from
	 * 
	 * https://github.com/sheehan/grails-console/blob/d6c12eea6abc0e7bfa1f78b51939b05944e0ec0b/grails3/plugin/grails-app/services/org/grails/plugins/console/ConsoleService.groovy
	 * 
	 * especially this:
	 * 
    Evaluation eval(String code, boolean autoImportDomains, request) {
        log.trace "eval() code: $code"

        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        PrintStream out = new PrintStream(baos)

        SystemOutputInterceptor systemOutInterceptor = createInterceptor(out)
        systemOutInterceptor.start()

        Evaluation evaluation = new Evaluation()

        long startTime = System.currentTimeMillis()
        try {
            Binding binding = createBinding(request, out)
            CompilerConfiguration configuration = createConfiguration(autoImportDomains)

            GroovyShell groovyShell = new GroovyShell(grailsApplication.classLoader, binding, configuration)
            evaluation.result = groovyShell.evaluate code
        } catch (Throwable t) {
            evaluation.exception = t
        }

        evaluation.totalTime = System.currentTimeMillis() - startTime
        systemOutInterceptor.stop()

        evaluation.output = baos.toString('UTF8')
        evaluation
    }

    private static SystemOutputInterceptor createInterceptor(PrintStream out) {
        new SystemOutputInterceptor({ String s ->
            out.println s
            return false
        })
    }

    private Binding createBinding(request, PrintStream out) {
        new Binding([
            session          : request.session,
            request          : request,
            ctx              : grailsApplication.mainContext,
            grailsApplication: grailsApplication,
            config           : grailsApplication.config,
            log              : log,
            out              : out
        ])
    }

    private CompilerConfiguration createConfiguration(boolean autoImportDomains) {
        CompilerConfiguration configuration = new CompilerConfiguration()
        if (autoImportDomains) {
            ImportCustomizer importCustomizer = new ImportCustomizer()
            importCustomizer.addImports(*grailsApplication.domainClasses*.fullName)
            configuration.addCompilationCustomizers importCustomizer
        }
        configuration
    }

	 */
	
	
	/*
Context: {
      Domain: "dcraft.com",
      Origin: "http:[ipaddress]",
      Chronology: "/America/Chicago",
      Locale: "en-US",  
      UserId: "119",
      Username: "awhite",
      FullName: "Andy White",
      Email: "andy.white@dcraft.com",
      AuthToken: "010A0D0502",
      Credentials: {
         Username: "nnnn",
         Password: "mmmm"
      }
}

	 
Context: {
      Domain: "dcraft.com",
      Origin: "http:[ipaddress]",
      Chronology: "/America/Chicago",
      Locale: "en-US"
}

	 */
	
	public String getId() {
		return this.id;
	}
	
	public HashMap<String, Struct> getCache() {
		return this.cache;
	}

	public String getKey() {
		return this.key;
	}
	
	/**
	 * @return logging level to use with this session (and all sub tasks)
	 */
	public DebugLevel getLevel() {
		return this.level;
	}
	
	/**
	 * @param v logging level to use with this session (and all sub tasks)
	 */
	public Session withLevel(DebugLevel v) {
		this.level = v;
		return this;
	}
	
	public boolean isKeep() {
		return this.keep;
	}
	
	public Session withKeep(boolean v) {
		this.keep = v;
		return this;
	}

	public UserContext getUser() {
		return this.user;
	}
	
	public String checkTokenUpdate() {
		// if there is any token, replace it with null and send the response
		return this.authtokenupdate.getAndSet(null);
	}

	public Session withAdatper(ISessionAdapter v) {
		this.adapter = v;
		return this;
	}
	
	public ISessionAdapter getAdapter() {
		return this.adapter;
	}
	
	public Session withOriginalOrigin(String v) {
		this.originalOrigin = v;
		return this;
	}
	
	protected Session(String id, String key) {
		this.id = id;
		this.key = key;		
		this.level = HubLog.getGlobalLevel();
		this.originalOrigin = "hub:";
		this.lastAccess = System.currentTimeMillis();
	}
	
	protected Session() {
		this.id = OperationContext.getHubId() + "_" + Session.nextSessionId();
		this.key = StringUtil.buildSecurityCode();		// TODO switch to crypto secure
		this.level = HubLog.getGlobalLevel();
		this.originalOrigin = "hub:";
		this.lastAccess = System.currentTimeMillis();
	}
	
	// used for changing user context
	public Session withUser(UserContext user) {
		String oldtoken = (this.user != null) ? this.user.getAuthToken() : null;
		
		this.user = user;
		
		String newtoken = this.user.getAuthToken();
		
		if (! Objects.equals(oldtoken, newtoken))
			this.authtokenupdate.set(newtoken);
		
		if (this.adapter != null)
			this.adapter.UserChanged(Session.this.user);
		
		return this;
	}
	
	public void addKnownToken(String token) {
		if (StringUtil.isNotEmpty(token))
			this.pastauthtokens.add(token);
	}
	
	public boolean isKnownAuthToken(String token) {
		if (token == null)
			return true;
		
		String currtoken = this.user.getAuthToken();

		if (Objects.equals(token, currtoken))
			return true;
		
		for (String pasttoken : this.pastauthtokens)
			if (Objects.equals(token, pasttoken))
				return true;
		
		return false;
	}
	
	public OperationContextBuilder allocateContextBuilder() {
		return new OperationContextBuilder()
			.withOrigin(this.originalOrigin)
			.withDebugLevel(this.level)
			.withSessionId(this.id);
	}
	
	public OperationContext allocateContext(OperationContextBuilder tcb) {
		return OperationContext.allocate(this.user, tcb); 
	}	
	
	public OperationContext useContext() {
		return this.useContext(this.allocateContextBuilder());
	}	
	
	public OperationContext useContext(OperationContextBuilder tcb) {
		OperationContext oc = OperationContext.allocate(this.user, tcb); 
		OperationContext.set(oc);
		return oc;
	}	
	
	/*
	public Session(OperationContextBuilder usrctx) {
		this.id = OperationContext.getHubId() + "_" + Session.nextSessionId();
		this.key = StringUtil.buildSecurityCode();		// TODO switch to crypto secure
		this.level = HubLog.getGlobalLevel();
		this.user = UserContext.allocate(usrctx);
		this.originalOrigin = "hub:";
		
		this.touch();
	}
	
	public Session(String origin, String domainid, String site, String token) {
		this(new OperationContextBuilder().withGuestUserTemplate().withTenantId(domainid).withSite(site).withAuthToken(token).withVerified(StringUtil.isEmpty(token)));
		
		this.originalOrigin = origin;
	}
	
	public Session(OperationContext ctx) {
		this(ctx.getUserContext().toBuilder());
		
		this.level = ctx.getLevel();		
		this.originalOrigin = ctx.getOrigin();
	}
	*/
	
	public void touch() {
		this.lastAccess = System.currentTimeMillis();

		// keep auth token alive by pinging it at least once every hour - TODO configure
		if ((this.lastAccess - this.lastReauthAccess > (60 * 60000)) && this.user.isAuthenticated()) {
			OperationContext curr = OperationContext.get();
			
			try {
				// be sure to send the message with the correct context
				this.useContext();
				
				this.verifySession(new FuncCallback<Message>() {				
					@Override
					public void callback() {
						// TODO communicate to session initiator that our context has changed
					}
				});
			}
			finally {
				OperationContext.set(curr);
			}
			
			// keep this up to date whether we are gateway or not, this way fewer checks
			this.lastReauthAccess = this.lastAccess;
		}
	}
	
	public void end() {
		//System.out.println("collab session ended: " + this.collabId);
		
		// TODO consider clearing adapter and reply handler too
		
		Logger.info("Ending session: " + this.id);
	}
	
	public TaskRun submitTask(Task task, IOperationObserver... observers) {
		TaskRun run = new TaskRun(task);
		
		if (task == null) {
			run.errorTr(213, "info");
			return run;
		}
		
		// ensure we have an id
		run.prep();
		
		final String id = task.getId();
		
		// the submitted task will now report as owned by this session - if it isn't already
		String sid = task.getContext().getSessionId();
		
		if (!this.id.equals(sid))
			task.withContext(task.getContext().toBuilder().withSessionId(this.id).toOperationContext());
		
		this.tasks.put(id, run);
		
		for (IOperationObserver observer: observers)
			task.withObserver(observer);
		
		task.withObserver(new OperationObserver() {			
			@Override
			public void completed(OperationContext or) {
				// TODO review that this is working correctly and does not consume memory
				// otherwise TaskRun complete can lookup session and remove via there - might be better
				Session.this.tasks.remove(id);
			}
		});
		
		Hub.instance.getWorkPool().submit(run);
		
		return run;
	}

	// collect all tasks, filter by tags if any
	public void collectTasks(List<TaskRun> bucket, String... tags) {
		for (TaskRun task : this.tasks.values()) 
			if ((tags.length == 0) || task.getTask().isTagged(tags))
				bucket.add(task);
	}

	public void countTags(Map<String, Long> tagcount) {
		for (TaskRun task : this.tasks.values()) {
			ListStruct tags = task.getTask().getTags();
			
			if ((tags == null) || (tags.getSize() == 0)) {
				long cnt = tagcount.containsKey("[none]") ? tagcount.get("[none]") : 0;
				
				cnt++;
				
				tagcount.put("[none]", cnt);
			}
			else {
				for (Struct stag : tags.getItems()) {
					String tag = stag.toString();
					
					long cnt = tagcount.containsKey(tag) ? tagcount.get(tag) : 0;
					
					cnt++;
					
					tagcount.put(tag, cnt);
				}
			}
		}
	}

	// count all tasks, filter by tags if any
	public int countTasks(String... tags) {
		int num = 0;
		
		for (TaskRun task : this.tasks.values()) 
			if ((tags.length == 0) || task.getTask().isTagged(tags))
				num++;
		
		return num;
	}

	// count all tasks, filter by tags if any
	public int countIncompleteTasks(String... tags) {
		int num = 0;
		
		for (TaskRun task : this.tasks.values()) 
			if (!task.isComplete() && ((tags.length == 0) || task.getTask().isTagged(tags)))
				num++;
		
		return num;
	}
	
	public RecordStruct toStatusReport() {
		RecordStruct rec = new RecordStruct();
		
		rec.setField("Id", this.id);
		rec.setField("Key", this.key);
		
		if (this.lastAccess != 0)
			rec.setField("LastAccess", TimeUtil.stampFmt.print(this.lastAccess));
		
		if (this.user != null)
			rec.setField("UserContext", this.user.freezeToRecord());
		
		if (this.level != null)
			rec.setField("DebugLevel", this.level.toString());
		
		if (StringUtil.isNotEmpty(this.originalOrigin))
			rec.setField("Origin", this.originalOrigin);
		
		rec.setField("Keep", this.keep);
		
		ListStruct tasks = new ListStruct();
		
		for (TaskRun t : this.tasks.values())
			tasks.addItem(t.toStatusReport());
		
		rec.setField("Tasks", tasks);
		
		return rec;
	}
	
	public void deliver(Message msg) {
		// session activity, don't time out
		this.touch();
		
		String serv = msg.getFieldAsString("Service"); 
		String feat = msg.getFieldAsString("Feature"); 
		String op = msg.getFieldAsString("Op"); 
		
		if ("Replies".equals(serv)) {
			if ("Reply".equals(feat)) {
				if ("Deliver".equals(op)) {
					String tag = msg.getFieldAsString("Tag");
					
					SendWaitInfo info = this.sendwaits.remove(tag);
					
					// if null fall through to adapter
					if (info == null) {
						//if (!"SendForget".equals(tag))
						//	OperationContext.get().error("Missing reply handler for tag: " + tag);
					}
					else {
						// restore original respond tag
						msg.setField("RespondTag", info.respondtag);
						
						info.callback.setReply(msg);
						info.callback.complete();
						
						return;
					}
				}
			}
		}
		
		if (this.adapter != null)
			this.adapter.deliver(msg);
	}
	
	// only allowed to be called on local session replies, not for external use because of threading
	protected void reply(Message rmsg, Message msg) {
		// put the reply on a new thread because of how LocalSession will build up a large call stack
		// if threads don't change 
    	rmsg.setField("Service", "Replies");  // msg.getFieldAsString("RespondTo"));
    	rmsg.setField("Feature", "Reply");
    	rmsg.setField("Op", "Deliver");
    	
		String tag = msg.getFieldAsString("RespondTag");

		// should always have a tag if got here
		if (StringUtil.isNotEmpty(tag)) {
			// pull session id out of the tag
			int pos = tag.indexOf('_', 30);
			
			if (pos != -1)
				tag = tag.substring(pos + 1);
			
			// strip out session id, restore original tag
			rmsg.setField("Tag", tag);
			
			Hub.instance.getWorkPool().submit(new ISynchronousWork() {
				@Override
				public void run(TaskRun run) {
			    	Session.this.deliver(rmsg);
				}
			});
		}
	}
	
	/*
	 * Typically called by Hyper RPC
	 * 
	 *	 we don't need a time out, it is up to the client to timeout
	 * 
	 * @param msg
	 * @param serviceResult
	 */
	public void sendMessageWait(Message msg, ServiceResult serviceResult) {
		SendWaitInfo swi = new SendWaitInfo();
		swi.original = msg;
		swi.callback = serviceResult;
		swi.respondtag = msg.getFieldAsString("RespondTag");

		msg.setField("RespondTag", swi.id);
		
		this.sendwaits.put(swi.id, swi);
		
		this.sendMessage(msg);
	}

	/*
	 * Typically called by Web and Common RPC
	 * 
	 * @param msg
	 */
	public void sendMessage(Message msg) {
		// be sure we are using a proper context
		if (!OperationContext.hasContext()) 
			this.useContext();
		
		// note that session has been used
		this.touch();		
		
		// update the credentials if present in message 
		if (msg.hasField("Credentials")) {
			// we don't want the creds in the message root on the bus - because they should
			// travel as part of the context with the message
			RecordStruct newcreds = msg.getFieldAsRecord("Credentials");
			msg.removeField("Credentials");
			
			// only do something with the creds if no AuthToken is present
			if (StringUtil.isEmpty(this.user.getAuthToken())) {
				if (this.adapter != null) {
					String ckey = this.adapter.getClientKey();
					
					if (StringUtil.isNotEmpty(ckey))
						newcreds.setField("ClientKeyPrint", ckey);
					else
						newcreds.removeField("ClientKeyPrint");
				}
				else {
					newcreds.removeField("ClientKeyPrint");
				}
				
				// if the sent credentials are different from those already in context then change
				// (set checks if different)
				OperationContextBuilder umod = UserContext.checkAddCredentials(this.user, newcreds);
	
				// credentials have changed, change local context but not Session - session should not have creds in it
				// and it will get updated by any 
				if (umod != null) 
					OperationContext.switchUser(OperationContext.get(), umod.toUserContext());
			}
		}
		
		// not valid outside of RPC calls
		// msg.removeField("Session");  NOT valid at all?
		
		String service = msg.getFieldAsString("Service");
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");

		// user requests a new session directly
		if ("Session".equals(service)) {
			// user requests end to session
			if ("Control".equals(feature)) {
				if ("Start".equals(op)) {
					this.verifySession(new FuncCallback<Message>() {				
						@Override
						public void callback() {
							Message rmsg = this.getResult();
							
							// TODO review how this is used/give less info to caller by default
							RecordStruct body = new RecordStruct();							
							rmsg.setField("Body", body);
							
							Session.this.user.freezeRpc(body);
							
							body.setField("SessionId", Session.this.id);		
							// web server will not return this
							//body.setField("SessionKey", Session.this.key);		
							
							Session.this.reply(rmsg, msg);
						}
					});
					
					return;
				}
				else if ("Stop".equals(op)) {
					if (this.user.isAuthenticated()) {
						Message msg2 = new Message("dcAuth", "Authentication", "SignOut");
						
						Hub.instance.getBus().sendMessage(msg2, new ServiceResult() {
							@Override
							public void callback() {
								Hub.instance.getSessions().terminate(Session.this.id);
							}
						});
					}
					else {
						Hub.instance.getSessions().terminate(this.id);
					}
					
					Session.this.reply(MessageUtil.success(), msg);
					
					return;
				}
				else if ("Touch".equals(op)) {
					Session.this.reply(MessageUtil.success(), msg);
					return;
				}
				else if ("SetDebugLevel".equals(op)) {
					// session user may alter the debug level for the session, but only if debugging is enabled on system
					if (HubLog.getDebugEnabled()) {
						RecordStruct rec = msg.getFieldAsRecord("Body");
						String level = rec.getFieldAsString("Level");
						
						try {
							this.level = DebugLevel.parse(level);
							Session.this.reply(MessageUtil.success(), msg);
						}
						catch (Exception x) {
							Session.this.reply(MessageUtil.error(1, "Failed to set level"), msg);
						}
					}
					
					return;
				}
				else if ("LoadUser".equals(op)) {
					Message rmsg = new Message();
					
					// TODO review how this is used/give less info to caller by default
					RecordStruct body = new RecordStruct();							
					rmsg.setField("Body", body);
					
					Session.this.user.freezeRpc(body);
					
					body.setField("SessionId", Session.this.id);		
					//body.setField("SessionKey", Session.this.key);				// TODO remove this, use only the HTTPONLY cookie for key - resolve for Java level clients
					
					Session.this.reply(rmsg, msg);
					
					return;
				}
				else if ("ReloadUser".equals(op)) {
					this.verifySession(new FuncCallback<Message>() {				
						@Override
						public void callback() {
							Message rmsg = this.getResult();
							
							if (rmsg.hasErrors()) {
								Session.this.reply(rmsg, msg);
								return;
							}
							
							rmsg = new Message();
							
							RecordStruct body = new RecordStruct();							
							rmsg.setField("Body", body);
							
							Session.this.user.freezeRpc(body);
							
							body.setField("SessionId", Session.this.id);		
							
							Session.this.reply(rmsg, msg);
						}
					});
					
					return;
				}
			}
		}
		
		// if the caller skips Session Start that is fine - but if they pass creds we verify anyway before processing the message 		
		if (!this.user.isVerified()) {
			this.verifySession(new FuncCallback<Message>() {				
				@Override
				public void callback() {
					Message rmsg = this.getResult();
					
					if (rmsg.hasErrors())
						Session.this.reply(rmsg, msg);
					else
						Session.this.sendMessageThru(msg);
				}
			});
		}
		else {
			Session.this.sendMessageThru(msg);
		}
	}

	// if session has an unverified user, verify it
	public void verifySession(FuncCallback<Message> cb) {
		boolean waslikeguest = this.user.looksLikeGuest();
		
		OperationContext tc = OperationContext.get();
		
		boolean nv = tc.needVerify();
		
		if (nv)
			System.out.println("NOVHOATS before verify: " + waslikeguest);
		
		tc.verify(new FuncCallback<UserContext>() {				
			@Override
			public void callback() {
				UserContext uc = this.getResult();
				
				if (uc != null) {
					boolean nowlikeguest = uc.looksLikeGuest();
					
					if (nowlikeguest && ! waslikeguest)
						cb.error(1, "User not authenticated!");
				}
				
				if (nv)
					System.out.println("NOVHOATS after verify: ");
				
				cb.setResult(this.toLogMessage());
				cb.complete();
			}
		});
	}
	
	private void sendMessageThru(Message msg) {		
		// TODO make sure the message has been validated by now
		
		String service = msg.getFieldAsString("Service");
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		if ("Session".equals(service)) {
			if ("Control".equals(feature)) {
				if ("CheckInBox".equals(op)) {
					Message reply = MessageUtil.success();
					
					if (this.adapter != null) 
						reply.setField("Body", this.adapter.popMessages());
					
					Session.this.reply(reply, msg);					
					return;
				}
				
				if ("CheckJob".equals(op)) {
					RecordStruct rec = msg.getFieldAsRecord("Body");
					Long jid = rec.getFieldAsInteger("JobId");
					
					TaskRun info = this.tasks.get(jid);
					
					if (info != null) {
						Struct res = info.getResult();
						Message reply = info.toLogMessage();
						
						reply.setField("Body",
								new RecordStruct(
										new FieldStruct("AmountCompleted", info.getContext().getAmountCompleted()),
										new FieldStruct("Steps", info.getContext().getSteps()),
										new FieldStruct("CurrentStep", info.getContext().getCurrentStep()),
										new FieldStruct("CurrentStepName", info.getContext().getCurrentStepName()),
										new FieldStruct("ProgressMessage", info.getContext().getProgressMessage()),
										new FieldStruct("Result", res)
								)
						);
						
						Session.this.reply(reply, msg);
					}
					else {
						Message reply = MessageUtil.error(1, "Job Not Found");		// TODO
						Session.this.reply(reply, msg);
					}
					
					return;
				}
				
				if ("ClearJob".equals(op)) {
					RecordStruct rec = msg.getFieldAsRecord("Body");
					Long jid = rec.getFieldAsInteger("JobId");
					
					this.tasks.remove(jid);
					
					Session.this.reply(MessageUtil.success(), msg);
					return;
				}
				
				if ("KillJob".equals(op)) {
					RecordStruct rec = msg.getFieldAsRecord("Body");
					Long jid = rec.getFieldAsInteger("JobId");
					
					// get not remove, because kill should do the remove and we let it do it in the natural way
					TaskRun info = this.tasks.get(jid);
					
					if (info != null) 
						info.kill();
					
					Session.this.reply(MessageUtil.success(), msg);
					return;
				}

				/* TODO someday support an interactive groovy shell via any session, assuming SysAdmin access and system wide setting
        Binding b = new Binding();
        b.setVariable("x", 1);
        b.setVariable("y", 2);
        b.setVariable("z", 3);
        GroovyShell sh = new GroovyShell(b);
        
        sh.evaluate("print z");
        sh.evaluate("d = 1");
        sh.evaluate("print d");
        
        sh.evaluate("println dcraft.util.HashUtil.getMd5('abcxyz')");
        
        sh.evaluate("import dcraft.util.HashUtil");
        sh.evaluate("println HashUtil.getMd5('abcxyz')");
				 * 
				 * 
				 * consider
				 * 	http://mrhaki.blogspot.co.uk/2011/06/groovy-goodness-add-imports.html
				 * 
				 * 
				 */
				
			}
			else if ("DataChannel".equals(feature)) {
				this.dataChannel(msg);
				return;
			}
		}
		
		this.sendMessageIn(msg);
	}
	
	private void sendMessageIn(final Message msg) {		
		// so that responses come to the Sessions service
		// the id will be stripped off before delivery to client
		String resptag = msg.getFieldAsString("RespondTag");
		
		if (StringUtil.isNotEmpty(resptag)) {
			msg.setField("RespondTag", this.id + "_" + resptag);		
			msg.setField("RespondTo", "Session");
		}
		
		/*
		System.out.println("------------");
		
		System.out.println("elevated: " + tc.isElevated());
		System.out.println("user: " + tc.getUserContext());
		System.out.println("message: " + msg);
		
		System.out.println("------------");
		*/
		
		OperationResult smor = Hub.instance.getBus().sendMessage(msg);
		
		if (smor.hasErrors())
			Session.this.reply(smor.toLogMessage(), msg);		
	}

	public void addChannel(DataStreamChannel v) {
		this.channellock.lock();
		
		try {
			this.channels.put(v.getId(), v);
		}
		finally {
			this.channellock.unlock();
		}
	}
	
	public DataStreamChannel getChannel(String id) {
		return this.channels.get(id);
	}
	
	public void removeChannel(String id) {
		this.channellock.lock();
		
		try {
			this.channels.remove(id);
		}
		finally {
			this.channellock.unlock();
		}
	}

	public void dataChannel(final Message msg) {
		String op = msg.getFieldAsString("Op");
		
		if ("Establish".equals(op)) {
			DataStreamChannel chan = new DataStreamChannel(this.getId(), msg.getFieldAsRecord("Body").getFieldAsString("Title"));
			
			RecordStruct sr = msg.getFieldAsRecord("Body").getFieldAsRecord("StreamRequest");
			
			RecordStruct srb = sr.getFieldAsRecord("Body");
			
			if (srb == null) {
				Session.this.reply(MessageUtil.error(0, "Missing StreamRequest Body"), msg);
				return;
			}
			
			// add to the existing fields - which might typically be "FilePath" or "Token"
			srb.setField("Channel", chan.getId());
			
			Message srmsg = MessageUtil.fromRecord(sr);

			Hub.instance.getBus().sendMessage(srmsg, res -> {				
				if (res.hasErrors()) { 
					res.error(1, "Start Upload error: " + res.getMessage());
					
					Session.this.reply(res.toLogMessage(), msg);
					return;
				}

				RecordStruct srrec = res.getBodyAsRec();
				
				if (srrec == null) { 
					Session.this.reply(MessageUtil.error(1, "Start Upload error: Missing StreamRequest response"), msg);
					return;
				}
					
				Session.this.addChannel(chan);
				chan.setBinding((RecordStruct) srrec.deepCopy());
				
				// protect from client view
				srrec.removeField("Hub");
				srrec.removeField("Session");
				srrec.removeField("Channel");
				
				// include the client end of the channel
				srrec.setField("ChannelId", chan.getId());
				
				Session.this.reply(MessageUtil.success(srrec), msg);
			}); 
			
			return;
		}
		
		if ("Free".equals(op)) {
			String chid = msg.getFieldAsRecord("Body").getFieldAsString("ChannelId");
			this.removeChannel(chid);
			
			Session.this.reply(MessageUtil.success(), msg);
			return;
		}

		/*
		if ("Allocate".equals(op)) {
			DataStreamChannel chan = new DataStreamChannel(this.getId(), msg.getFieldAsRecord("Body").getFieldAsString("Title"));
			
			this.addChannel(chan);
			
			Session.this.reply(MessageUtil.success("ChannelId", chan.getId()), msg);
			return;
		}
		
		if ("Bind".equals(op)) {
			RecordStruct rec = msg.getFieldAsRecord("Body");
			String chid = rec.getFieldAsString("ChannelId");
			
			DataStreamChannel chan = this.getChannel(chid);
			
			if (chan == null) {
				Session.this.reply(MessageUtil.error(1, "Missing channel"), msg);
				return;
			}
			
			chan.setBinding(rec);
			
			// TODO tell the channel it is a dest or src
			
			Session.this.reply(MessageUtil.success(), msg);
			return;
		}
		*/
		
		Session.this.reply(MessageUtil.errorTr(441, "Session", "DataChannel", op), msg);
	}

	public void clearToGuest() {
		this.withUser(new OperationContextBuilder()
			.withGuestUserTemplate()
			.withTenantId(this.user.getTenantId())
			.withSite(this.user.getSiteAlias())
			.toUserContext());
	}

	public boolean reviewPlan(long clearGuest, long clearUser) {
		// TODO add plans into mix - check both tasks and channels for completeness (terminate only on complete, vs on timeout, vs never)
		// TODO add session plan features
		
		// review get called often - optimize so that as few objects as possible are 
		// created each time this is called
		if (this.channels.size() > 0) {
			// cleannup expired channels
			List<DataStreamChannel> killlist = null;
			
			this.channellock.lock();
			
			try {
				for (DataStreamChannel chan : this.channels.values()) {
					if (chan.isHung()) {
						if (killlist == null)
							killlist = new ArrayList<>();
						
						killlist.add(chan);
					}
				}
			}
			finally {
				this.channellock.unlock();
			}
			
			if (killlist != null) {
				for (DataStreamChannel chan : killlist) {
					Logger.warn("Session " + this.id + " found hung transfer: " + chan);
					chan.abort();
				}		
			}
		}
		
		if (this.sendwaits.size() > 0) {
			// cleannup expired channels
			List<SendWaitInfo> killlist = null;
			
			this.channellock.lock();
			
			try {
				for (SendWaitInfo chan : this.sendwaits.values()) {
					if (chan.isHung()) {
						if (killlist == null)
							killlist = new ArrayList<>();
						
						killlist.add(chan);
					}
				}
			}
			finally {
				this.channellock.unlock();
			}
			
			if (killlist != null) {
				for (SendWaitInfo chan : killlist) {
					Logger.warn("Session " + this.id + " found hung send wait: " + chan);
					this.sendwaits.remove(chan.id);
				}		
			}
		}
		
		if (this.isLongRunning())
			return ((this.lastAccess > clearUser) || this.keep); 
		
		return ((this.lastAccess > clearGuest) || this.keep); 
	}
	
	// user sessions can be idle for a longer time (3 minutes default) than guest sessions (75 seconds default)
	// why? because we need to keep tethered sessions going and we only want to send message once every minute
	// and it might take user 1 minute to update their session, so to be sure to keep everything in line
	// requires > 2 minutes
	public boolean isLongRunning() {
		return this.user.isTagged("User");
	}

	public Collection<DataStreamChannel> channels() {
		return this.channels.values();
	}
	
	public CtpAdapter allocateCtpAdapter() {
		return new CtpAdapter(this.allocateContext(this.allocateContextBuilder()));
	}
	
	public class SendWaitInfo {
		protected String id = StringUtil.buildSecurityCode();  // Session.nextUUId();
		protected ServiceResult callback = null;
		protected Message original = null;
		protected String respondtag = null;
		protected long started = System.currentTimeMillis();
		
		// give two minutes
		protected boolean isHung() {
			return (this.started < (System.currentTimeMillis() - 120000));
		}
 	}
}
