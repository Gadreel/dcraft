/* ************************************************************************
#
#  designCraft.io
#
#  http://designcraft.io/
#
#  Copyright:
#    Copyright 2016 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */

 /**
  * <p>
  * UIBuilder supports a multiple phase approach to building a web page.  Overall the goal is to generate html
  * output.  Except for Forms and other special content, the whole page should load as HTML even in dynamic mode.
  * The first two phases focus on getting the tags into the right places, filling out the body of the document.
  * The next two phases focus on getting the tags - many of which are enhanced Server Side Tags - to transform
  * into a valid HTML document.
  * </p>
  * 
  * <ul>
  * 	<li>Expand - goal is loading Page Parts (and their definitions) into the root of the document, though some content may insert other places as well as long as it does not change the existing structure notably.</li>
  * 	<li>Build - goal is to place the Page Parts and run server scripts on mostly assembled document</li>
  * 	<li>Translate - goal is to rearrange document into plain HTML (XML) output</li>
  * 	<li>Sequence - follow up on the Translate phase, straighten out the lists created.</li>
  * </ul>
  * 
  * Notes: From each source file / blob loaded, copy the PagePart, PagePartDef, Skeleton (only first is used), 
  * ServerScript, Function, Require* tags.
  * 
  * Add <Require Class="abc xyz" />
  * 
  * 
  * 
  * 
<CheckGroup Label="Preferred Communication:" Name="CommMethod">
	<RadioCheck Label="Email" Value="Email" />
	<RadioCheck Label="Text" Value="Text" />
	<RadioCheck Label="Phone Call" Value="Phone Call" />
</CheckGroup>


<div class="ui-field-contain">
	<fieldset id="af4bd6dc-5533-3af4-e066-102d80f2bafc" class="ui-controlgroup ui-controlgroup-vertical ui-corner-all ui-mini">
		<div role="heading" class="ui-controlgroup-label">
			<legend>Preferred Communication:</legend>
		</div>
		<div class="ui-controlgroup-controls ">
			<div class="ui-checkbox ui-mini">
				<label for="af4bd6dc-5533-3af4-e066-102d80f2bafc-ce8ae9da5b7cd6c3df2929543a9af92d" class="ui-btn ui-corner-all ui-btn-inherit ui-btn-icon-left ui-checkbox-off ui-first-child">Email</label>
				<input type="checkbox" id="af4bd6dc-5533-3af4-e066-102d80f2bafc-ce8ae9da5b7cd6c3df2929543a9af92d" name="CommMethod" value="Email" />
			</div>
			<div class="ui-checkbox ui-mini">
				<label for="af4bd6dc-5533-3af4-e066-102d80f2bafc-9dffbf69ffba8bc38bc4e01abf4b1675" class="ui-btn ui-corner-all ui-btn-inherit ui-btn-icon-left ui-checkbox-on">Text</label>
				<input type="checkbox" id="af4bd6dc-5533-3af4-e066-102d80f2bafc-9dffbf69ffba8bc38bc4e01abf4b1675" name="CommMethod" value="Text">
			</div>
			<div class="ui-checkbox ui-mini">
				<label for="af4bd6dc-5533-3af4-e066-102d80f2bafc-261d7742488a0e2b787643a1e19ad3cf" class="ui-btn ui-corner-all ui-btn-inherit ui-btn-icon-left ui-last-child ui-checkbox-on">Phone Call</label>
				<input type="checkbox" id="af4bd6dc-5533-3af4-e066-102d80f2bafc-261d7742488a0e2b787643a1e19ad3cf" name="CommMethod" value="Phone Call">
			</div>
			<label for="CommMethod" id="lblCommMethodMessage" class="error" style="display: none;"></label>
		</div>
	</fieldset>
</div>

--------

<FieldContainer Label="Full-Time Emp:">
	<TextInput Name="FullTimeEmp" DataType="Integer" />
</FieldContainer>


<div class="ui-field-contain">
	<label for="9c596236-a23f-49a0-777b-f7fa891ac7b6">Full-Time Emp: </label>
	<div class="ui-input-text ui-body-inherit ui-corner-all ui-mini ui-shadow-inset">
		<input type="text" id="9c596236-a23f-49a0-777b-f7fa891ac7b6" name="FullTimeEmp">
	</div>
</div>
  * 
  * 
  */
package dcraft.web.ui;

