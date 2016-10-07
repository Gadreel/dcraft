package dcraft.ctp.cmd;

import dcraft.ctp.CtpConstants;
import dcraft.struct.RecordStruct;

public class ResponseCommand extends BodyCommand {
	public RecordStruct getResult() {
		return this.body;
	}

	public ResponseCommand() {
		this.setCmdCode(CtpConstants.CTP_CMD_RESPONSE);
	}
}
