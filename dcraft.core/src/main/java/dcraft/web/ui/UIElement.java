package dcraft.web.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.Map.Entry;

import dcraft.lang.CountDownCallback;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationCallback;
import dcraft.util.StringUtil;
import dcraft.web.core.IOutputContext;
import dcraft.web.ui.tags.form.Form;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class UIElement extends XElement {
	protected WeakReference<UIElement> parent = null;
	
	protected List<List<XNode>> targetnodes = new ArrayList<>();
	
	protected Map<String, String> valueparams = null;
	protected Map<String, Collection<XNode>> complexparams = null;
	
	public void setParent(UIElement v) {
		this.parent = new WeakReference<UIElement>(v);
	}

	public UIElement getParent() {
		WeakReference<UIElement> p = this.parent;
		
		if (p == null)
			return null;
		
		return p.get();
	}
	
	public UIElement getRoot() {
		UIElement p = this.getParent();
		
		if (p != null)
			return p.getRoot();
		
		return null;
	}

	public Form getForm() {
		UIElement p = this.getParent();

		while (p != null) {
			if (p instanceof Form)
				return (Form) p;
			
			p = p.getParent();
		}
		
		return null;
	}
	
	public UIElement() {
		super();
	}
	
	public UIElement(String tag) {
		super(tag);
	}
	
	public UIElement withClass(String... classnames) {
		for (String cname : classnames) {
			if (StringUtil.isNotEmpty(cname)) {
				String cv = this.getAttribute("class");
				
				if (StringUtil.isNotEmpty(cv)) {
					// TODO check for duplicates - must match entire name not just part, in the cv
					this.setAttribute("class", cv + " " + cname);		
				}
				else {
					this.setAttribute("class", cname);
				}
			}
		}
		
		return this;
	}
	
	// do more tests on this - TODO make more efficient 
	public UIElement withoutClass(String... classnames) {
		for (String cname : classnames) {
			if (StringUtil.isNotEmpty(cname)) {
				String currclass = this.getAttribute("class");
				
				// TODO repeat loop until class is thoroughly gone
				if (StringUtil.isNotEmpty(currclass)) {
					int pos = currclass.indexOf(cname);
					
					if (pos == 0) {
						String newclass = currclass.substring(pos + cname.length());
						this.setAttribute("class", newclass);		
					}
					else if (pos > 0) {
						String newclass = currclass.substring(0, pos)
								+ currclass.substring(pos + cname.length());
						this.setAttribute("class", newclass);		
					}
				}
			}
		}
		
		return this;
	}
	
    public void expand(WeakReference<UIWork> work) {
    	//System.out.println("expand: " + this.toLocalString());
    	
		for (XNode child : this.getChildren()) {
			if (child instanceof UIElement) {
				UIElement wchild = (UIElement)child;
				wchild.setParent(this);
				wchild.expand(work);
			}
		}
	}
	
    public void build(WeakReference<UIWork> work) {
    	//System.out.println("build: " + this.toLocalString());
    	
		for (XNode child : this.getChildren()) {
			if (child instanceof UIElement) {
				UIElement wchild = (UIElement)child;
				wchild.setParent(this);
				wchild.build(work);
			}
		}
	}
    
    protected void safeTranslate(WeakReference<UIWork> work) {
		for (XNode child : this.getChildren()) {
			List<XNode> cnodes = new ArrayList<>();
			
			if (child instanceof UIElement) {
				UIElement wchild = (UIElement)child;
				wchild.setParent(this);
				
				wchild.translate(work, cnodes);
			}
			else {
				cnodes.add(child);
			}
			
			this.targetnodes.add(cnodes);
		}
    }
    
    public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
    	//System.out.println("translate: " + this.toLocalString());
    	
    	// default behavior is to add only this to the parent nodes
    	if (pnodes != null)
    		pnodes.add(this);
    	
    	//OperationContext.get().touch();
    	
    	if (!this.hasChildren()) 
    		return;
    	
    	boolean fndexpand = false;
    	
		for (int i = 0; i < this.children.size(); i++) {
			XNode child = this.children.get(i);
			
			// reparse children that are not already UI
			if (! (child instanceof UIElement) && child instanceof XElement)  {
				fndexpand = true;
				break;
			}
		}
		
		// if we don't need to run an expand routine on any child then skip all the callbacks and counters
		if (!fndexpand) {
			UIElement.this.safeTranslate(work);
			return;
		}
		
    	work.get().incTranslate();
    	
    	CountDownCallback translatecallback = new CountDownCallback(1, new OperationCallback() {
			@Override
			public void callback() {
				UIElement.this.safeTranslate(work);
				work.get().decTranslate();
			}
		});
    	
		for (int i = 0; i < this.children.size(); i++) {
			XNode child = this.children.get(i);
			
			// convert children that are not already UI
			if (! (child instanceof UIElement) && child instanceof XElement)  {
				XElement xchild = (XElement)child;
				
				translatecallback.increment();
				
				final int pos = i;
				
				UIUtil.convert(work, this, xchild, 
					new FuncCallback<UIElement>() {
						@Override
						public void callback() {
							UIElement.this.replace(pos, this.getResult());
							translatecallback.countDown();
						}
					});	
			}
		}
		
		translatecallback.countDown();
    }
    
    public void translateSkip(WeakReference<UIWork> work, List<XNode> pnodes) {
    	//OperationContext.get().touch();
    	
    	if (!this.hasChildren()) 
    		return;
    	
    	boolean fndexpand = false;
    	
		for (int i = 0; i < this.children.size(); i++) {
			XNode child = this.children.get(i);
			
			// reparse children that are not already UI
			if (! (child instanceof UIElement) && child instanceof XElement)  {
				fndexpand = true;
				break;
			}
		}
		
		// if we don't need to run an expand routine on any child then skip all the callbacks and counters
		if (!fndexpand) {
			for (XNode child : this.getChildren()) {
				if (child instanceof UIElement) {
					UIElement wchild = (UIElement)child;
					wchild.setParent(this);
					
					wchild.translate(work, pnodes);
				}
				else {
					pnodes.add(child);
				}
			}
			
			return;
		}
		
    	work.get().incTranslate();
    	
    	CountDownCallback translatecallback = new CountDownCallback(1, new OperationCallback() {
			@Override
			public void callback() {
				UIElement.this.safeTranslate(work);
				
				for (XNode child : UIElement.this.getChildren()) {
					if (child instanceof UIElement) {
						UIElement wchild = (UIElement)child;
						wchild.setParent(UIElement.this);
						
						wchild.translate(work, pnodes);
					}
					else {
						pnodes.add(child);
					}
				}
				
				work.get().decTranslate();
			}
		});
    	
		for (int i = 0; i < this.children.size(); i++) {
			XNode child = this.children.get(i);
			
			// convert children that are not already UI
			if (! (child instanceof UIElement) && child instanceof XElement)  {
				XElement xchild = (XElement)child;
				
				translatecallback.increment();
				
				final int pos = i;
				
				UIUtil.convert(work, this, xchild, 
					new FuncCallback<UIElement>() {
						@Override
						public void callback() {
							UIElement.this.replace(pos, this.getResult());
							translatecallback.countDown();
						}
					});	
			}
		}
		
		translatecallback.countDown();
    }
	
    public void sequence() {
    	//System.out.println("sequence: " + this.toLocalString());
    	
		UIElement.this.children = new ArrayList<>();
    	
    	for (List<XNode> list : this.targetnodes) {
        	for (XNode node : list) {
        		UIElement.this.children.add(node);
        		
    			if (node instanceof UIElement) {
    				UIElement wchild = (UIElement)node;
    				wchild.sequence();
    			}
        	}
    	}
	}
    
	public UIElement withParam(String name, String value) {
		if (this.valueparams == null)
			this.valueparams = new HashMap<String, String>();

		this.valueparams.put(name, value);
		
		return this;
	}

	public boolean hasParam(String name) {
		if ((this.valueparams != null) && this.valueparams.containsKey(name))
			return true;

		UIElement p = this.getParent();
		
		if (p != null)
			return p.hasParam(name);

		return false;
	}

	public String getParam(String name) {
		if ((this.valueparams != null) && this.valueparams.containsKey(name))
			return this.valueparams.get(name);

		UIElement p = this.getParent();
		
		if (p != null)
			return p.getParam(name);

		return null;
	}

	public UIElement withUIParam(String name, Collection<XNode> list) {
		if (this.complexparams == null)
			this.complexparams = new HashMap<String, Collection<XNode>>();

		this.complexparams.put(name, list);
		
		return this;
	}

	public Collection<XNode> getUIParam(String name) {
		if ((this.complexparams != null) && this.complexparams.containsKey(name))
			return this.complexparams.get(name);

		UIElement p = this.getParent();
		
		if (p != null)
			return p.getUIParam(name);

		return null;
	}

	public String expandMacro(IOutputContext ctx, String value) {
		if (StringUtil.isEmpty(value))
			return null;

		boolean checkmatches = true;

		while (checkmatches) {
			checkmatches = false;
			Matcher m = UIUtil.macropatten.matcher(value);

			while (m.find()) {
				String grp = m.group();

				String macro = grp.substring(1, grp.length() - 1);
				String val = null;

				// params on this tree
				if (macro.startsWith("val|"))
					val = this.getParam(macro.substring(4));
				
				if (val == null)
					val = ctx.expandMacro(macro);

				// if any of these, then replace and check (expand) again
				if (val != null) {
					value = value.replace(grp, val);
					checkmatches = true;
				}
			}
		}

		return value;
	}
	
	// if this block is a fragment that should be merged with root, call this during build
	// PagePart, PagePartDef, Skeleton (only first is used), Function, Require	
	public void mergeWithRoot(WeakReference<UIWork> work, UIElement root, boolean includescript) {
		// copy all attributes over, unless they have been overridden
		if (this.attributes != null) {
			for (Entry<String, String> attr : this.attributes.entrySet())
				if (!root.hasAttribute(attr.getKey()))
					root.setAttribute(attr.getKey(), attr.getValue());
		}
		
		// copy appropriate children over
		for (XElement el : this.selectAll("*")) {
			String name = el.getName();
			
			if ("dc.Function".equals(name))
				root.add(el);
			else if ("dc.PagePartDef".equals(name))
				root.add(0, el);		// before the in page definitions
			else if ("dc.PagePart".equals(name))
				root.add(el);
			else if ("dc.Body".equals(name))
				root.add(el);
			else if ("dc.ServerScript".equals(name) && includescript)
				root.add(el);
			else if (name.startsWith("dc.Require"))
				root.add(el);
		}		
	}
}
