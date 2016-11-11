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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.count.CountManager;
import dcraft.hub.Hub;
import dcraft.hub.ISystemWork;
import dcraft.hub.SiteInfo;
import dcraft.hub.SysReporter;
import dcraft.lang.op.OperationContextBuilder;
import dcraft.lang.op.OperationResult;
import dcraft.log.Logger;
import dcraft.struct.RecordStruct;
import dcraft.util.StringUtil;
import dcraft.work.TaskRun;
import dcraft.xml.XElement;

public class Sessions implements IService {
	protected ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<String, Session>();
	//protected List<String> termsessions = new ArrayList<>();

	public String serviceName() {
		return "Session";
	}	
	
	public Collection<Session> list() {
		return this.sessions.values();
	}
	
	public void init(OperationResult or, XElement config) {
		ISystemWork sessioncleanup = new ISystemWork() {
			@Override
			public void run(SysReporter reporter) {
				reporter.setStatus("Reviewing session plans");
				
				if (!Hub.instance.isStopping()) {
					// guest sessions only last 1 minute, users 5 minutes
					long clearGuest = System.currentTimeMillis() - (75 * 1000);		// TODO configure - 1 minute, 15 secs
					long clearUser = System.currentTimeMillis() - (195 * 1000);		// TODO config - 3 minutes, 15 secs 
					
					for (Session sess : Sessions.this.sessions.values()) {
						if (!sess.reviewPlan(clearGuest, clearUser)) {
							Sessions.this.sessions.remove(sess.getId());
							
							Logger.info("Killing inactive session: " + sess.getId());
							
							sess.end();
						}
					}
				}
				
				reporter.setStatus("After reviewing session plans");
			}

			@Override
			public int period() {
				return 60;	// TODO configure?
			}
		};
		
		Hub.instance.getClock().addSlowSystemWorker(sessioncleanup);	
	}
		
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		//String op = msg.getFieldAsString("Op");
		
		if ("Manager".equals(feature)) {
			/* TODO probably a worthwhile concept, but review
			RecordStruct req = msg.getFieldAsRecord("Body");
			
			if ("Start".equals(op)) {
				Session s = new Session(OperationContext.get());
				this.sessions.put(s.getId(), s);
				//return s.call(req.getFieldAsList("Batch"));
				request.complete();
				return;
			}
			else if ("End".equals(op)) {
				Session s = this.sessions.get(req.getFieldAsString("Id"));
				
				if ((s != null) && s.getKey().equals(req.getFieldAsString("AccessCode"))) 
					this.terminate(s.getId());
				else
					request.error(1, "Unable to end session, missing Id or Code or session already terminated.");	
				
				request.complete();
				return;
			}
			else if ("Touch".equals(op)) {
				Session s = this.sessions.get(req.getFieldAsString("Id"));
				
				if (s != null) 
					s.touch();
				else
					request.error(1, "Unable to touch session, missing Id or Code or session terminated.");	
				
				request.complete();
				return;
			}
			*/
		}
		else if ("Session".equals(feature)) {
			RecordStruct req = msg.getFieldAsRecord("Body");
			
			if (!req.isFieldEmpty("Id") && !req.isFieldEmpty("AccessCode")) {
				Session s = this.sessions.get(req.getFieldAsString("Id"));
				
				if ((s != null) && s.getKey().equals(req.getFieldAsString("AccessCode"))) 
					//return s.call(req.getFieldAsList("Batch"));
					request.complete();
					return;
			}
			
			request.error(1, "Unable to use session, missing Id or Code or session already terminated.");		// TODO better codes
			request.complete();
			return;
		}
		else if ("Reply".equals(feature)) {
			String tag = msg.getFieldAsString("Tag");
			
			// pull session id out of the tag
			int pos = tag.indexOf('_', 30);
			String sessionid = tag.substring(0, pos);	
			tag = tag.substring(pos + 1);
			
			// strip out session id, restore original tag
			msg.setField("Tag", tag);
			msg.setField("Service", "Replies");
			
			Session s = this.sessions.get(sessionid);
			
			if (s == null) {
				request.error("Missing session");
				request.complete();
				return;
			}
			
			// if we get this far consider it delivered - as a far as we know anyway
			request.complete();
			
			s.deliver(msg);
			
			return;
		}
		/* TODO probably out of date 
		else if ("InBox".equals(feature)) {
			if ("Deliver".equals(op)) {
				String tag = msg.getFieldAsString("Tag");
				
				// pull session id out of the tag
				int pos = tag.indexOf('_', 30);
				
				if (pos == -1)
					return MessageUtil.errorTr(451, tag);
				
				String sessionid = tag.substring(0, pos);	
				
				Session s = this.sessions.get(sessionid);
				
				if (s == null)
					return MessageUtil.errorTr(450, sessionid);
				
				tag = tag.substring(pos + 1);
				
				// pull destination out of the tag
				pos = tag.indexOf('_');
				
				if (pos == -1) {
					msg.setField("Service", tag);
					msg.removeField("Feature");
					msg.removeField("Op");
					msg.removeField("Tag");
				}
				else {
					msg.setField("Service", tag.substring(0, pos));
					tag = tag.substring(pos + 1);
					
					pos = tag.indexOf('_');
					
					if (pos == -1) {
						msg.setField("Feature", tag);
						msg.removeField("Op");
						msg.removeField("Tag");
					}
					else {
						msg.setField("Feature", tag.substring(0, pos));
						tag = tag.substring(pos + 1);
						
						pos = tag.indexOf('_');
						
						if (pos == -1) {
							msg.setField("Op", tag);
							msg.removeField("Tag");
						}
						else {
							msg.setField("Op", tag.substring(0, pos));
							tag = tag.substring(pos + 1);
							
							// restore original tag
							msg.setField("Tag", tag);
						}
					}
				}
				
				s.deliver(msg);
				
				// TODO get some results from delivery attempt
			    Hub.instance.getBus().sendReply(MessageUtil.success(), msg, "Sessions", "InBox", "Deliver");
				return null;
			}
		}
		*/
		
