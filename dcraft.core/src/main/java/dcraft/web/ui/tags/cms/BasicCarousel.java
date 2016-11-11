package dcraft.web.ui.tags.cms;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.cms.util.GalleryUtil;
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
import dcraft.xml.XNode;

public class BasicCarousel extends UIElement {
	public BasicCarousel() {
		super("dcm.BasicCarousel");
	}
	
	@Override
	public void expand(WeakReference<UIWork> work) {
		String gallery = this.getAttribute("Gallery");
		String alias = this.getAttribute("Show");

		this
			.withClass("dcm-basic-carousel-img", "dc-no-select")
			.withAttribute("data-dcm-period", this.getAttribute("Period"))
			.withAttribute("data-dcm-gallery", gallery)
			.withAttribute("data-dcm-show", alias);
		
		RecordStruct meta = (RecordStruct) work.get().getContext().getSite().getGalleryMeta(gallery, work.get().getContext().isPreview());
		
		RecordStruct showmeta = GalleryUtil.findShow(meta, alias);
		
		if (showmeta != null) {
			this
				.withAttribute("data-dcm-centering", showmeta.getFieldAsString("Centering"));
			
			boolean defpreload = showmeta.getFieldAsBoolean("Preload");
			boolean preloadenabled = this.hasNotEmptyAttribute("Preload") 
					? "true".equals(this.getAttribute("Preload").toLowerCase())
					: defpreload;

			String variname = showmeta.getFieldAsString("Variation");
			ListStruct images = showmeta.getFieldAsList("Images");
			
			if ((images != null) && ! images.isEmpty() && StringUtil.isNotEmpty(variname)) {
				UIElement viewer = new UIElement("img");
				
				viewer
					.withClass("dcm-basic-carousel-img");
				
				// TODO add a randomize option
				
				// TODO support a separate preload image, that is not a variation but its own thing
				// such as checkered logos in background
				
				RecordStruct topimg = images.getItemAsRecord(0);
				
				if (preloadenabled) {
					CacheFile preload = work.get().getContext().getSite().findSectionFile("galleries", gallery + "/" + 
							topimg.getFieldAsString("Alias") + ".v/preload.jpg", work.get().getContext().isPreview());
					
					Memory mem = IOUtil.readEntireFileToMemory(preload.getFilePath());
					
					String data = Base64.encodeToString(mem.toArray(), false);
					
					viewer.withAttribute("src", "data:image/jpeg;base64," + data);
				}
				
				UIElement list = new UIElement("div").withClass("dcm-basic-carousel-list");
				
				for (Struct simg : images.getItems()) {
					RecordStruct img = (RecordStruct) simg;
					
					UIElement iel = new UIElement("img");
					
					iel
						.withAttribute("src", "/galleries" + gallery + "/" + img.getFieldAsString("Alias") + ".v/" + variname + ".jpg")
						.withAttribute("data-dcm-img", img.toString());
					
					list.with(iel);
				}
				
				this.with(viewer).with(list);
			}
		}
		
		super.expand(work);
	}

	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this
			.withClass("dcm-basic-carousel")
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName());
    	
		this.setName("div");
		
		super.translate(work, pnodes);
    }
}
