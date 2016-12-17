package dcraft.cms.feed.proc;

import java.util.function.Function;

import org.joda.time.DateTime;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.ICollector;
import dcraft.db.util.ByteUtil;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;

public class FeedScan implements ICollector {
	@Override
	public void collect(DatabaseInterface conn, DatabaseTask task, OperationResult log, RecordStruct collector, Function<Object,Boolean> uniqueConsumer) {
		RecordStruct extras = collector.getFieldAsRecord("Extras");
		
		// TODO verify fields
		
		String chan = "/" + OperationContext.get().getSite().getAlias() + "/" + extras.getFieldAsString("Channel");
		DateTime fromdate = extras.getFieldAsDateTime("FromDate");
		Object lasttime = null;
		
		if (fromdate != null)
			lasttime = conn.inverseTime(fromdate);
		
		DateTime todate = extras.getFieldAsDateTime("ToDate");
		Long totime = null;
		
		if (todate != null)
			totime = conn.inverseTime(todate);
		
		boolean reverse = extras.getFieldAsBooleanOrFalse("Reverse");
		
		if ((fromdate != null) && (todate != null) && (todate.isBefore(fromdate)))
			reverse = true;
		
		Object lastid = extras.getFieldAsString("LastId");
		long max = extras.getFieldAsInteger("Max", 100);
		long cnt = 0;
		
		// TODO add Tags filter
		
		// TODO how to do the Preview index
		
		// TODO add site

		/*
		 * ^dcmFeedIndex(did, channel, publish datetime, id)=[content tags]
		 */
		
		String did = task.getTenant();
		
		try {
			if (reverse) {
				if (lasttime == null) 
					lasttime = ByteUtil.extractValue(conn.prevPeerKey("dcmFeedIndex", did, chan, null));
				
				while ((cnt < max) && (lasttime != null) && ((totime == null) || (totime.compareTo(((Number) lasttime).longValue()) < 0))) {
					lastid = ByteUtil.extractValue(conn.nextPeerKey("dcmFeedIndex", did, chan, lasttime, lastid));		// might return null
	
					// try the next publish time
					if (lastid == null) {
						lasttime = ByteUtil.extractValue(conn.prevPeerKey("dcmFeedIndex", did, chan, lasttime));
						
						continue;
					}
					
					// TODO check tags
					
					if (uniqueConsumer.apply(lastid))
						cnt++;
				}
			}
			else {
				if (lasttime == null) 
					lasttime = ByteUtil.extractValue(conn.nextPeerKey("dcmFeedIndex", did, chan, null));
				
				while ((cnt < max) && (lasttime != null) && ((totime == null) || (totime.compareTo(((Number) lasttime).longValue()) > 0))) {
					lastid = ByteUtil.extractValue(conn.nextPeerKey("dcmFeedIndex", did, chan, lasttime, lastid));		// might return null
	
					// try the next publish time
					if (lastid == null) {
						lasttime = ByteUtil.extractValue(conn.nextPeerKey("dcmFeedIndex", did, chan, lasttime));
						
						continue;
					}
					
					// TODO check tags
					
					if (uniqueConsumer.apply(lastid))
						cnt++;
				}
			}
		}
		catch (Exception x) {
			log.error("Error scanning feed index: " + x);
		}
	}
}
