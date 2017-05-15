/* This class contains utility methods to register and manage JMX beans
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package arces.unibo.SEPA.server.beans;

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
