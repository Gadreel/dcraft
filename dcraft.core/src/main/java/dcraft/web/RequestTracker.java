package dcraft.web;

public class RequestTracker {
	public long start = 0;
	public long deadMark = 0;
	public long newContext;
	public long lastOffer;
	public long currOffer;
	public boolean needVerify;
	public long atVerify;
	public long atContinue;
	public long atSendRpc;
	public long atReturnRpc;
	public long atRepliedRpc;
	public long afterVerify;
}
