package dcraft.filestore.bucket;

import dcraft.cms.bucket.GalleryBucket;
import dcraft.hub.Hub;
import dcraft.hub.SiteInfo;
import dcraft.xml.XElement;

public class BucketUtil {
	static public Bucket buildBucket(String name, SiteInfo site) {
		XElement settings = site.getSettings();
		
		if (settings == null)
			return null;
		
		for (XElement bucket : settings.selectAll("Buckets/Bucket")) {
			if (name.equals(bucket.getAttribute("Name"))) {
				// TODO allow for override classes
				
				Bucket b = new Bucket();
				
				if (bucket.hasNotEmptyAttribute("Class")) {
					b = (Bucket) Hub.instance.getInstance(bucket.getAttribute("Class"));
				}
				
				b.init(site, bucket, null);
				return b;
			}
		}
		
		if ("WebGallery".equals(name)) {
			XElement bucket = new XElement("Bucket")
				.withAttribute("Name", "WebGallery")
				.withAttribute("ReadAuthTags", "Editor,Admin")
				.withAttribute("WriteAuthTags", "Editor,Admin")
				.withAttribute("RootFolder", "/galleries");

			Bucket b = new GalleryBucket();
			b.init(site, bucket, null);
			return b;
		}
		
		if ("WebFileStore".equals(name)) {
			XElement bucket = new XElement("Bucket")
				.withAttribute("Name", "WebFileStore")
				.withAttribute("ReadAuthTags", "Editor,Admin")
				.withAttribute("WriteAuthTags", "Editor,Admin")
				.withAttribute("RootFolder", "/files");

			Bucket b = new Bucket();
			b.init(site, bucket, null);
			return b;
		}
		
		if ("TenantFileStore".equals(name)) {
			XElement bucket = new XElement("Bucket")
				.withAttribute("Name", "TenantFileStore")
				.withAttribute("ReadAuthTags", "Developer")
				.withAttribute("WriteAuthTags", "Developer")
				.withAttribute("RootFolder", "/");

			Bucket b = new Bucket();
			b.init(site, bucket, null);
			return b;
		}
		
		if ("ManagedForm".equals(name)) {
			XElement bucket = new XElement("Bucket")
				.withAttribute("Name", "ManagedForm")
				.withAttribute("UploadToken", "true")
				.withAttribute("ReadAuthTags", "Admin,Staff")
				.withAttribute("WriteAuthTags", "Guest,User")
				.withAttribute("RootFolder", "/buckets/ManagedForm");

			Bucket b = new Bucket();
			b.init(site, bucket, null);
			return b;
		}

		// sub sites may access root Gallery and root Files if integrated mode
		if (site.isSharedSection("files")) 		// tests both files and galleries
			return site.getTenant().getRootSite().getBucket(name);
		
		return null;
	}
	
	static public boolean isSufficentEvidence(String lookingfor, String got) {
		if ("Size".equals(lookingfor)) 
			return ("Size".equals(got)  || "MD5".equals(got) || "SHA128".equals(got) || "SHA256".equals(got) || "SHA512".equals(got));
		
		if ("MD5".equals(lookingfor)) 
			return ("MD5".equals(got) || "SHA128".equals(got) || "SHA256".equals(got) || "SHA512".equals(got));
		
		if ("SHA128".equals(lookingfor)) 
			return ("SHA128".equals(got) || "SHA256".equals(got) || "SHA512".equals(got));
		
		if ("SHA256".equals(lookingfor)) 
			return ("SHA256".equals(got) || "SHA512".equals(got));
		
		if ("SHA512".equals(lookingfor)) 
			return ("SHA512".equals(got));
		
		return false;
	}
	
	static public String maxEvidence(String lhs, String rhs) {
		if ("Size".equals(lhs) && ("MD5".equals(rhs) || "SHA128".equals(rhs) || "SHA256".equals(rhs) || "SHA512".equals(rhs)))
			return rhs;
		
		if ("MD5".equals(lhs) && ("SHA128".equals(rhs) || "SHA256".equals(rhs) || "SHA512".equals(rhs)))
			return rhs;
		
		if ("SHA128".equals(lhs) && ("SHA256".equals(rhs) || "SHA512".equals(rhs)))
			return rhs;
		
		if ("SHA256".equals(lhs) && "SHA512".equals(rhs))
			return rhs;
		
		return lhs;
	}	
}
