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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.schema.Field.ReqTypes;
import dcraft.struct.CompositeStruct;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.ScalarStruct;
import dcraft.struct.Struct;
import dcraft.struct.builder.ICompositeBuilder;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;

public class DataType {
	enum DataKind {
		Scalar(1),
		List(2),
		Record(3);
	    
	    private int code;

	    private DataKind(int c) {
	      code = c;
	    }

	    public int getCode() {
	      return code;
	    }
	}

	protected Schema schema = null;
	protected String id = null;
	protected DataKind kind = null;
	protected XElement definition = null;	
	protected List<XElement> xtraDefinitions = null;  //new ArrayList<XElement>();
	
	// for record
	protected HashMap<String,Field> fields = null; 
	protected boolean anyRec = false;
	
	// for list
	protected TypeOptionsList items = null;
	protected int minItems = 0;
	protected int maxItems = 0;
	
	// for scalar
	protected CoreType core = null;
	
	protected boolean compiled = false;

	public String getId() {
		return this.id;
	}
	
	public DataKind getKind() {
		return this.kind;
	}

	public DataType getPrimaryItemType() {
		if (this.items != null) 
			return this.items.getPrimaryType();
		
		return null;
	}
	
	public CoreType getCoreType() {
		return this.core;
	}
	
	public boolean isAnyRecord() {
		return this.anyRec;
	}
	
	public Collection<Field> getFields() {
		if (this.fields == null)
			return new ArrayList<Field>();
		
		return this.fields.values();
	}
	
	public Field getField(String name) {
		if (this.fields == null)
			return null;
		
		return this.fields.get(name);
	}
	
	public TypeOptionsList getItems() {
		return this.items;
	}
	
	public DataType(Schema schema) {
		this.schema = schema;
	}

	// default to 13 levels
	public RecordStruct toJsonDef() {
		return this.toJsonDef(13);
	}

	public RecordStruct toJsonDef(int lvl) {
		if (lvl == 0) {
			RecordStruct def = new RecordStruct();
			def.setField("Kind", DataKind.Scalar);
			def.setField("CoreType", new CoreType(RootType.Any).toJsonDef());
			return def;
		}
		
		RecordStruct def = new RecordStruct();
		
		if (StringUtil.isNotEmpty(this.id))
			def.setField("Id", this.id);
		
		def.setField("Kind", this.kind.getCode());
		
		if (this.kind == DataKind.Record) {
			if (this.anyRec)
				def.setField("AnyRec", true);
			
			ListStruct fields = new ListStruct();
			
			for (Field fld : this.fields.values()) 
				fields.addItem(fld.toJsonDef(lvl - 1));
			
			def.setField("Fields", fields);
		}
		else if (this.kind == DataKind.List) {
			if (this.maxItems > 0)
				def.setField("MaxItems", this.maxItems);
			
			if (this.minItems > 0)
				def.setField("MinItems", this.minItems);
			
			if (this.items != null)
				def.setField("Items", this.items.toJsonDef(lvl - 1));
		}
		else if (this.kind == DataKind.Scalar) {
			if (this.core != null)
				def.setField("CoreType", this.core.toJsonDef());
		}
		
		return def;
	}
	
	public void load(XElement dtel) {
		if (this.definition != null) {
			if (this.xtraDefinitions == null)
				this.xtraDefinitions = new ArrayList<XElement>();
			
			this.xtraDefinitions.add(dtel);			
			return;
		}
		
		this.definition = dtel;
		
		String elname = dtel.getName();
		
		if ("Record".equals(elname) || "Table".equals(elname) || "Request".equals(elname) || "Response".equals(elname) || "RecRequest".equals(elname) || "RecResponse".equals(elname)) 
			this.kind = DataKind.Record;
		else if ("List".equals(elname) || "ListRequest".equals(elname) || "ListResponse".equals(elname)) 
			this.kind = DataKind.List;
		else 
			this.kind = DataKind.Scalar;
		
		this.id = dtel.getAttribute("Id");
	}

	public void compile() {
		if (this.compiled)
			return;
		
		this.compiled = true;
		
		if (this.kind == DataKind.Record)
			this.compileRecord();
		else if (this.kind == DataKind.List)
			this.compileList();
		else
			this.compileScalar();
	}

