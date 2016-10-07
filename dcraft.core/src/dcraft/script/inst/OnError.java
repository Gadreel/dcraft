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
package dcraft.script.inst;

import java.util.List;

import dcraft.locale.Tr;
import dcraft.script.ErrorMode;
import dcraft.script.ExecuteState;
import dcraft.script.Instruction;
import dcraft.script.StackEntry;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;

public class OnError extends Instruction {
	@Override
	public void run(final StackEntry stack) {
		String output = this.source.hasText() ? stack.resolveValue(this.source.getText()).toString() : null;
		long code = stack.intFromSource("Code", 0);
		
		if (StringUtil.isEmpty(output) && (code > 1)) {
			List<XElement> params = this.source.selectAll("Param");
			Object[] oparams = new Object[params.size()];
			
			for (int i = 0; i < params.size(); i++) 
				oparams[i] = stack.refFromElement(params.get(i), "Value").toString();
			
			output = Tr.tr("_code_" + code, oparams);
		}		
		
		String mode = stack.stringFromSource("Mode");
		
		ErrorMode m = ErrorMode.Resume;
		
		if (StringUtil.isNotEmpty(mode))
			try {
				m = ErrorMode.valueOf(mode);
			}
			catch (Exception x) {
			}
		
		stack.getActivity().setErrorMode(m, code, output);
		stack.setState(ExecuteState.Done);
		stack.resume();
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
