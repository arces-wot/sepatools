/* This is the SEPA consumer interface
Copyright (C) 2016-2017 Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package arces.unibo.SEPA.client.pattern;

import arces.unibo.SEPA.commons.SPARQL.ARBindingsResults;
import arces.unibo.SEPA.commons.SPARQL.Bindings;
import arces.unibo.SEPA.commons.SPARQL.BindingsResults;

public interface IConsumer extends IClient{	
	public String subscribe(Bindings forcedBindings);
	public boolean unsubscribe();
	
	public void notify(ARBindingsResults notify,String spuid,Integer sequence);
	public void notifyAdded(BindingsResults bindingsResults,String spuid,Integer sequence);
	public void notifyRemoved(BindingsResults bindingsResults,String spuid,Integer sequence);
	
	public void onSubscribe(BindingsResults bindingsResults,String spuid);
}
