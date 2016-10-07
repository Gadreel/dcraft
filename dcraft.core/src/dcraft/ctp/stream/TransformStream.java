package dcraft.ctp.stream;

abstract public class TransformStream extends BaseStream implements IStreamUp {
	@Override
	public void read() {
		if (this.handlerFlush() == ReturnOption.CONTINUE)
			this.upstream.read();
	}
}
