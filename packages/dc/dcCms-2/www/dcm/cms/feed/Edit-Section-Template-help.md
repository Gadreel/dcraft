Title: Edit Section Template Help
Skeleton: /dcw/MarkdownViewer
AuthTags: Admin,Editor

This section is required for a Gallery Section to operate correctly, however it is more complex that many of the CMS settings and it is advisable to see assistance from a web developer before changing this.

Consider an image list which has extra properties of Phone, Email, Title and Name.  Here is an example of the Show's data:

```javascript
Image: [
	 { 
	 	"Name": "Yvette Jones",
	 	"Title": "Marketing Advisor",
		"Description": "Expertise in online, print, radio",
		"Phone": "608.555.6666",
		"Email": "yvette@example.com",
		"Alias": "Yvette-Jones"
	 } ,
	 { 
	 	"Name": "Jay Smith",
	 	"Title": "Accounting Advisor",
		"Description": "Accounting for remodeling industry",
		"Phone": "608.777.8888",
		"Email": "jsmith@example.com",
		"Alias": "Jay-Smith"
	 } ,
	 { 
	 	"Name": "Kathy Johnson",
	 	"Title": "Executive Director",
		"Description": "Leads meetings, develops proposals",
		"Phone": "608.333.4444",
		"Email": "kathy@example.com",
		"Alias": "Kathy-Johnson"
	 } 
]
```

And here is an example template:

```xml
&lt;div class="board-member"&gt;
	&lt;img src="&#64;path&#64;" /&gt;
	&lt;p class="name-title"&gt;&#64;img|Name&#64;&lt;br /&gt;&#64;img|Title&#64;&lt;/p&gt;
	&lt;p&gt;
		&#64;img|Description&#64;&lt;br /&gt;
		&lt;a href="tel:&#64;img|Phone&#64;"&gt; &#64;img|Phone&#64; &lt;/a&gt;
		&lt;br /&gt;
		&lt;a href="mailto:&#64;img|Email&#64;"&gt; &#64;img|Email&#64; &lt;/a&gt;
	&lt;/p&gt;
&lt;/div&gt;
```

A template is intended to layout one entry of the image list. It will be be repeated for each image in the list.  The template is filled in by replacing macros within the template.

The key macro is `&#64;path&#64;` which expands to the full path of the image for that show and image.

The other macro starts with img| such as `&#64;img|Description&#64;`.  The word following the | is the property name to expand.  For example `&#64;img|Name&#64;` will grab the value from the image's Name property.

Given the template above, here is how the first entry will expand:

```xml
&lt;div class="board-member"&gt;
	&lt;img src="/galleries/Board/Yvette-Jones.v/full.jpg" /&gt;
	&lt;p class="name-title"&gt;Yvette Jones&lt;br /&gt;Marketing Advisor&lt;/p&gt;
	&lt;p&gt;
		Expertise in online, print, radio&lt;br /&gt;
		&lt;a href="tel:608.555.6666"&gt; 608.555.6666 &lt;/a&gt;
		&lt;br /&gt;
		&lt;a href="mailto:yvette@example.com"&gt; yvette@example.com &lt;/a&gt;
	&lt;/p&gt;
&lt;/div&gt;
```

Note: the template must be well-formed XML, as must all HTML used in this CMS.
