package dcraft.cms.thread.proc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.struct.builder.ICompositeBuilder;

public class FolderCounting implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		// TODO replicating
		// if (task.isReplicating()) 

		TablesAdapter db = new TablesAdapter(conn, task); 

		/* TODO use dcmThreadA or dcmThreadB 	 */
		
		BigDateTime when = BigDateTime.nowDateTime();
		boolean historical = false;
		ICompositeBuilder out = task.getBuilder();
		AtomicReference<String> currparty = new AtomicReference<>();
		Map<String, FolderCount> currdata = new HashMap<>(); 
		
		try {
			Function<Object,Boolean> partyConsumer = new Function<Object,Boolean>() {				
				@Override
				public Boolean apply(Object t) {
					try {
						String id = t.toString();						
						String party = currparty.get();

						// TODO filter labels 
						
						String foldr = (String) db.getStaticList("dcmThread", id, "dcmFolder", party);
						
						FolderCount fd = currdata.get(foldr);
						
						if (fd == null) {
							fd = new FolderCount();
							fd.name = foldr;
							currdata.put(foldr, fd);
						}
						
						Boolean read = (Boolean) db.getStaticList("dcmThread", id, "dcmRead", party);
						
						if ((read == null) || !read)
							fd.newcnt++;
						
						fd.totalcnt++;
						
						return true;
					}
					catch (Exception x) {
						log.error("Issue with folder counting: " + x);
					}
					
					return false;
				}
			};				
			
			ListStruct values = params.getFieldAsList("FilterParties");
			
			out.startList();
			
			for (Struct s : values.getItems()) {
				currparty.set(s.toString());
				currdata.clear();
				
				// collect data for this party
				db.traverseIndex("dcmThread", "dcmParty", currparty.get(), when, historical, partyConsumer);
				
				//output data for this party
				out.startRecord();
				out.field("Party", currparty.get());
				out.field("Folders");
				out.startList();
				
				for (FolderCount cnt : currdata.values()) {
					out.startRecord();
					out.field("Name", cnt.name);
					out.field("New", cnt.newcnt);
					out.field("Total", cnt.totalcnt);
					out.field("Labels");
					
					// TODO split and output labels
					out.startList();
					out.endList();
					
					out.endRecord();
				}
				
				out.endList();
				
				out.endRecord();						
			}
			
			out.endList();
		}
		catch (Exception x) {
			log.error("Issue with folder counting: " + x);
		}
		
		task.complete();
	}
	
	public class FolderCount {
		public String name = null;
		public int newcnt = 0;
		public int totalcnt = 0;
		public String Labels = null;
	}
}