	protected void compileRecord() {
		List<String> inhlist = new ArrayList<String>();
		
		if ("True".equals(this.definition.getAttribute("Any")))
			this.anyRec = true;
		
		String inherits = this.definition.getAttribute("Inherits");
		
		if (StringUtil.isNotEmpty(inherits)) {
			String[] ilist = inherits.split(",");
			
			for (int i = 0; i < ilist.length; i++)
				inhlist.add(ilist[i]);
		}		

		List<DataType> inheritTypes = new ArrayList<DataType>();
		
		for (String iname : inhlist) {
			DataType dtype = this.schema.manager.getType(iname);
			
			if (dtype == null) {
				OperationContext.get().errorTr(413, iname);
				continue;
			}
			
			dtype.compile();
			
			inheritTypes.add(dtype);
		}
		
		this.fields = new HashMap<String,Field>();
		
		for (XElement fel : this.definition.selectAll("Field")) {
			Field f = new Field(this.schema);
			f.compile(fel);
			this.fields.put(f.name, f);
		}
		
		// TODO Review how we use xtraDefinitions 
		if (this.xtraDefinitions != null) {
			for (XElement el : this.xtraDefinitions) {
				for (XElement fel : el.selectAll("Field")) {
					Field f = new Field(this.schema);
					f.compile(fel);
					this.fields.put(f.name, f);
				}
			}
		}
		
		// TODO these should probably come before the local definitions
		for (DataType dt : inheritTypes) {
			for (Field fld : dt.getFields()) {
				if (!this.fields.containsKey(fld.name))
					this.fields.put(fld.name, fld);
			}
		}
	}

	protected void compileList() {
		this.items = new TypeOptionsList(this.schema);		
		this.items.compile(this.definition);
		
		if (this.definition.hasAttribute("MinCount"))
			this.minItems = (int)StringUtil.parseInt(this.definition.getAttribute("MinCount"), 0);
		
		if (this.definition.hasAttribute("MaxCount"))
			this.maxItems = (int)StringUtil.parseInt(this.definition.getAttribute("MaxCount"), 0);
	}

	protected void compileScalar() {
		this.core = new CoreType(this.schema);
		this.core.compile(this.definition);
	}

	// don't call this with data == null from a field if field required - required means "not null" so put the error in
	public boolean match(Object data) {
		if (this.kind == DataKind.Record) {
			if (data instanceof RecordStruct)
				return this.matchRecord((RecordStruct)data);
			
			return false;
		}
		
		if (this.kind == DataKind.List) {
			if (data instanceof RecordStruct)
				return this.matchList((RecordStruct)data);

			return false;
		}

		return this.matchScalar(data);
	}

	protected boolean matchRecord(RecordStruct data) {
		if (this.fields != null) {
			
			// match only if all required fields are present 
			for (Field fld : this.fields.values()) {
				if ((fld.required == ReqTypes.True) && !data.hasField(fld.name))
					return false;
				
				if ((fld.required == ReqTypes.IfPresent) && data.hasField(fld.name) && data.isFieldEmpty(fld.name))
					return false;
			}
			
			return true;
		}
		
		// this is an exception to the rule, there is no "non-null" state to return from this method
		return this.anyRec;
	}

	protected boolean matchList(CompositeStruct data) {
		return true;		
	}

	protected boolean matchScalar(Object data) {
		if (this.core == null) 
			return false;
		
		return this.core.match(data);
	}
	
	// don't call this with data == null from a field if field required - required means "not null" so put the error in
	// returns true only if there was a non-null value present that conforms to the expected structure (record, list or scalar) 
	// null values that do not conform should not cause an false
	public boolean validate(Struct data) {
		if (data == null)
			return false;
		
		if (this.kind == DataKind.Record) {
			if (data instanceof ICompositeBuilder)
				data = ((ICompositeBuilder)data).toLocal();		
			
			if (data instanceof RecordStruct)
				return this.validateRecord((RecordStruct)data);

			OperationContext.get().errorTr(414, data);
			return false;
		}
		
		if (this.kind == DataKind.List) {
			if (data instanceof ListStruct)
				return this.validateList((ListStruct)data);

			OperationContext.get().errorTr(415, data);		
			return false;
		}
		
		if (this.core == null) {
			OperationContext.get().errorTr(420, data);   
			return false;
		}
		
		if (this.core.root == RootType.Any)
			return true;
		
		if (data instanceof ScalarStruct) 
			return this.core.validate(this, (ScalarStruct) data);
		
		OperationContext.get().error("Data should be scalar: " + data);		
		return false;
	}
	
