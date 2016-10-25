package dcraft.ctp.cmd;

import dcraft.ctp.CtpConstants;

public class RequestCommand extends BodyCommand {
	public RequestCommand() {
		this.setCmdCode(CtpConstants.CTP_CMD_REQUEST);
	}
}
