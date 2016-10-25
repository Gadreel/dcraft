package dcraft.web.ui.tags.form;

import java.util.List;

import dcraft.web.ui.UIElement;
import dcraft.xml.XElement;

public class RadioGroup extends CoreField {
	@Override
	public void addControl() {
		UIElement grp = new UIElement("div")
			.withClass("dc-pui-control");
		
		grp
			.withAttribute("id", this.fieldid)
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName());
		
		if ("dcf.HorizRadioGroup".equals(this.getName()) || "dcf.YesNo".equals(this.getName()))
			grp.withClass("dc-pui-controlgroup-horizontal");
		else
			grp.withClass("dc-pui-controlgroup-vertical");
		
		RadioControl.enhanceField(this, grp);
		
		List<XElement> inputs = this.fieldinfo.selectAll("RadioButton");
		
		for (XElement el : inputs) 
			grp.with(RadioControl.fromRadioField(this, (UIElement) el));

		this.with(grp);
		
		/*
		 * 
				<dcf.RadioGroup Label="Radio:" Name="Internship">
				   <RadioButton Value="Internship" Label="Yes, a pre-Apprenticeship Internship only right now" />
				   <RadioButton Value="Either" Label="Yes, either a pre-Apprenticeship Internship or an Apprenticeship" />
				   <RadioButton Value="Apprenticeship" Label="No thanks, an Apprenticeship only please" />
				</dcf.RadioGroup>
				

				<div class="dc-pui-control dc-pui-controlgroup-vertical">
					<div class="dc-pui-radio">
						<input type="radio" id="comm1" name="CertInterest" value="No" />
						<label for="comm1" class="dc-pui-input-button"><i class="fa fa-circle" aria-hidden="true"></i> Not Interested</label>
					</div>
					<div class="dc-pui-radio">
						<input type="radio" id="comm2" name="CertInterest" value="Yes" />
						<label for="comm2" class="dc-pui-input-button"><i class="fa fa-circle" aria-hidden="true"></i> Interested</label>
					</div>
					<div class="dc-pui-radio">
						<input type="radio" id="comm3" name="CertInterest" value="Maybe" />
						<label for="comm3" class="dc-pui-input-button"><i class="fa fa-circle" aria-hidden="true"></i> Maybe</label>
					</div>
				</div>


				<dcf.HorizRadioGroup Label="Years Experience:" Name="GrazingYears" Required="true">
				   <RadioButton Value="LessThanFive" Label="0 - 5" />
				   <RadioButton Value="FiveToTen" Label="5 - 10" />
				   <RadioButton Value="TenToFifteen" Label="10 - 15" />
				   <RadioButton Value="FifteenToTwenty" Label="15 - 20" />
				   <RadioButton Value="TwentyOrMore" Label="20+" />
				</dcf.HorizRadioGroup>

				
				<div class="dc-pui-control dc-pui-controlgroup-horizontal">
					<div class="dc-pui-radio">
						<input type="radio" id="comm1b" name="CertInterestb" value="No" />
						<label for="comm1b" class="dc-pui-input-button"><i class="fa fa-circle" aria-hidden="true"></i> Not Interested</label>
					</div>
					<div class="dc-pui-radio">
						<input type="radio" id="comm2b" name="CertInterestb" value="Yes" />
						<label for="comm2b" class="dc-pui-input-button"><i class="fa fa-circle" aria-hidden="true"></i> Interested</label>
					</div>
					<div class="dc-pui-radio">
						<input type="radio" id="comm3b" name="CertInterestb" value="Maybe" />
						<label for="comm3b" class="dc-pui-input-button"><i class="fa fa-circle" aria-hidden="true"></i> Maybe</label>
					</div>
				</div>
				
				
		*/		
	}
}
