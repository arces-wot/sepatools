package arces.unibo.SEPA.beans;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public class SEPABeans {
	public static boolean registerMBean(Object bObj,String mBeanName) {
		//Get the MBean server
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        
        //register the MBean        
        ObjectName name;
		try {
			name = new ObjectName(mBeanName);
		} catch (MalformedObjectNameException e) {
			return false;
		}
        try {
			mbs.registerMBean(bObj, name);
		} catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
			return false;
		}
        
        return true;
	}
}
