
e.Data.ChannelXml

contains:

	<Channel Hide="True" InnerPath="/Pages" Name="Pages">
		<Template Skeleton="/skeletons/General" Name="General">
			<PagePartDef BuildId="pubMainArticle" For="article" Title="Article" Editor="html" /> 
		</Template> 
				
		<PagePartDef BuildClass="banner" BuildId="pubMainTopBanner" Gallery="/Banners" For="banner" Title="Banner" Editor="gallery" Variation="full" /> 
		<PagePartDef BuildOp="Prepend" BuildId="pubMainArticle" For="intro" Title="Intro" Editor="html" /> 
		<PagePartDef BuildId="pubMainColumns" For="column-one" Title="Article" Editor="html" /> 
		<PagePartDef BuildId="pubMainColumns" For="column-two" Title="Aside" Editor="html" /> 
	</Channel> 

parse:

	var xp = new X2JS( { useDoubleQuotes : true, keepCData : true, attributePrefix: '$', arrayAccessFormPaths: [ 'Channel.PagePartDef', 'Channel.Template.PagePartDef' ] } )
	
	var xo = xp.xml_str2json(e.Data.ChannelXml)
	

structure from JSON.stringify(xo, null, '\t')
	
	{
		"Channel": {
			"Template": {
				"PagePartDef": [
					{
						"$BuildId": "pubMainArticle",
						"$For": "article",
						"$Title": "Article",
						"$Editor": "html"
					}
				],
				"$Skeleton": "/skeletons/General",
				"$Name": "General"
			},
			"PagePartDef": [
				{
					"$BuildClass": "banner",
					"$BuildId": "pubMainTopBanner",
					"$Gallery": "/Banners",
					"$For": "banner",
					"$Title": "Banner",
					"$Editor": "gallery",
					"$Variation": "full"
				},
				{
					"$BuildOp": "Prepend",
					"$BuildId": "pubMainArticle",
					"$For": "intro",
					"$Title": "Intro",
					"$Editor": "html"
				},
				{
					"$BuildId": "pubMainColumns",
					"$For": "column-one",
					"$Title": "Article",
					"$Editor": "html"
				},
				{
					"$BuildId": "pubMainColumns",
					"$For": "column-two",
					"$Title": "Aside",
					"$Editor": "html"
				}
			],
			"$Hide": "True",
			"$InnerPath": "/Pages",
			"$Name": "Pages"
		}
	}

back to xml

vkbeautify.xml(xp.json2xml_str(xo))


content:

e.Data.ContentXml

	<dcf Locale="en">
		<Field Value="Home" Name="Title" /> 
		<Field Value="." Name="Description" /> 
		<Field Value="." Name="Keywords" /> 
		<PagePart Format="image" Value="/Banners/Big.v/full.jpg" For="banner" /> 
		<PagePart Format="md" For="intro" External="True" /> 
		<PagePart Format="md" For="column-one" External="True" /> 
		<PagePart Format="md" For="column-two" External="True" /> 
	</dcf> 

Becomes:

	{
		"dcf": {
			"Field": [
				{
					"$Value": "Home",
					"$Name": "Title"
				},
				{
					"$Value": ".",
					"$Name": "Description"
				},
				{
					"$Value": ".",
					"$Name": "Keywords"
				}
			],
			"PagePart": [
				{
					"$Format": "image",
					"$Value": "/Banners/Big.v/full.jpg",
					"$For": "banner"
				},
				{
					"$Format": "md",
					"$For": "intro",
					"$External": "True"
				},
				{
					"$Format": "md",
					"$For": "column-one",
					"$External": "True"
				},
				{
					"$Format": "md",
					"$For": "column-two",
					"$External": "True"
				}
			],
			"$Locale": "en"
		}
	}


## additional notes

	xtralayout.push({
		Element: 'FieldContainer',
		Label: $(this).attr('Label'),
		Children: [
				{
					Element: 'TextInput',
					Name: $(this).attr('Name')
				}
		]
	});



	