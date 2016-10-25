package dcraft.ctp.cmd;

import dcraft.ctp.CtpConstants;

public class RelayCommand extends BodyCommand {
	public RelayCommand() {
		this.setCmdCode(CtpConstants.CTP_CMD_RELAY);
	}
}
