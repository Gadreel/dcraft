package dcraft.web.ui.tags.cms;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.cms.util.GalleryUtil;
import dcraft.filestore.CommonPath;
import dcraft.io.CacheFile;
import dcraft.lang.Memory;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.Base64;
import dcraft.util.IOUtil;
import dcraft.util.StringUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.web.ui.tags.Button;
import dcraft.xml.XNode;

public class BasicCarousel extends UIElement {
	public BasicCarousel() {
		super("dcm.BasicCarousel");
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
		String gallery = this.getAttribute("Gallery");
		String alias = this.getAttribute("Show");
		Long pathpart = Struct.objectToInteger(this.getAttribute("PathPart"));
		
		if (pathpart != null) {
			CommonPath fpath = work.get().getContext().getPath();
			
			if (! fpath.isRoot()) {
				String name = fpath.getName(pathpart.intValue() - 1);
				
				if (StringUtil.isNotEmpty(name))
					alias = name;
			}
		}
		
		System.out.println("using show: " + alias);

		this
			.withClass("dc-no-select")
			.withAttribute("data-dcm-period", this.getAttribute("Period"))
			.withAttribute("data-dcm-gallery", gallery)
			.withAttribute("data-dcm-show", alias);
		
		RecordStruct meta = (RecordStruct) work.get().getContext().getSite().getGalleryMeta(gallery, work.get().getContext().isPreview());
		
		RecordStruct showmeta = GalleryUtil.findShow(meta, alias);
		
		if (showmeta != null) {
			this
				.withAttribute("data-dcm-centering", showmeta.getFieldAsString("Centering"));
			
			boolean defpreload = showmeta.getFieldAsBooleanOrFalse("Preload");
			boolean preloadenabled = this.hasNotEmptyAttribute("Preload") 
					? "true".equals(this.getAttribute("Preload").toLowerCase())
					: defpreload;

			String variname = showmeta.getFieldAsString("Variation");
			ListStruct images = showmeta.getFieldAsList("Images");
			
			if ((images != null) && ! images.isEmpty() && StringUtil.isNotEmpty(variname)) {
				UIElement viewer = new UIElement("img");
				
				viewer
					.withClass("dcm-basic-carousel-img");
				
				UIElement fader = new UIElement("img");
				
				fader
					.withClass("dcm-basic-carousel-fader");
				
				
				// TODO add a randomize option
				
				// TODO support a separate preload image, that is not a variation but its own thing
				// such as checkered logos in background
				
				RecordStruct topimg = images.getItemAsRecord(0);
				
				if (preloadenabled) {
					CacheFile preload = work.get().getContext().getSite().findSectionFile("galleries", gallery + "/" + 
							topimg.getFieldAsString("Alias") + ".v/preload.jpg", work.get().getContext().isPreview());
					
					if (preload != null) {
						Memory mem = IOUtil.readEntireFileToMemory(preload.getFilePath());
						
						String data = Base64.encodeToString(mem.toArray(), false);
						
						viewer.withAttribute("src", "data:image/jpeg;base64," + data);
					}
				}
				
				UIElement list = new UIElement("div").withClass("dcm-basic-carousel-list");
				
				for (Struct simg : images.getItems()) {
					RecordStruct img = (RecordStruct) simg;
					
					String extrapath = img.isNotFieldEmpty("Path") ? img.getFieldAsString("Path") + "/" : "";
					
					UIElement iel = new UIElement("img");
					
					iel
						.withAttribute("src", "/galleries" + gallery + "/" + extrapath + img.getFieldAsString("Alias") + ".v/" + variname + ".jpg")
						.withAttribute("data-dcm-img", img.toString());
					
					list.with(iel);
				}
				
				this.with(fader).with(viewer).with(list);
			}
			
			this.with(new Button("dcmi.GalleryButton")
					.withClass("dcuiPartButton", "dcuiCmsi")
					.withAttribute("Icon", "fa-pencil")
				);
		}
		
		super.expand(work);
	}

	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this
			.withClass("dcm-basic-carousel", "dcm-cms-editable")
			.withAttribute("data-dccms-edit", this.getAttribute("AuthTags", "Editor,Admin,Developer"))
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName());
    	
		this.setName("div");
		
		super.translate(work, pnodes);
    }
}
