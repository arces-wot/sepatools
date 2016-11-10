package arces.unibo.KPI;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JLabel;

public class KP_GUI  extends Panel implements ActionListener, iKPIC_subscribeHandler2 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	KPICore kp = null;
	String user_kp_id="";	
	/**
	 * 
	 */
	StringBuilder history;
	SSAP_XMLTools xmlTools=null;

	JFrame   f1;

	JPanel jpConnection=null;
	JPanel jpJoinLeave=null;
	JPanel jpTextFieldsOld=null;
	JPanel jpConsole=null;
	JPanel jpTextFieldsNew=null;
	JPanel jpUpdate=null;
	JPanel jpSparql=null;		//SPARQL SECTION
	JPanel jpSubscriptions;
	JPanel jpRules;

	JPanel jpPolicy=null;//FG

	JPanel jpMain=null;
	JPanel jpMemo=null;


	//Connection
	JTextField tfIP,tfPort,tfSSN;
	//old
	JTextField tfSN,tfPN,tfON,tfSNType,tfONType;
	//new
	JTextField tfSO,tfPO,tfOO,tfSOType,tfOOType;
	//SPARQL
	JTextField tfSQ;
	//JtextField

	//FG POLICY
	JTextField tfA;

	JButton bGo,bJoin,bLeave,bRDFQuery,bInsert,bRemove,bSubscribe,bUnsubscribe,bUpdate;
	JButton bInsertProtection,bRemoveProtection;

	JButton bPOLICYAdd,bPOLICYDel; //FG add bPOLICYquery

	// SPARQL buttons
	JButton bSPARQLquery;
	JButton bSPARQLsubscription;
	JButton bSPARQLUpdate;
	JButton bRULE;
	JButton bDelRULE;
	JButton bViewHistory;

	//subscriptions
	JComboBox<ComboItemRenderable> cbSUB;
	JComboBox<ComboItemRenderable> cbRULES;

	//FG policy
	JComboBox<ComboItemRenderable> cbPOL;
	JComboBox<ComboItemRenderable> cbPOLPM;

	/*
	 *  Components for service discovery
	 */

	JTextField tfSerName;
	JTextField otherParams;
	JButton bSearchRService;
	JComboBox<?> cbSOFIADNSIPList;
	String[] SOFIADNS_IP = new String[1];

	//Synchronization
	JTextField tfKPID;

	JTextArea taHistory   = new JTextArea();

	JButton bSetKPID;

	JTextArea taMemo;

	JComboBox<?> cbIPList;
	JComboBox<?> cbPortList;
	/*String[] SIB_IP = {
            "127.0.0.1",
            "localhost",
            "mml.arces.unibo.it"
            };*/
	int SIB_IP_TOT=3;
	int SIB_PORT_TOT=2;
	String[] SIB_IP = new String[SIB_IP_TOT];
	String[] SIB_PORT = new String[SIB_PORT_TOT];

	String my_last_subscription="";

	JLabel label_sub;
	JLabel label_pol,label_polPM,label_polA;//FG

	public KP_GUI()
	{
		JLabel labelx;
		history = new StringBuilder();

		/*0_________________________________________________________*/
		//Connection
		tfIP   = new JTextField("");
		/*-_________________________________________________________*/

		SIB_IP[0]="127.0.0.1";
		SIB_IP[1]="localhost";
		SIB_IP[2]="mml.arces.unibo.it";

		SIB_PORT[1] = "10010";
		SIB_PORT[0] = "7701";

		/*-_________________________________________________________*/

		cbIPList = new JComboBox<Object>(SIB_IP);    
		cbIPList.setEditable(true);

		cbPortList = new JComboBox<Object>(SIB_PORT);
		cbPortList.setEditable(true);
		/*-_________________________________________________________*/

		tfPort = new JTextField("");
		tfSSN  = new JTextField("");

		tfIP.setColumns(10);
		tfPort.setColumns(5);
		tfSSN.setColumns(5);



		bGo    = new JButton("INIT"); bGo.addActionListener(this);

		jpConnection=new JPanel(false);
		jpConnection.setLayout(new GridLayout(4, 1));
		JPanel line1=new JPanel(false);
		JPanel line2=new JPanel(false);
		JPanel line3=new JPanel(false);
		JPanel line4=new JPanel(false);

		line1.add( new JLabel("IP") );   line1.add(cbIPList);
		line1.add( new JLabel("PORT") ); line1.add(cbPortList);
		line1.add( new JLabel("SSN") );  line1.add(tfSSN);
		line1.add( bGo );

		jpConnection.add( line1 );

		/*
		 * Components for service discovery
		 */
		//SOFIADNS_IP[0]="http://mml.arces.unibo.it/sofia.dns";
		SOFIADNS_IP[0]="http://mml.arces.unibo.it/sofia.service.registry";
		tfSerName    = new JTextField("sib");
		bSearchRService = new JButton("Search for Service");       bSearchRService.addActionListener(this);
		cbSOFIADNSIPList = new JComboBox<Object>(SOFIADNS_IP); cbSOFIADNSIPList.setEditable(true);

		cbSOFIADNSIPList.setSize(100, 20);
		tfSerName.setColumns(10);

		line2.add( new JLabel("SOFIA.DNS") );    line2.add(cbSOFIADNSIPList);		
		//E' tutto da mettere nella casella di testo dei parametri line2.add( new JLabel("SERVICE NAME") ); line2.add(tfSerName);
		//line2.add( bSearchAndGo );

		jpConnection.add( line2 );

		otherParams  = new JTextField("");
		otherParams.setColumns(40);

		line3.add( new JLabel("Service profile properties:") ); line3.add( otherParams );
		jpConnection.add( line3 );

		bSearchRService.setSize(100, 50);
		line4.add( bSearchRService );
		jpConnection.add( line4 );
		/*1_________________________________________________________*/

		bJoin    = new JButton("Join");  bJoin.addActionListener(this);
		bLeave   = new JButton("Leave"); bLeave.addActionListener(this);

		bViewHistory = new JButton("View Command History");
		bViewHistory.addActionListener(this);
		//bViewHistory.setForeground(Color.);


		//Synchronization
		tfKPID    = new JTextField("");		  tfKPID.setColumns(10);
		bSetKPID  = new JButton("Re-Join with this KP ID"); 
		bSetKPID.addActionListener(this);
		//bSetKPID.setForeground(Color.BLUE);


		jpJoinLeave=new JPanel(false);
		jpJoinLeave.add(bJoin);
		jpJoinLeave.add(bLeave);	


		labelx = new JLabel("KP ID");
		//labelx.setForeground(Color.BLUE);
		jpJoinLeave.add( labelx );
		jpJoinLeave.add( tfKPID   );
		jpJoinLeave.add( bSetKPID );
		jpJoinLeave.add(bViewHistory);

		/*2_________________________________________________________*/
		//old
		tfSN     = new JTextField("");
		tfPN     = new JTextField("");
		tfON     = new JTextField("");
		tfSNType = new JTextField("");
		tfONType = new JTextField("");

		tfSN.setColumns(10);
		tfPN.setColumns(10);
		tfON.setColumns(10);
		tfSNType.setColumns(10);
		tfONType.setColumns(10);

		jpTextFieldsOld=new JPanel(false);
		//jpTextFieldsOld.add( new JLabel("Subject") );
		labelx = new JLabel("Subject (S)"); 
		//labelx.setForeground(Color.RED);
		jpTextFieldsOld.add( labelx );
		jpTextFieldsOld.add( tfSN );
		//jpTextFieldsOld.add( new JLabel("Predicate") );
		labelx = new JLabel("Predicate (S)"); 
		//labelx.setForeground(Color.RED);
		jpTextFieldsOld.add( labelx );

		jpTextFieldsOld.add( tfPN ); 
		jpTextFieldsOld.add( new JLabel("Object") );    jpTextFieldsOld.add( tfON );
		jpTextFieldsOld.add( new JLabel("Sub Type") );  jpTextFieldsOld.add( tfSNType );
		jpTextFieldsOld.add( new JLabel("Obj Type") );  jpTextFieldsOld.add( tfONType );

		/*3_________________________________________________________*/

		bRDFQuery    = new JButton("RDF Query");   bRDFQuery.addActionListener(this);
		bInsert      = new JButton("Insert");      bInsert.addActionListener(this);
		bRemove      = new JButton("Remove");      bRemove.addActionListener(this);
		bSubscribe   = new JButton("Subscribe");   bSubscribe.addActionListener(this);
		bUnsubscribe = new JButton("Unsubscribe"); bUnsubscribe.addActionListener(this);

		jpConsole=new JPanel(false);
		jpConsole.add(bRDFQuery);
		jpConsole.add(bInsert);
		jpConsole.add(bRemove);
		jpConsole.add(bSubscribe);
		jpConsole.add(bUnsubscribe);

		/*4_________________________________________________________*/
		//new
		tfSO     = new JTextField("");
		tfPO     = new JTextField("");
		tfOO     = new JTextField("");
		tfSOType = new JTextField("");
		tfOOType = new JTextField("");

		tfSO.setColumns(10);
		tfPO.setColumns(10);
		tfOO.setColumns(10);
		tfSOType.setColumns(10);
		tfOOType.setColumns(10);

		jpTextFieldsNew=new JPanel(false);
		jpTextFieldsNew.add( new JLabel("Subject") );   jpTextFieldsNew.add( tfSO );
		jpTextFieldsNew.add( new JLabel("Predicate") ); jpTextFieldsNew.add( tfPO );
		jpTextFieldsNew.add( new JLabel("Object") );    jpTextFieldsNew.add( tfOO );
		jpTextFieldsNew.add( new JLabel("Sub Type") );  jpTextFieldsNew.add( tfSOType );
		jpTextFieldsNew.add( new JLabel("Obj Type") );  jpTextFieldsNew.add( tfOOType );

		/*5_________________________________________________________*/

		bUpdate    = new JButton("Update");  bUpdate.addActionListener(this);
		bInsertProtection= new JButton("Insert Protection");  bInsertProtection.addActionListener(this);
		bRemoveProtection= new JButton("Remove Protection");  bRemoveProtection.addActionListener(this);
		bInsertProtection.setForeground(Color.RED);
		bRemoveProtection.setForeground(Color.RED);		

		jpUpdate=new JPanel(false);
		jpUpdate.add(bUpdate);
		jpUpdate.add(bInsertProtection);
		jpUpdate.add(bRemoveProtection);
		cbSUB=new JComboBox<ComboItemRenderable>();

		/*6_________________________________________________________*/

		taMemo=new JTextArea(80,25);


		/*7___SPARQL________________________________________________*/

		bSPARQLquery = new JButton("SPARQL query");
		bSPARQLsubscription = new JButton("SPARQL subscription");
		bSPARQLUpdate = new JButton("SPARQL update");
		bRULE = new JButton("Persistent update");

		bSPARQLquery.addActionListener(this);
		bSPARQLsubscription.addActionListener(this);
		bSPARQLUpdate.addActionListener(this);
		bRULE.addActionListener(this);

		bDelRULE = new JButton("Delte Pers Update");
		bDelRULE.addActionListener(this);
		bDelRULE.setForeground(Color.BLUE);

		JLabel labelz = new JLabel("SPARQL query"); 
		//labelz.setForeground(Color.RED);
		tfSQ   = new JTextField("");
		tfSQ.setColumns(30);

		bSPARQLUpdate.setForeground(Color.BLUE);
		bRULE.setForeground(Color.BLUE);

		jpSparql=new JPanel(false);
		jpSparql.add(labelz); 
		jpSparql.add(tfSQ);
		jpSparql.add(bSPARQLquery);
		jpSparql.add(bSPARQLsubscription);
		jpSparql.add(bSPARQLUpdate);
		jpSparql.add(bRULE);
		jpSparql.add(bDelRULE);

		cbRULES=new JComboBox<ComboItemRenderable>();

		/*8___Subscriptions___and Rules_____*/
		jpSubscriptions = new JPanel(false);
		label_sub = new JLabel("Active subscriptions"); 
		jpSubscriptions.add(label_sub);
		jpSubscriptions.add(cbSUB);
		label_sub = new JLabel("Active subscription"); 
		/*_________________________________________________________*/

		label_sub = new JLabel("Active persistent updates");
		label_sub.setForeground(Color.BLUE);
		jpSubscriptions.add(label_sub);
		jpSubscriptions.add(cbRULES);
		//	label_sub = new JLabel("Active persistent updates"); labelx.setForeground(Color.BLUE);
		/*_________________________________________________________*/

		/* FG *9__POLICY____*/
		cbPOL=new JComboBox<ComboItemRenderable>();
		cbPOLPM=new JComboBox<ComboItemRenderable>();

		tfA   = new JTextField("");
		tfA.setColumns(15);

		bPOLICYAdd = new JButton("POLICY ADD");
		bPOLICYAdd.addActionListener(this);
		bPOLICYDel = new JButton("POLICY DELETE");
		bPOLICYDel.addActionListener(this);

		label_polPM = new JLabel("Protection Mode");
		label_polA = new JLabel("Allowed");
		label_pol = new JLabel("Active policy");

		jpPolicy=new JPanel(false);
		jpPolicy.add(label_polA);
		jpPolicy.add(tfA);
		jpPolicy.add(label_polPM);
		jpPolicy.add(cbPOLPM);
		jpPolicy.add(label_pol);
		jpPolicy.add(cbPOL);
		jpPolicy.add(bPOLICYAdd);
		jpPolicy.add(bPOLICYDel);

		ComboItemRenderable item = new ComboItemRenderable();
		item.setName("R");
		item.setVisualize("R");
		cbPOLPM.addItem(item);
		item = new ComboItemRenderable();
		item.setName("W");
		item.setVisualize("W");
		cbPOLPM.addItem(item);
		item = new ComboItemRenderable();
		item.setName("RW");
		item.setVisualize("R/W");
		cbPOLPM.addItem(item);
		/*_______________________________________________________*/

		jpMain=new JPanel(true);
		jpMain.setLayout(new GridLayout(1,1));
		jpMain.add( jpConnection ,0);

		jpMemo=new JPanel(false);
		jpMemo.setLayout(new GridLayout(1,1));
		//ADD HERE A SCROLL BAR PANE
		//jpMemo.add(taMemo);
		JScrollPane sbrText;
		sbrText = new JScrollPane(taMemo);
		sbrText.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		jpMemo.add(  sbrText   );

		taMemo.append("OK!");

		this.setLayout( new BorderLayout() );
		this.add( jpMain, BorderLayout.SOUTH);
		//this.add( jpMemo, BorderLayout.NORTH );

		System.out.println("GUI DONE!");

		//this.validateTree();

	}//KP_GUI()

	

	//scroolbars: http://www.roseindia.net/java/java-tips/GUI/components/40textarea/25ex-textarea.shtml



	public void actionPerformed(ActionEvent e)
	{
		SIBResponse ret= null;
		JComponent c = (JComponent) e.getSource();

		//System.out.println("ACTION : "+comm); if(comm==null)return;
		//System.out.println("ACTION:"+ (c == bGo?"bGO":"?") );

		if( c == bGo )
		{
			jpMain.setLayout(new GridLayout(8,1));
			System.out.println("ACTION:bGO");
			//System.out.println("ACTION:tfIP.getText()"+tfIP.getText());
			//System.out.println("ACTION:(String)cbIPList.getSelectedItem()"+(String)cbIPList.getSelectedItem());
			System.out.println("ACTION:tfPort:"+tfPort.getText());


			this.kp=new KPICore( /*tfIP.getText()*/ (String)cbIPList.getSelectedItem()
					,Integer.parseInt(cbPortList.getSelectedItem().toString())
					,tfSSN.getText());

			if(!user_kp_id.equals(""))kp.setNodeID(user_kp_id);

			this.kp.enable_debug_message();
			this.kp.enable_error_message();

			this.kp.setEventHandler2(this);
			this.kp.enable_debug_message();
			this.kp.enable_error_message();

			if(kp!=null)
			{   jpMain.remove(jpConnection);
			jpMain.add( jpJoinLeave );
			jpMain.add( jpTextFieldsOld );
			jpMain.add( jpConsole );
			jpMain.add( jpTextFieldsNew );
			jpMain.add( jpUpdate );
			jpMain.add( jpSparql );
			jpMain.add( jpSubscriptions );

			jpMain.add( jpPolicy );//FG

			this.add( jpMemo, BorderLayout.CENTER );

			taMemo.setText("");
			taMemo.append("READY!");

			this.getParent().setSize(1200, 600);
			this.getParent().repaint();

			xmlTools = new SSAP_XMLTools(null,null,null);

			}//if(kp!=null)
		}//if(c==bGo)

		else if( c == bSearchRService )
		{

			System.out.println("ACTION:bSearchAndGo");
			System.out.println("ACTION:tfSerName:"+tfSerName.getText());
			System.out.println("ACTION:cbSOFIADNSIPList:"+cbSOFIADNSIPList.getSelectedItem());


			ArcesServiceRegistry sr=new ArcesServiceRegistry( (String)cbSOFIADNSIPList.getSelectedItem() );
			Vector<Properties> reg=null;
			Properties userServiceSearch=new Properties();

			//From, e.g. a=1&b=2&qwerty=23, to properties
			if(!otherParams.getText().equals(""))
			{
				String serviceInfo[]=otherParams.getText().split("\\&");

				for(int i=0;i<serviceInfo.length;i++)
				{ String param[]=serviceInfo[i].split("=");  int name=0, value=1;
				userServiceSearch.setProperty(param[name], param[value]);
				}
			}	    	

			reg = sr.search( userServiceSearch );

			if(reg==null || reg.size()==0)
			{JOptionPane.showConfirmDialog(null,
					"No service found!\n...try again!"
					,"WARNING",JOptionPane.CLOSED_OPTION);
			}
			else
			{String str_service_list="";

			for(int i=0; i<reg.size() ;i++)
			{ 
				str_service_list+=reg.elementAt(i).toString()+"\n";
			}

			JOptionPane.showConfirmDialog(null,
					"Service found list:\n"
							+str_service_list
							+"\n"
							,"WARNING",JOptionPane.CLOSED_OPTION);
			}


		}//else if( c == bSearchRService )

		else if( c == bJoin)
		{ taMemo.setText(""); 
		ret = kp.join();  
		taMemo.append("Sent: " + kp.xmlTools.join() + "\n");

		taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n"); 
		taMemo.append("Join confirmed:"+(this.xmlTools.isResponseConfirmed(ret)?"YES":"NO"));
		taMemo.append("\n*** SSAP message status:"+ret.Status +"\n");
		history.append("join ip=" + kp.HOST + " port = " + kp.PORT + " SIB Nme = " + kp.SMART_SPACE_NAME + "\n");

		taHistory.setText(history.toString());
		}//bJoin

		else if( c == bLeave)
		{ taMemo.setText(""); ret = kp.leave(); 
		taMemo.append("Sent: " + kp.xmlTools.leave() + "\n");

		taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n"); 
		taMemo.append("Leave confirmed:"+(this.xmlTools.isResponseConfirmed(ret)?"YES":"NO"));
		taMemo.append("\n*** SSAP message status:"+ ret.Status +"\n");
		history.append("leave ip=" + kp.HOST + " port = " + kp.PORT + " SIB Nme = " + kp.SMART_SPACE_NAME + "\n");

		taHistory.setText(history.toString());
		}//bLeave

		else if( c == bRDFQuery)
		{ taMemo.setText("");
		ret = kp.queryRDF( tfSN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfSN.getText()
				,tfPN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfPN.getText() 
						,tfON.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfON.getText()
								,tfSNType.getText()
								,tfONType.getText());
		taMemo.append("Sent: " + kp.xmlTools.queryRDF( tfSN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfSN.getText()
				,tfPN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfPN.getText() 
						,tfON.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfON.getText()
								,tfSNType.getText()
								,tfONType.getText()) + "\n");

		taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n"); 
		taMemo.append("RDFQuery confirmed:"+(this.xmlTools.isResponseConfirmed(ret)?"YES":"NO")+"\n");
		taMemo.append("\n*** SSAP message status:"+ ret.Status +"\n");

		if(!this.xmlTools.isResponseConfirmed(ret))
		{
			return;
		}
		history.append("rdf query s = " + tfSN.getText() + " p = " + tfPN.getText() + " o = " + tfON.getText()+ " ot = " + tfONType.getText() + "\n" );
		Vector<Vector<String>> triples = null;

		if(ret!=null)
		{
			triples = ret.query_results;
		}

		if(triples!=null)
		{
			taMemo.append("Triple List:\n");
			for(int i=0; i<triples.size() ; i++ )
			{ Vector<String> t=triples.get(i);
			String st=  "  S:["+t.get(0)
					+"] P:["+t.get(1)
					+"] O:["+t.get(2)
					+"] Otype:["+t.get(3)+"]";

			taMemo.append(st+"\n");

			}//for(int j=0; i<triple.size() ; i++ )
		}  
		taHistory.setText(history.toString());
		}//bRDFQuery
		else if( c == bSPARQLquery)
		{

			//TEST 
			taMemo.setText("");
			taMemo.append("\nSPARQL QUERY!"); 

			ret = kp.querySPARQL(tfSQ.getText());
			taMemo.append("Sent: " + kp.xmlTools.querySPARQL(tfSQ.getText()) + "\n");
			taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n");
			taMemo.append("SPARQLQuery confirmed:"+(this.xmlTools.isResponseConfirmed(ret)?"YES":"NO")+"\n");
			taMemo.append("\n*** SSAP message status:"+ret.Status +"\n");

			if(ret!=null)
			{
				SSAP_sparql_response myresponse = ret.sparqlquery_results;
				taMemo.append("\n-------------------------SPARQL QUERY--------------------------------\n");
				taMemo.append("It is "+myresponse.hasResults()+" that has results\n");
				taMemo.append("Variable names are: "+ myresponse.getVariablesNames()+"\n");
				taMemo.append("---------------------------------------------------------\n");
				taMemo.append("It is "+myresponse.hasLinks()+" that has links\n");
				taMemo.append("Links are: "+myresponse.getLinksHrefs()+"\n");	
				taMemo.append("-------------------------GET SIZE--------------------------------\n");
				taMemo.append("Number of variables = "+myresponse.getVariablesNames().size()+"\n");
				taMemo.append("SIZE (number of results) = "+myresponse.size()+"\n");
				taMemo.append("-------------------------GET Boolean--------------------------------\n");
				taMemo.append("It is "+myresponse.hasBooleans()+" that has boolean value\n");
				if(myresponse.hasBooleans())
				{
					taMemo.append("Boolean is "+myresponse.getBooleans().firstElement() +"\n");
				}
				taMemo.append("-------------------------GET RESULTS--------------------------------\n");
				taMemo.append("It is "+myresponse.hasResults()+" that has results\n");
				taMemo.append("Results are:\n ");
				taMemo.append(myresponse.print_as_string());
				history.append("sparql query s = " + tfSQ.getText() + "\n");

			}
			taHistory.setText(history.toString());

		}//bSPARQLquery
		else if( c == bSPARQLsubscription)
		{

			//TEST 
			taMemo.setText("");
			taMemo.append("\nSPARQL Subscription!"); 

			ret = kp.subscribeSPARQL(tfSQ.getText(), this);
			String subID = ret.subscription_id;
			taMemo.append("Sent: " + kp.xmlTools.subscribeSPARQL(tfSQ.getText()) + "\n");

			taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n");
			taMemo.append("SPARQLQuery confirmed:"+(this.xmlTools.isResponseConfirmed(ret)?"YES":"NO")+"\n");
			taMemo.append("\n*** SSAP message status: "+ret.Status +"\n");
			taMemo.append("\n*** subscriptionID:"+subID);
			ComboItemRenderable item = new ComboItemRenderable();
			item.setName(subID);
			item.setVisualize(tfSQ.getText());
			cbSUB.addItem(item);
			if(ret!=null)
			{
				SSAP_sparql_response myresponse = ret.sparqlquery_results;
				taMemo.append("\n-------------------------SPARQL QUERY--------------------------------\n");
				taMemo.append("It is "+myresponse.hasResults()+" that has results\n");
				taMemo.append("Variable names are: "+ myresponse.getVariablesNames()+"\n");
				taMemo.append("---------------------------------------------------------\n");
				taMemo.append("It is "+myresponse.hasLinks()+" that has links\n");
				taMemo.append("Links are: "+myresponse.getLinksHrefs()+"\n");	
				taMemo.append("-------------------------GET SIZE--------------------------------\n");
				taMemo.append("Number of variables = "+myresponse.getVariablesNames().size()+"\n");
				taMemo.append("SIZE (number of results) = "+myresponse.size()+"\n");
				taMemo.append("-------------------------GET RESULTS--------------------------------\n");
				taMemo.append("It is "+myresponse.hasResults()+" that has results\n");

				taMemo.append("Results are: ");
				taMemo.append(myresponse.print_as_string());
				history.append("sparql subscription s = " + tfSQ.getText() + "\n");
				taHistory.setText(history.toString());
			}


		}//bSPARQLquery
		else if( c == bSPARQLUpdate)
		{


			taMemo.setText("");
			taMemo.append("\nSPARQL Update!"); 

			ret = kp.update_sparql(tfSQ.getText());
			history.append("Sparql update s = " + tfSQ.getText() + "\n");

			taMemo.append("Sent: " + kp.xmlTools.update_sparql(tfSQ.getText()) + "\n");
			taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n");

			taMemo.append("SPARQLUpdate confirmed:"+(this.xmlTools.isResponseConfirmed(ret)?"YES":"NO")+"\n");
			taMemo.append("\n*** SSAP message status:"+ret.Status +"\n");

			taHistory.setText(history.toString());

		}//bSPARQLUpdate
		else if( c == bRULE)
		{
			taMemo.setText("");
			taMemo.append("\nPersistent SPARQL Update!"); 
			ret = kp.persistent_update(tfSQ.getText());
			taMemo.append("Sent: " + kp.xmlTools.persistent_update(tfSQ.getText()) + "\n");

			taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n");
			taMemo.append("SPARQLUpdate Persistent confirmed:"+(this.xmlTools.isResponseConfirmed(ret)?"YES":"NO")+"\n");
			taMemo.append("\n*** SSAP message status:"+ret.Status +"\n");

			String updateID = ret.update_id;
			ComboItemRenderable item = new ComboItemRenderable();
			item.setName(updateID);
			item.setVisualize(tfSQ.getText() );
			cbRULES.addItem(item);
			taMemo.append("Update ID:"+my_last_subscription+"\n");	  
			taMemo.append("\n*** SSAP message status:"+ret.Status +"\n");
			history.append("persistent sparql update s = " + tfSQ.getText() + "\n");

			taHistory.setText(history.toString());
		}//bRULE

		//DeleteRule
		else if( c == bDelRULE)
		{ taMemo.setText("");
		taMemo.append("Cancelling Rule ... waiting for the SIB answer...\n");
		ComboItemRenderable item = (ComboItemRenderable) cbRULES.getSelectedItem();
		ret = kp.cancel_persistent_update( item.getName());	  
		cbRULES.removeItemAt(cbRULES.getSelectedIndex());
		taMemo.append("Sent: " + kp.xmlTools.cancel_persistent_update( item.getName()) + "\n");

		taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"\n"); 
		//taMemo.append("Cancel Persistent Update confirmed:"+(this.xmlTools.isUnSubscriptionConfirmed(ret)?"YES":"NO")+"\n");
		taMemo.append("...ok:"+ret+"\n");
		taMemo.append("\n*** SSAP message status:"+ret.Status +"\n");
		history.append("cancel persistent update: " + item.getVisualize() + "\n");


		taHistory.setText(history.toString());
		}//bDEL_RULE

		else if( c == bInsert)
		{
			/*String buff = new String(":)");

	    	for(int i=0;i<15000;i++) buff=buff + "X";

	    	 taMemo.setText(""); 
	    	 taMemo.append(buff);

		      ret = kp.insert(
		    		     tfSO.getText()
			            ,tfPO.getText() 
			            ,buff
			            ,tfSOType.getText()
			            ,tfOOType.getText());
		      taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"\n");
	    	  if(true)return;*/

			taMemo.setText(""); 
//			ORIGINAL CODE
//			ret = kp.insert(
//					tfSN.getText()
//					,tfPN.getText() 
//					,tfON.getText()
//					,tfSNType.getText()
//					,tfONType.getText());
//			taMemo.append("Sent: " + kp.xmlTools.insert(
//					tfSN.getText()
//					,tfPN.getText() 
//					,tfON.getText()
//					,tfSNType.getText()
//					,tfONType.getText()) + "\n");
			//modify dy FG
			ret = kp.insert( 
					tfSN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfSN.getText()
							,tfPN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfPN.getText() 
									,tfON.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfON.getText()
											,tfSNType.getText()
											,tfONType.getText());	    
			taMemo.append("Sent: " + kp.xmlTools.insert( 
					tfSN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfSN.getText()
							,tfPN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfPN.getText() 
									,tfON.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfON.getText()
											,tfSNType.getText()
											,tfONType.getText()) + "\n");

			taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n"); 
			taMemo.append("\n*** SSAP message status:"+ret.Status  +"\n");
			history.append("rdf insert s = " + tfSN.getText() + " p = " + tfPN.getText() + " o = " + tfON.getText()+ " ot = " + tfONType.getText() + "\n" );


			taHistory.setText(history.toString());
		}//bInsert

		else if( c == bRemove)
		{ taMemo.setText("");
		ret = kp.remove( 
				tfSN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfSN.getText()
						,tfPN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfPN.getText() 
								,tfON.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfON.getText()
										,tfSNType.getText()
										,tfONType.getText());	    
		taMemo.append("Sent: " + kp.xmlTools.remove( 
				tfSN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfSN.getText()
						,tfPN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfPN.getText() 
								,tfON.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfON.getText()
										,tfSNType.getText()
										,tfONType.getText()) + "\n");

		taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n");
		taMemo.append("\n*** SSAP message status:"+ret.Status +"\n");
		history.append("rdf remove s = "  + tfSN.getText() + " p = " + tfPN.getText() + " o = " + tfON.getText()+ " ot = " + tfONType.getText() + "\n" );


		taHistory.setText(history.toString());

		}//bRemove

		else if( c == bSubscribe)
		{ taMemo.setText("");
		ret = kp.subscribeRDF(  
				tfSN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfSN.getText()
						,tfPN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfPN.getText() 
								,tfON.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfON.getText()
										,tfONType.getText(), this);	
		taMemo.append("Sent: " + kp.xmlTools.subscribeRDF(  
				tfSN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfSN.getText()
						,tfPN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfPN.getText() 
								,tfON.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfON.getText()
										,tfONType.getText()) + "\n");

		taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n"); 
		taMemo.append("Subscribe confirmed:"+(this.xmlTools.isResponseConfirmed(ret)?"YES":"NO")+"\n");	  
		my_last_subscription=ret.subscription_id;
		ComboItemRenderable item = new ComboItemRenderable();
		item.setName(my_last_subscription);
		item.setVisualize("s=" + tfSN.getText() + "  p=" + tfPN.getText() + "  o=" + tfON.getText()  + "  ot=" + tfONType.getText() );
		cbSUB.addItem(item);
		taMemo.append("Subscribe ID:"+my_last_subscription+"\n");	  
		taMemo.append("\n*** SSAP message status:"+ret.Status +"\n");
		history.append("rdf subscribe s = "  + tfSN.getText() + " p = " + tfPN.getText() + " o = " + tfON.getText()+ " ot = " + tfONType.getText() + "\n" );
		if(!this.xmlTools.isResponseConfirmed(ret))
		{
			return;
		}
		history.append("rdf query s = " + tfSN.getText() + " p = " + tfPN.getText() + " o = " + tfON.getText()+ " ot = " + tfONType.getText() + "\n" );
		Vector<Vector<String>> triples = null;

		if(ret!=null)
		{
			triples = ret.query_results;
		}

		if(triples!=null)
		{
			taMemo.append("Triple List:\n");
			for(int i=0; i<triples.size() ; i++ )
			{ Vector<String> t=triples.get(i);
			String st=  "  S:["+t.get(0)
					+"] P:["+t.get(1)
					+"] O:["+t.get(2)
					+"] Otype:["+t.get(3)+"]";

			taMemo.append(st+"\n");

			}//for(int j=0; i<triple.size() ; i++ )
		}  
		taHistory.setText(history.toString());
		}//bSubscribe

		else if( c == bUnsubscribe)
		{ taMemo.setText("");

		taMemo.append("UnSubscribe ... waiting for the SIB answer...\n");
		ComboItemRenderable item = (ComboItemRenderable) cbSUB.getSelectedItem();
		ret = kp.unsubscribe( item.getName());	 
		taMemo.append("Sent: " + kp.xmlTools.unsubscribe( item.getName()) + "\n");
		taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n"); 

		cbSUB.removeItemAt(cbSUB.getSelectedIndex());
		//taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"\n"); 
		//taMemo.append("UnSubscribe confirmed:"+(this.xmlTools.isUnSubscriptionConfirmed(ret)?"YES":"NO")+"\n");
		taMemo.append("...ok:"+ret+"\n");
		//taMemo.append("\n*** SSAP message status:"+xmlTools.getSSAPmsgStatus(ret));
		history.append("unsubscribe " + item.getVisualize() + "\n");

		taHistory.setText(history.toString());

		}//bUnsubscribe

		else if( c == bUpdate)
		{ taMemo.setText("");

		ret = kp.update( 
				tfSN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfSN.getText()
						,tfPN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfPN.getText() 
								,tfON.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any"  : tfON.getText()
										,tfSNType.getText()
										,tfONType.getText()
										,tfSO.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any":tfSO.getText()
												,tfPO.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any":tfPO.getText() 
														,tfOO.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any":tfOO.getText()
																,tfSOType.getText()
																,tfOOType.getText()
				);
		taMemo.append("Sent: " + kp.xmlTools.update( 
				tfSN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfSN.getText()
						,tfPN.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any" : tfPN.getText() 
								,tfON.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any"  : tfON.getText()
										,tfSNType.getText()
										,tfONType.getText()
										,tfSO.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any":tfSO.getText()
												,tfPO.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any":tfPO.getText() 
														,tfOO.getText().equals("*") ? "http://www.nokia.com/NRC/M3/sib#any":tfOO.getText()
																,tfSOType.getText()
																,tfOOType.getText()
				) + "\n");

		taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"\n"); 
		taMemo.append("\n*** SSAP message status:"+ret.Status +"\n");
		history.append("rdf update sn = " + tfSN.getText() + " p = " + tfPN.getText() + " o = " + tfON.getText()+ " ot = " + tfONType.getText()  + 
				"so = " +  tfSO.getText() + " p = " + tfPO.getText() + " o = " + tfOO.getText()+ " ot = " + tfOOType.getText() + "\n" );

		taHistory.setText(history.toString());
		}//bUpdate

		else if( c == bInsertProtection)
		{boolean success=false;
		Vector<String> properties = new Vector<String>();

		taMemo.setText("");

		if(   tfPN.getText().equals("")||tfSN.getText().equals("")
				|| tfPN.getText().equals("*")||tfSN.getText().equals("*"))
		{ taMemo.append( "ERROR!!!\nSubject or predicate are empty, plese check!!!\n"); 
		return;
		}

		properties.add(tfPN.getText());
		ret = kp.insertProtection(tfSN.getText(), properties); 
		success = xmlTools.isResponseConfirmed(ret);

		taMemo.append( "INSERT PROTECTION:SUCCESS:"+(success?"YES":"NO")
				+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"\n"); 
		taMemo.append("\n*** SSAP message status:"+ret.Status +"\n");

		}//bInsertProtection

		else if( c == bRemoveProtection)
		{boolean success=false;
		Vector<String> properties = new Vector<String>();
		taMemo.setText("");

		if(   tfPN.getText().equals("")||tfSN.getText().equals("")
				|| tfPN.getText().equals("*")||tfSN.getText().equals("*"))
		{ taMemo.append( "ERROR!!!\nSubject or predicate are empty, plese check!!!\n"); 
		return;
		}
		properties.add(tfPN.getText());
		ret = kp.removeProtection(tfSN.getText(), properties); 
		success = xmlTools.isResponseConfirmed(ret);

		taMemo.append( "REMOVE PROTECTION:SUCCESS:"+(success?"YES":"NO")
				+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"\n"); 
		taMemo.append("\n*** SSAP message status:"+ret.Status +"\n");

		}//bRemove

		else if( c == bSetKPID )
		{
			//Before...leave the Smart Space...
			taMemo.setText(""); 
			ret = kp.leave(); 
			taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n"); 
			taMemo.append("Leave confirmed:"+(this.xmlTools.isResponseConfirmed(ret)?"YES":"NO"));	    

			try {Thread.sleep(1000);} catch (InterruptedException e1) {e1.printStackTrace();System.out.print("Sleep failed:"+e1+"\n");}

			//set the new KP ID
			kp.setNodeID( tfKPID.getText() );

			//Join the Smart Space taMemo.setText(""); 
			ret = kp.join();  
			taMemo.append("Sent: " + kp.xmlTools.join() + "\n");

			taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n"); 
			taMemo.append("Join confirmed:"+(this.xmlTools.isResponseConfirmed(ret)?"YES":"NO"));
			taMemo.append("\n*** SSAP message status:"+ret.Status +"\n");

		}//else if(c==bSetKPID)
		else if(c==bViewHistory)
		{


			//ADD HERE A SCROLL BAR PANE
			//jpMemo.add(taMemo);
			//		JScrollPane sbrText;
			//		sbrText = new JScrollPane(taMemo);
			//		sbrText.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			//		jpMemo.add(  sbrText   );




			if((f1== null) || (!f1.isVisible()))
			{
				taHistory = new JTextArea();


				f1 = new JFrame(" - GUI History - ");


				f1.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						f1.setVisible(false);
						f1.dispose();
					}
				}
						);

				JPanel jpHistory = new JPanel(false);
				jpHistory.setLayout(new GridLayout(1,1));
				//JTextArea taHistory = new JTextArea();
				jpHistory.add(taHistory);
				JScrollPane sbrText1;
				sbrText1 = new JScrollPane(taHistory);
				sbrText1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				jpHistory.add(  sbrText1   );

				f1.setSize(new Dimension(900,200));
				f1.add("Center", jpHistory);    
				f1.add( jpHistory);
				// f.pack();
				f1.setVisible(true);

				//	jpHistory.setLayout( new BorderLayout() );
				//jpHistory.add( jpMain, BorderLayout.SOUTH);
				taHistory.append(history.toString());
			}
			else
			{
				taHistory.setText(history.toString());
			}

		}//else if(c==bViewHistory)

		//FG button action
		else if( c == bPOLICYAdd)
		{
			//TEST 
			taMemo.setText("");
			taMemo.append("\nSPARQL Policy!\n"); 

			ComboItemRenderable item = (ComboItemRenderable) cbPOLPM.getSelectedItem();
			ret = kp.policyAdd(tfSQ.getText(),tfA.getText(),item.getName());
			String polID = ret.policy_id;
			taMemo.append("Sent: " + kp.xmlTools.policyAdd(tfSQ.getText(),tfA.getText(),cbPOLPM.getName()) + "\n");

			taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n");
			taMemo.append("SPARQLQuery confirmed:"+(this.xmlTools.isResponseConfirmed(ret)?"YES":"NO")+"\n");
			taMemo.append("\n*** SSAP message status: "+ret.Status +"\n");
			taMemo.append("\n*** MessageType "+ret.MessageType +"\n");
			item = new ComboItemRenderable();
			item.setName(polID);
			item.setVisualize(tfSQ.getText().toString());
			if(!ret.MessageType.equals("ERROR")){
				taMemo.append("\n*** policyID:"+polID);
				cbPOL.setPreferredSize(new Dimension(200,25));
				cbPOL.addItem(item);
			}
//			if(ret!=null && xmlTools.isResponseConfirmed(ret)==true)
//			{
//				SSAP_sparql_response myresponse = ret.sparqlquery_results;
//				taMemo.append("\n-------------------------GET SIZE--------------------------------\n");
//				taMemo.append("Number of variables = "+myresponse.getVariablesNames().size()+"\n");
//				taMemo.append("SIZE (number of results) = "+myresponse.size()+"\n");
				
				
//			}
			history.append("sparql policy: " + tfSQ.getText() + "\n");
			taHistory.setText(history.toString());
		}//bPolicy

		//FG Policy Del
		else if( c == bPOLICYDel)
		{
			taMemo.setText("");
			taMemo.append("\nSPARQL Policy Remove!");

			taMemo.append("Policy remove ... waiting for the SIB answer...\n");
			taMemo.append("Dimension combo ... "+cbPOL.getItemCount()+"\n");
			if(cbPOL.getItemCount()>0){
				ComboItemRenderable item = (ComboItemRenderable) cbPOL.getSelectedItem();

				ret = kp.policyDel(item.getName());
				taMemo.append("Sent: " + kp.xmlTools.policyDel( item.getName()) + "\n");
				taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n"); 
				taMemo.append("\n*** SSAP message status: "+ret.Status +"\n");
				
				if(!ret.MessageType.equals("ERROR")){
					String polID = ret.policy_id;
					cbPOL.removeItemAt(cbPOL.getSelectedIndex());
					history.append("unpolicy " + item.getVisualize() + "\n");
					taMemo.append("Policy id da SIB"+polID.toString());
				}
			}
			else{
				ret = kp.policyDel("0");
				taMemo.append("SIB MESSAGE:\n"+ret+"\nKP-CORE MESSAGE:"+kp.getErrMess()+"("+kp.getErrID()+")\n");
				taMemo.append("\n*** SSAP message status: "+ret.Status +"\n");
			}
			taHistory.setText(history.toString());

		}//bPOLICYDEL

	}//public void actionPerformed(ActionEvent e)

	
	/*____________________________________________________________________*/
	public static void main(String args[])
	{    	 
		String ip="127.0.0.1",
				port="10010",
				ssname="X",
				s="*",
				p="*",
				o="*",
				kpid="DEAD-BEAF",
				def_kpid="DEAD-BEAF",
				autoJoin="false",
				goChanges="false";



		System.out.println("\n**************************************************");
		System.out.println("*  Available parameters (with default values):");
		System.out.println("*      -ip "+ip);
		System.out.println("*      -port "+port);
		System.out.println("*      -ssname "+ssname);
		System.out.println("*      -s "+s);
		System.out.println("*      -p "+p);
		System.out.println("*      -o "+o);
		System.out.println("*      -kpid "+"<random UUID>");
		System.out.println("*      -autoJoin "+autoJoin+" [true|false]");
		System.out.println("**************************************************\n");

		if (args.length > 0) 
			for(int i=0; i<args.length ;i++)
			{
				if( args[i].equals("-ip") ) {ip=args[++i];  goChanges="true";}
				else if( args[i].equals("-port") ) {port=args[++i]; goChanges="true";}
				else if( args[i].equals("-ssname") ) {ssname=args[++i]; goChanges="true";}
				else if( args[i].equals("-s") ) {s=args[++i];}
				else if( args[i].equals("-p") ) {p=args[++i];}
				else if( args[i].equals("-o") ) {o=args[++i];}
				else if( args[i].equals("-kpid") ) {kpid=args[++i];}
				else if( args[i].equals("-autoJoin") ) {autoJoin=args[++i];}
				else System.out.println("Parameter not recognized: "+args[i]);     
			}//if (args.length > 0)


		KP_GUI kpgui = new KP_GUI();
		Frame   f = new Frame(" - SOFIA KP - ");


		f.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		}
				);

		f.setSize(new Dimension(700,200));
		f.add("Center", kpgui);        //
		// f.pack();
		f.setVisible(true);

		/*______________________*/
		//kpgui.tfIP.setText("127.0.0.1");        
		//kpgui.cbIPList.setSelectedItem("mml.arces.unibo.it");
		kpgui.tfIP.setText(ip);        
		kpgui.cbIPList.setSelectedItem(ip);


		kpgui.tfPort.setText(port);
		kpgui.tfSSN.setText(ssname);

		/** /
        kpgui.tfSO.setText("*");
        kpgui.tfPO.setText("*");
        kpgui.tfOO.setText("*");
/ **/
		kpgui.tfSN.setText(s);
		kpgui.tfPN.setText(p);
		kpgui.tfON.setText(o);
		/**/        
		kpgui.tfSNType.setText("uri");
		kpgui.tfSOType.setText("uri");
		kpgui.tfONType.setText("uri");
		kpgui.tfOOType.setText("uri");
		/**/
		kpgui.tfKPID.setText(kpid);
		/*______________________*/


		if(!kpid.equals(def_kpid))kpgui.user_kp_id=kpid;
		if(goChanges.equals("true"))
			kpgui.actionPerformed( new ActionEvent( kpgui.bGo,0,null) );
		if(autoJoin.equals("true"))
		{ if(!goChanges.equals("true"))
			kpgui.actionPerformed( new ActionEvent( kpgui.bGo,0,null) );
		kpgui.actionPerformed( new ActionEvent( kpgui.bJoin,0,null) );
		}

		kpgui.taMemo.append("\n**************************************************\n");
		kpgui.taMemo.append("*  KP parameters:\n");
		kpgui.taMemo.append("*      -ip "+ip+"\n");
		kpgui.taMemo.append("*      -port "+port+"\n");
		kpgui.taMemo.append("*      -ssname "+ssname+"\n");
		kpgui.taMemo.append("*      -s "+s+"\n");
		kpgui.taMemo.append("*      -p "+p+"\n");
		kpgui.taMemo.append("*      -o "+o+"\n");
		if(goChanges.equals("true"))
			kpgui.taMemo.append("*      -kpid "+kpgui.kp.nodeID+"\n");
		kpgui.taMemo.append("**************************************************\n");


	}//public static void main(String args[])

	private class ComboItemRenderable
	{
		String name= "";
		String visualize = "";

		protected String getName() {
			return name;
		}
		protected void setName(String name) {
			this.name = name;
		}
		protected String getVisualize() {
			return visualize;
		}
		protected void setVisualize(String visualize) {
			this.visualize = visualize;
		}

		public String toString()
		{
			return this.getVisualize();
		}
	}

	@Override
	public void kpic_RDFEventHandler(Vector<Vector<String>> newTriples,
			Vector<Vector<String>> oldTriples, String indSequence, String subID) {

		String temp = "\n Notif. " + indSequence + " id = " + subID +"\n";
		for(int i = 0; i < newTriples.size(); i++ )
		{
			temp+="New triple s =" + newTriples.elementAt(i).elementAt(0) + "  + predicate" + newTriples.elementAt(i).elementAt(1) + "object =" + newTriples.elementAt(i).elementAt(2) +"\n";
		}
		for(int i = 0; i < oldTriples.size(); i++ )
		{
			temp+="Obsolete triple s =" + oldTriples.elementAt(i).elementAt(0) + "  + predicate" + oldTriples.elementAt(i).elementAt(1) + "object =" + oldTriples.elementAt(i).elementAt(2) + "\n";
		}
		System.out.println(temp);
		taMemo.append(temp);
		taMemo.repaint();



		// TODO Auto-generated method stub

	}

	@Override
	public void kpic_SPARQLEventHandler(SSAP_sparql_response newResults,
			SSAP_sparql_response oldResults, String indSequence, String subID) {
		// TODO Auto-generated method stub
		taMemo.append("\n Notif. " + indSequence + " id = " + subID +"\n");
		if (newResults != null)
		{
			System.out.println("new: \n " + newResults.print_as_string());
			taMemo.append("new: \n " + newResults.print_as_string());
			taMemo.repaint();
		}
		if (oldResults != null)
		{
			System.out.println("obsolete: \n " + oldResults.print_as_string());
			taMemo.append("obsolete: \n " + oldResults.print_as_string());
			taMemo.repaint();
		}
	}

	@Override
	public void kpic_UnsubscribeEventHandler(String sub_ID) {
		// TODO Auto-generated method stub
		System.out.println("Unsubscribed " + sub_ID);
		taMemo.append("Unsubscribed " + sub_ID);

	}

	@Override
	public void kpic_ExceptionEventHandler(Throwable SocketException) {
		// TODO Auto-generated method stub

		System.out.println("Exception in subscription handler " + SocketException.toString());
		taMemo.append("Exception in subscription handler " + SocketException.toString());
		taMemo.repaint();

	}
}