		// Enlist		
		
		request.error(1, "Sessions does not support this feature or operation.");		// TODO better codes
		request.complete();
	}

	public Session lookup(String sessionid) {
		if (StringUtil.isEmpty(sessionid))
			return null;
		
		return this.sessions.get(sessionid);
	}
	
	public Session restore(SiteInfo site, String origin, String id, String key, String authtoken) {
		Session s = new Session(id, key)
				.withOriginalOrigin(origin)
				.withUser(new OperationContextBuilder()
					.withGuestUserTemplate()
					.withTenantId(site.getTenant().getId())
					.withSite(site.getAlias())
					.withAuthToken(authtoken)
					.withVerified(StringUtil.isEmpty(authtoken))
					.toUserContext()
				);
			
			this.sessions.put(s.getId(), s);
			
			return s;
	}
	
	public Session create(SiteInfo site, String origin) {
		return this.create(site, origin, null);
	}
	
	public Session create(SiteInfo site, String origin, String authtoken) {
		return this.create(site.getTenant().getId(), site.getAlias(), origin, authtoken);
	}
	
	public Session create(String tenant, String site, String origin) {
		return this.create(Hub.instance.getTenants().resolveTenantId(tenant), site, origin, null);
	}
	
	public Session create(String tenantid, String site, String origin, String authtoken) {
		Session s = new Session()
			.withOriginalOrigin(origin)
			.withUser(new OperationContextBuilder()
				.withGuestUserTemplate()
				.withTenantId(tenantid)
				.withSite(site)
				.withAuthToken(authtoken)
				.withVerified(StringUtil.isEmpty(authtoken))
				.toUserContext()
			);
		
		this.sessions.put(s.getId(), s);
		
		return s;
	}

	// a root and elevated session that doesn't timeout
	public Session createForService() {
		return this.createForService("root", "root");
	}

	// a root and elevated session that doesn't timeout
	public Session createForService(String tenantid) {
		return this.createForService(tenantid, "root");
	}

	// a root and elevated session that doesn't timeout
	public Session createForService(String tenantid, String site) {
		Session s = new Session()
			.withKeep(true)
			.withUser(new OperationContextBuilder()
				.withRootUserTemplate()
				.withTenantId(tenantid)
				.withSite(site)
				.toUserContext()
			);
		
		this.sessions.put(s.getId(), s);
		return s;
	}

	/*
	// based on current context/user
	public Session createForContext(OperationContext ctx) {
		Session s = new Session(ctx);
		this.sessions.put(s.getId(), s);
		return s;
	}

	// based on current context/user
	public Session findOrCreate(OperationContext ctx) {
		String sid = ctx.getSessionId();
		
		Session s = this.sessions.get(sid);
		
		if (s == null) {
			s = new Session(ctx);
			s.id = ctx.getSessionId();			// we are not copying and do not have secret key...may need to review this in future
			this.sessions.put(s.getId(), s);
		}
		else {
			s.setUser(ctx.getUserContext());
		}
		
		return s;
	}
	
	// runs a single task and then terminates session
	public SessionTaskInfo createForSingleTaskAndDie(Task info) {		
		Session session = this.createForContext(info.getContext());
		
		if (session == null)
			return null;
		
		// this maybe overkill but setting up a single use reference to the session
		// for use with terminating session later - this is to help with GC
		// which is a causing issues so we want it to be as obvious as possible
		// that a session and related tasks and task contexts are free when done
		final AtomicReference<Session> sessref = new AtomicReference<>(); 
		
		sessref.set(session);
		
		OperationObserver listener = new OperationObserver() {			
			@Override
			public void completed(OperationContext or) {
				Session session = sessref.get();
				
				if (session != null) {
					Hub.instance.getSessions().terminate(session.id);
					sessref.set(null);
				}
			}
		};
		
		TaskRun run = session.submitTask(info, listener);
		
		// completion listener will be run even if we get here with errors
		// so no need to worry about terminate
		
		return new SessionTaskInfo(session, run);
	}
	*/

	public List<TaskRun> collectTasks(String... tags) {
		List<TaskRun> matches = new ArrayList<TaskRun>();
		
		for (Session sess : Sessions.this.sessions.values()) 
			sess.collectTasks(matches, tags);
		
		return matches;
	}
	
	public int countTasks(String... tags) {
		int num = 0;
		
		for (Session sess : Sessions.this.sessions.values()) 
			num += sess.countTasks(tags);
		
		return num;
	}
	
	public int countIncompleteTasks(String... tags) {
		int num = 0;
		
		for (Session sess : Sessions.this.sessions.values()) 
			num += sess.countIncompleteTasks(tags);
		
		return num;
	}

	public void terminate(String id) {
		if (StringUtil.isEmpty(id))
			return;

		Session s = this.sessions.remove(id);

		if (s != null)
			s.end();
	}

	public void recordCounters() {
		CountManager cm = Hub.instance.getCountManager();
		
		long totalKeepers = 0;
		long totalTasks = 0;
		long totalIncompleteTasks = 0;
		
		Collection<Session> lsessions = this.sessions.values();
		HashMap<String,Long> tagcount = new HashMap<>();
		
		for (Session sess : lsessions) {
			if (sess.isKeep())
				totalKeepers++;
			
			totalTasks += sess.countTasks();
			totalIncompleteTasks += sess.countIncompleteTasks();
			
			sess.countTags(tagcount);
		}
		
		cm.allocateSetNumberCounter("dcSessionCount", lsessions.size());
		cm.allocateSetNumberCounter("dcSessionKeepersCount", totalKeepers);
		cm.allocateSetNumberCounter("dcSessionTaskCount", totalTasks);
		cm.allocateSetNumberCounter("dcSessionTaskIncompleteCount", totalIncompleteTasks);
		
		for (Entry<String, Long> tagentity : tagcount.entrySet()) 
			cm.allocateSetNumberCounter("dcSessionTag_" + tagentity.getKey() + "_Count", tagentity.getValue());
	}
}
