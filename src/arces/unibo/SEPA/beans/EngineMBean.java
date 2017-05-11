package arces.unibo.SEPA.beans;

import java.util.Date;

import arces.unibo.SEPA.scheduling.EngineProperties;

public interface EngineMBean {
	
	public EngineProperties getProperties();
	public Date getStartDate();
}
