package dcraft.cms.util;

import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;

public class GalleryUtil {
	static public RecordStruct findVariation(RecordStruct meta, String alias) {
		if (StringUtil.isEmpty(alias))
			return null;
		
		if ((meta != null) && meta.isNotFieldEmpty("Variations")) {
			for (Struct vs : meta.getFieldAsList("Variations").getItems()) {
				RecordStruct vari = (RecordStruct) vs;
				
				if (alias.equals(vari.getFieldAsString("Alias")))
					return vari;
			}
		}
		
		return null;
	};

	static public RecordStruct findShow(RecordStruct meta, String alias) {
		if (StringUtil.isEmpty(alias))
			return null;
		
		if ((meta != null) && meta.isNotFieldEmpty("Shows")) {
			for (Struct vs : meta.getFieldAsList("Shows").getItems()) {
				RecordStruct vari = (RecordStruct) vs;
				
				if (alias.equals(vari.getFieldAsString("Alias")))
					return vari;
			}
		}
		
		return null;
	};

	static public RecordStruct findPan(RecordStruct meta, String alias) {
		if (StringUtil.isEmpty(alias))
			return null;
		
		if ((meta != null) && meta.isNotFieldEmpty("UploadPlans")) {
			for (Struct vs : meta.getFieldAsList("UploadPlans").getItems()) {
				RecordStruct vari = (RecordStruct) vs;
				
				if (alias.equals(vari.getFieldAsString("Alias")))
					return vari;
			}
		}
		
		return null;
	};
}
