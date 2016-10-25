package dcraft.ctp.cmd;

import dcraft.ctp.CtpConstants;

public class EngageCommand extends BodyCommand {
	public EngageCommand() {
		this.setCmdCode(CtpConstants.CTP_CMD_ENGAGE);
	}
}
