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
package dcraft.ctp.f;

import dcraft.ctp.CtpCommand;
import dcraft.ctp.CtpCommandMapper;
import dcraft.ctp.CtpConstants;

public class CtpfCommandMapper extends CtpCommandMapper {
	static public CtpfCommandMapper instance = new CtpfCommandMapper();
	
	@Override
	public CtpCommand map(int code) {
		CtpCommand cmd = super.map(code);
		
		if (cmd != null)
			return cmd;
		
		if (code == CtpConstants.CTP_F_CMD_STREAM_ABORT)
			return CtpFCommand.STREAM_ABORT;
		
		if (code == CtpConstants.CTP_F_CMD_STREAM_BLOCK)
			return new BlockCommand();
		
		if (code == CtpConstants.CTP_F_CMD_STREAM_FINAL)
			return CtpFCommand.STREAM_FINAL;
		
		if (code == CtpConstants.CTP_F_CMD_STREAM_READ)
			return CtpFCommand.STREAM_READ;
		
		if (code == CtpConstants.CTP_F_CMD_STREAM_WRITE)
			return CtpFCommand.STREAM_WRITE;
		
		return null;
	}
}
