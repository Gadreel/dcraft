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
package dcraft.schema;

import java.util.HashMap;

import dcraft.bus.Message;
import dcraft.util.ArrayUtil;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;

public class ServiceSchema {
	protected SchemaManager man = null;
	protected HashMap<String, Service> services = new HashMap<String, Service>();
	
	public ServiceSchema(SchemaManager man) {
		this.man = man;
	}	
	
	public void load(Schema schema, XElement db) {
		for (XElement serel : db.selectAll("Service")) {
			String sname = serel.getAttribute("Name");
			//String sclass = serel.getAttribute("Class");
			
			if (StringUtil.isEmpty(sname))
				continue;
			
			Service ser = this.services.get(sname);
			
			if (ser == null) {
				ser = new Service();
				ser.name = sname;
				this.services.put(sname, ser);
			}
			
			for (XElement secel : serel.selectAll("Secure")) {
				String[] tags = secel.hasAttribute("Tags") 
					? secel.getAttribute("Tags").split(",")
					: new String[] { "Guest", "User" };
				
				for (XElement ftel : secel.selectAll("Feature")) {
					String fname = ftel.getAttribute("Name");
					
					if (StringUtil.isEmpty(fname))
						continue;
					
					Feature fet = ser.features.get(fname); 
					
					if (fet == null) {
						fet = new Feature();
						fet.name = fname;
						ser.features.put(fname, fet);
					}
					
					for (XElement opel : ftel.selectAll("Op")) {
						String oname = opel.getAttribute("Name");
						
						if (StringUtil.isEmpty(oname))
							continue;
						
						Op opt = new Op();
						opt.name = oname;
						opt.securityTags = tags;
						
						fet.ops.put(oname, opt);
						
						XElement req = opel.find("Request", "RecRequest");
						
						if (req != null)
							opt.request = this.man.loadDataType(schema, req);
						
						XElement resp = opel.find("Response", "RecResponse");
						
						if (resp != null)
							opt.response = this.man.loadDataType(schema, resp);
					}			
					
					for (XElement opel : ftel.selectAll("OpMod")) {
						String oname = opel.getAttribute("Name");
						
						if (StringUtil.isEmpty(oname))
							continue;
						
						String[] curr = fet.opmods.get(oname);
						
						if (curr != null)
							curr = ArrayUtil.addAll(curr, tags);
						else
							curr = tags;
						
						fet.opmods.put(oname, curr);
					}			
				}			
			}			
		}			
		
		// TODO we need to have additional table info stored locally - but at moment at least we can validate against Services
		
	}

	public DataType getRequestType(Message msg) {
		Op opt = this.getOp(msg.getFieldAsString("Service"), msg.getFieldAsString("Feature"), msg.getFieldAsString("Op"));
		
		if (opt != null)
			return opt.request;
		
		return null;
	}

	public DataType getResponseType(String service, String feature, String op) {
		Op opt = this.getOp(service, feature, op);
		
		if (opt != null)
			return opt.response;
		
		return null;
	}
	
	public Op getOp(Message msg) {
		return this.getOp(msg.getFieldAsString("Service"), msg.getFieldAsString("Feature"), msg.getFieldAsString("Op"));
	}
	
	public Op getOp(String service, String feature, String op) {
		if (StringUtil.isEmpty(service))
			return null;
		
		Service s = this.services.get(service);
		
		if (s == null)
			return null;
		
		if (StringUtil.isEmpty(feature))
			feature  = "default";
		
		Feature f = s.features.get(feature);
		
		if (f == null)
			return null;
		
		if (StringUtil.isEmpty(op))
			op = "default";
		
		return f.ops.get(op);
	}
	
	public String[] getOpSecurity(Message msg) {
		return this.getOpSecurity(msg.getFieldAsString("Service"), msg.getFieldAsString("Feature"), msg.getFieldAsString("Op"));
	}
	
	public String[] getOpSecurity(String service, String feature, String op) {
		if (StringUtil.isEmpty(service))
			return null;
		
		Service s = this.services.get(service);
		
		if (s == null)
			return null;
		
		if (StringUtil.isEmpty(feature))
			feature  = "default";
		
		Feature f = s.features.get(feature);
		
		if (f == null)
			return null;
		
		if (StringUtil.isEmpty(op))
			op = "default";
		
		return f.opmods.get(op);
	}

	public class Service {
		protected String name = null;
		protected HashMap<String, Feature> features = new HashMap<String, Feature>();
	}

	public class Feature {
		protected String name = null;
		protected HashMap<String, Op> ops = new HashMap<String, Op>();
		protected HashMap<String, String[]> opmods = new HashMap<String, String[]>();
	}

	public class Op {
		protected String name = null;
		protected String[] securityTags = null;
		
		protected DataType request = null;
		protected DataType response = null;
		
		public boolean isTagged(String... tags) {
			if (this.securityTags == null) 
				return false;
			
			for (int i = 0; i < this.securityTags.length; i++) {
				String has = this.securityTags[i];

				for (String wants : tags) {
					if (has.equals(wants))
						return true;
				}
			}
			
			return false;
		}
	}

	public void compile() {
		for (Service s : this.services.values()) {
			for (Feature f : s.features.values()) {
				for (Op o : f.ops.values()) {
					if (o.request != null)
						o.request.compile();
					
					if (o.response != null)
						o.response.compile();
				}
			}
		}
	}

	public void remove(String name) {
		this.services.remove(name);
	}
}
