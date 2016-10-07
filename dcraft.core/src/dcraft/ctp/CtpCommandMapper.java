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
package dcraft.ctp;

import dcraft.ctp.cmd.EngageCommand;
import dcraft.ctp.cmd.ProgressCommand;
import dcraft.ctp.cmd.RelayCommand;
import dcraft.ctp.cmd.RequestCommand;
import dcraft.ctp.cmd.ResponseCommand;
import dcraft.ctp.cmd.StateCommand;

public class CtpCommandMapper {
	static public CtpCommandMapper instance = new CtpCommandMapper();
	
	public CtpCommand map(int code) {
		if (code == CtpConstants.CTP_CMD_ALIVE)
			return CtpCommand.ALIVE;
		
		if (code == CtpConstants.CTP_CMD_ENGAGE)
			return new EngageCommand();
		
		if (code == CtpConstants.CTP_CMD_EXIT)
			return CtpCommand.EXIT_NO_SIGN_OUT;
		
		if (code == CtpConstants.CTP_CMD_EXIT_SIGN_OUT)
			return CtpCommand.EXIT_SIGN_OUT;
		
		if (code == CtpConstants.CTP_CMD_PROGRESS)
			return new ProgressCommand();
		
		if (code == CtpConstants.CTP_CMD_RELAY)
			return new RelayCommand();
		
		if (code == CtpConstants.CTP_CMD_REQUEST)
			return new RequestCommand();
		
		if (code == CtpConstants.CTP_CMD_RESPONSE)
			return new ResponseCommand();
		
		if (code == CtpConstants.CTP_CMD_STATE)
			return new StateCommand();
		
		return null;
	}
}
