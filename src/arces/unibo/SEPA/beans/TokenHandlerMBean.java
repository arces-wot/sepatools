package arces.unibo.SEPA.beans;

public interface TokenHandlerMBean {

	public int getAvailableTokens();
	public long getTimeout();
	public void setTimeout(long timeout);
	public long getMaxTokens();
	
}