	public Struct normalizeValidate(Struct data) {
		if (data == null)
			return null;
		
		data.setType(this);
		
        //if (Logger.isDebug())
        //	Logger.debug("Validating: " + this.kind + " against: " + data);
		
		if (this.kind == DataKind.Record) {
			if (data instanceof ICompositeBuilder)
				data = ((ICompositeBuilder)data).toLocal();	
			
			if (data instanceof RecordStruct) {
				normalizeValidateRecord((RecordStruct)data);
				return data;
			}

			OperationContext.get().errorTr(414, data);
			return null;
		}
		
		if (this.kind == DataKind.List) {
			if (data instanceof ListStruct) {
				this.normalizeValidateList((ListStruct)data);
				return data;
			}
			
			OperationContext.get().errorTr(415, data);		
			return null;
		}
		
		if (this.core == null) {
			OperationContext.get().errorTr(420, data);   
			return null;
		}
		
		if (this.core.root == RootType.Any)
			return data;
		
		if (data instanceof ScalarStruct) 
			return this.core.normalizeValidate(this, (ScalarStruct) data);
		
		OperationContext.get().error("Data should be scalar: " + data);		
		return null;
	}

	protected boolean validateRecord(RecordStruct data) {
		if (this.fields != null) {
			// handles all but the case where data holds a field not allowed 
			for (Field fld : this.fields.values()) 
				fld.validate(data.hasField(fld.name), data.getField(fld.name));
			
			if (!this.anyRec)
				for (FieldStruct fld : data.getFields()) {
					if (! this.fields.containsKey(fld.getName()))
						OperationContext.get().errorTr(419, fld.getName(), data);	
				}
		}
		
		// this is an exception to the rule, there is no "non-null" state to return from this method
		return true;
	}

	protected void normalizeValidateRecord(RecordStruct data) {
		if (this.fields != null) {
			// handles all but the case where data holds a field not allowed 
			for (Field fld : this.fields.values()) {
		        //if (Logger.isDebug())
		        //	Logger.debug("Validating field: " + fld.name);
				
				Struct s = data.getField(fld.name);
				Struct o  = fld.normalizeValidate(data.hasField(fld.name), data.getField(fld.name));
				
				if (s != o)
					data.setField(fld.name, o);
			}
			
			if (!this.anyRec)
				for (FieldStruct fld : data.getFields()) {
					if (! this.fields.containsKey(fld.getName()))
						OperationContext.get().errorTr(419, fld.getName(), data);	
				}
		}
	}

	protected boolean validateList(ListStruct data) {
		if (this.items == null) 
			OperationContext.get().errorTr(416, data);   
		else
			for (Struct obj : data.getItems())
				this.items.validate(obj);		
		
		if ((this.minItems > 0) && (data.getSize() < this.minItems))
			OperationContext.get().errorTr(417, data);   
		
		if ((this.maxItems > 0) && (data.getSize() > this.maxItems))
			OperationContext.get().errorTr(418, data);   
		
		return true;		
	}

	protected void normalizeValidateList(ListStruct data) {
		if (this.items == null) 
			OperationContext.get().errorTr(416, data);   
		else
			for (int i = 0; i < data.getSize(); i++) {
				Struct s = data.getItem(i);
				Struct o = this.items.normalizeValidate(s);
				
				if (s != o)
					data.replaceItem(i, o);
			}
		
		if ((this.minItems > 0) && (data.getSize() < this.minItems))
			OperationContext.get().errorTr(417, data);   
		
		if ((this.maxItems > 0) && (data.getSize() > this.maxItems))
			OperationContext.get().errorTr(418, data);   
	}
	
	public Struct wrap(Object data) {
		if (data == null) 
			return null;
		
		if (this.kind == DataKind.Record) {
			if (data instanceof RecordStruct) {
				Struct s = (Struct)data;

				if (!s.hasExplicitType())
					s.setType(this);
				
				return s;
			}
			
			OperationContext.get().errorTr(421, data);		
			return null;
		}
		
		if (this.kind == DataKind.List) {
			// TODO support Collection<Object> and Array<Object> as input too
			
			if (data instanceof ListStruct) {
				Struct s = (Struct)data;
				
				if (!s.hasExplicitType())
					s.setType(this);
				
				return s;
			}
			
			OperationContext.get().errorTr(439, data);		
			return null;
		} 
		
		return this.core.normalize(this, data);
	}
	
	public FuncResult<Struct> create() {
		FuncResult<Struct> mr = new FuncResult<>();
		
		Struct st = null;
		
		// TODO not just core can have Class ...
		
		if (this.kind == DataKind.Record) 
			st = new RecordStruct();
		else if (this.kind == DataKind.List) 
			st = new ListStruct();
		else 
			st = this.core.create(this);
		
		// TODO err message if null
		
		if (st != null) {
			st.setType(this);
			mr.setResult(st);
		}
		
		return mr; 
	}
}
