package dcraft.ctp.cmd;

import dcraft.ctp.CtpConstants;

public class StateCommand extends BodyCommand {
	public StateCommand() {
		this.setCmdCode(CtpConstants.CTP_CMD_STATE);
	}
}
