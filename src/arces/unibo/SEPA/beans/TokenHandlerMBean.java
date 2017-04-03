package arces.unibo.SEPA.server;

public interface TokenHandlerMBean {

	public int getAvailableTokens();
	public long getTimeout();
	public void setTimeout(long timeout);
	public long getMaxTokens();
	
}
