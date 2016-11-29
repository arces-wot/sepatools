package arces.unibo.SEPA;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JTextArea;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import java.awt.Canvas;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import arces.unibo.SEPA.Logger.VERBOSITY;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.border.TitledBorder;
import java.awt.Checkbox;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.ItemEvent;
import javax.swing.border.EtchedBorder;

public class Dashboard implements GenericClient.Notification {
	private String tag ="Dashboard";
	
	Properties appProperties = new Properties();
	
	private DefaultTableModel namespacesDM;
	private String namespacesHeader[] = new String[] {"Prefix", "URI"};
	
	private BindingsTableModel bindingsDM = new BindingsTableModel();
	private BindingsRender bindingsRender = new BindingsRender();
	
	private ForcedBindingsTableModel updateForcedBindingsDM = new ForcedBindingsTableModel();
	private ForcedBindingsTableModel subscribeForcedBindingsDM = new ForcedBindingsTableModel();
	
	private DefaultListModel<String> updateListDM = new DefaultListModel<String>();
	private DefaultListModel<String> subscribeListDM = new DefaultListModel<String>();
	
	private DefaultTableModel propertiesDM;
	private String propertiesHeader[] = new String[] {"Property", "Domain","Range","Comment"};
	
	private GenericClient kp;
	private String subID = "";
	private JFrame frmSepaDashboard;
	
	static Dashboard window;
	private JTextField textFieldIP;
	private JTextField textFieldPORT;
	private JTextField txtFieldName;
	private JTable updateForcedBindings;
	private JTable subscribeForcedBindings;
	private JTable bindingsResultsTable;
	private JTable namespacesTable;
	private JTextField prefix;
	private JTextField namespace;

	private JLabel lblInfo;
	private JTextArea SPARQLUpdate;
	private JTextArea SPARQLSubscribe;
	
	private JButton btnUpdate;
	private JButton btnSubscribe;
	private JButton btnQuery;
	private JLabel lblInfoVisualizer;
	private JButton btnLoadRdfxml;
	private JButton btnLoadRdfxmlDataset;
	private Checkbox qNameCheckbox;
	private JTree classTree;
	
	SPARQLApplicationProfile appProfile = new SPARQLApplicationProfile();
	
	//Visualizer
	private SPARQLApplicationProfile vizProfile = new SPARQLApplicationProfile();
	private ClassMonitor classMonitor;
	private PropertyMonitor propertyMonitor;
	private JTable propertiesTable;
	
	class PropertyMonitor extends Consumer {
		private HashMap<String,OWLClassNodeModel> domainTree = new HashMap<String,OWLClassNodeModel>();
		private HashMap<String,OWLClassNodeModel> rangeTree = new HashMap<String,OWLClassNodeModel>();
		OWLClassNodeModel root;
		OWLClassNodeModel domain; 
		OWLClassNodeModel range;
		
		public PropertyMonitor(SPARQLApplicationProfile appProfile, String subscribeID) {
			super(appProfile, subscribeID);
		}

		@Override
		public void notify(BindingsResults notify) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void notifyAdded(ArrayList<Bindings> bindingsResults) {
			for (Bindings binding : bindingsResults) {
				String propertyURI = "";
				String domainURI = "";
				String rangeURI = "";
				String comment = "";
				
				if (binding.getBindingValue("?property") != null) propertyURI = appProfile.qName(binding.getBindingValue("?property").getValue());
				if (binding.getBindingValue("?domain") != null) domainURI = appProfile.qName(binding.getBindingValue("?domain").getValue());	
				if (binding.getBindingValue("?range") != null) rangeURI = appProfile.qName(binding.getBindingValue("?range").getValue());
				if (binding.getBindingValue("?comment") != null) comment = binding.getBindingValue("?comment").getValue();
				
				if (propertyURI.equals("")) continue; 
				
				propertiesDM.addRow(new String[]{propertyURI,domainURI,rangeURI,comment});
			}
		}

		@Override
		public void notifyRemoved(ArrayList<Bindings> bindingsResults) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void notifyFirst(ArrayList<Bindings> bindingsResults) {
			propertiesTable.selectAll();
			propertiesTable.clearSelection();
			
			notifyAdded(bindingsResults);
		}
		
	}
	
	class ClassMonitor extends Consumer {
		private HashMap<String,OWLClassNodeModel> treeMap = new HashMap<String,OWLClassNodeModel>();
		OWLClassNodeModel root;
		
		public ClassMonitor(SPARQLApplicationProfile appProfile, String subscribeID) {
			super(appProfile, subscribeID);
		}

		@Override
		public void notify(BindingsResults notify) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void notifyAdded(ArrayList<Bindings> bindingsResults) {
			for (Bindings binding : bindingsResults) {
				String classURI = null;
				String classLabel = null;
				String classComment = null;
				String subclassURI = null;
				
				if (binding.getBindingValue("?class") != null) classURI = appProfile.qName(binding.getBindingValue("?class").getValue());
				if (binding.getBindingValue("?subclass") != null) subclassURI = appProfile.qName(binding.getBindingValue("?subclass").getValue());	
				if (binding.getBindingValue("?label") != null) classLabel = binding.getBindingValue("?label").getValue();
				if (binding.getBindingValue("?comment") != null) classComment = binding.getBindingValue("?comment").getValue();
				
				OWLClassNodeModel classNode = null;
				OWLClassNodeModel subclassNode = null;
				
				if (classURI != null) {
					if (!treeMap.containsKey(classURI)) {
						classNode = new OWLClassNodeModel(classURI);
						root.add(classNode);
						treeMap.put(classURI, classNode);
					}
					else {
						classNode = treeMap.get(classURI);
					}
					
					//Label & comment
					if (classLabel != null) {
						classNode.setLabel(classLabel);
						if (classComment != null) classNode.setComment("URI: "+classURI+"\n\n"+classComment);
						else classNode.setComment("URI: "+classURI);
					}
					else if (classComment != null) classNode.setComment(classComment);
				}
				
				if (subclassURI != null) {
					if (!treeMap.containsKey(subclassURI)){
						subclassNode = new OWLClassNodeModel(subclassURI);
						treeMap.put(subclassURI, subclassNode);
					}
					else subclassNode = treeMap.get(subclassURI);
						
					classNode.add(subclassNode);	
				}
			}
		}

		@Override
		public void notifyRemoved(ArrayList<Bindings> bindingsResults) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void notifyFirst(ArrayList<Bindings> bindingsResults) {		
			root = new OWLClassNodeModel("owl:Thing");
			classTree.setModel(null);
			treeMap.clear();
			
			notifyAdded(bindingsResults);
			
			classTree.setModel(new DefaultTreeModel(root));
		}
		
	}
	
	static class OWLClassNodeModel extends DefaultMutableTreeNode {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6299628084428311514L;

		private String label = null;
		private String uri = null;
		private String comment = null;
		
		public OWLClassNodeModel(String uri) {
			super(uri);
			this.uri = uri;
		}
		
		@Override
		public String toString() {
			if (label != null) return label;
			return uri;
		}
		
		public String getComment() {
			if (comment == null) return "";
			return comment;
		}
		
		public void setLabel(String label) {
			this.label = label;
		}
		
		public void setComment(String comment) {
			this.comment = comment;
		}
		
	}
	
	static class ForcedBindingsTableModel extends AbstractTableModel {
		/**
		 * 
		 */
		private static final long serialVersionUID = -8524602022439421892L;
		
		ArrayList<String[]> rowValues = new ArrayList<String[]>();
		ArrayList<Boolean> rowTypes = new ArrayList<Boolean>();
		ArrayList<String> columns = new ArrayList<String>();
		
		public void clearBindings() {
			rowValues.clear();
			rowTypes.clear();
			
			super.fireTableDataChanged();
		}
		
		public void addBindings(String variable,boolean literal) {
			rowValues.add(new String[] {variable,""});
			rowTypes.add(literal);
			
			super.fireTableDataChanged();
		}
		
		public ForcedBindingsTableModel() {
			columns.add("Variable");
			columns.add("Value");
			columns.add("Literal");
		}
		
		@Override
		public int getRowCount() {
			return rowValues.size();
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0 || columnIndex == 1) return rowValues.get(rowIndex)[columnIndex];
			return rowTypes.get(rowIndex);
		}
		
	    @Override
	    public Class<?> getColumnClass(int columnIndex) {
	    	if (columnIndex == 0 || columnIndex == 1) return String.class;
	    	return Boolean.class;
	    }
	    
		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex < getColumnCount()) return columns.get(columnIndex);
			return null;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (columnIndex == 1) return true;
			return false;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 1) {
				String[] currentValue = rowValues.get(rowIndex);
				currentValue[1] = (String) aValue;
				rowValues.set(rowIndex, currentValue);
			}
			if (columnIndex == 2) rowTypes.set(rowIndex, (Boolean) aValue);
		}

		@Override
		public void addTableModelListener(TableModelListener l) {
			super.addTableModelListener(l);
		}

		@Override
		public void removeTableModelListener(TableModelListener l) {
			super.removeTableModelListener(l);
		}
	}
	
	static class BindingsTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 2698789913874225961L;
		
		ArrayList<Bindings> rows = new ArrayList<Bindings>();
		ArrayList<String> columns = new ArrayList<String>();
		
		public void clear() {
			columns.clear();
			rows.clear();
			super.fireTableStructureChanged();
			super.fireTableDataChanged();
		}
		
		public void setResults(BindingsResults res) {						
			if (res == null) return;
			
			if (!columns.containsAll(res.getVariables()) || columns.size() != res.getVariables().size()) {
				columns.clear();
				columns.addAll(res.getVariables());
				super.fireTableStructureChanged();
			}			
			
			if (res.getAddedBindings() != null) rows.addAll(res.getAddedBindings());
			if (res.getRemovedBindings() != null) rows.addAll(res.getRemovedBindings());
				
			super.fireTableDataChanged();
		}
		
	    @Override
	    public Class<?> getColumnClass(int columnIndex) {
	    	return BindingValue.class;
	    }

		@Override
		public int getRowCount() {
			return rows.size();
		}

		@Override
		public int getColumnCount() {
			return columns.size();
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex < getColumnCount()) return columns.get(columnIndex);
			return null;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			BindingValue ret = null;
			if (rowIndex < getRowCount() && columnIndex < getColumnCount()) {
				ret = rows.get(rowIndex).getBindingValue(columns.get(columnIndex));
			}
			return ret;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			super.setValueAt(aValue, rowIndex, columnIndex);
		}

		@Override
		public void addTableModelListener(TableModelListener l) {
			super.addTableModelListener(l);
		}

		@Override
		public void removeTableModelListener(TableModelListener l) {
			super.removeTableModelListener(l);
		}
	}
	
	static class BindingsRender extends DefaultTableCellRenderer {		
		private static final long serialVersionUID = 3932800852596396532L;
		
		DefaultTableModel namespaces;
		private boolean showAsQname = true;
		
		public BindingsRender() { super(); }
	    
		public void setNamespaces(DefaultTableModel namespaces) {
			this.namespaces = namespaces;
		}
		public void showAsQName(boolean set) {
			showAsQname = set;
		}
		
		private String qName(String uri) {
			if (namespaces == null) return uri;
			if (uri == null) return null;
			for (int row=0; row < namespaces.getRowCount();row++) {
				String prefix = namespaces.getValueAt(row, 0).toString();
				String ns = namespaces.getValueAt(row, 1).toString();
				if (uri.startsWith(ns)) return uri.replace(ns, prefix+":");
			}
			return uri;
		}
		
		@Override
		public void setValue(Object value) {
			if (value == null) return;
			
			BindingValue binding = (BindingValue) value;
			
			if (binding.isLiteral()) {
	    		setFont(new Font(null,Font.BOLD,12));
	    		setForeground(Color.RED);
	    	}
			else {
				setFont(new Font(null,Font.PLAIN,12));
	    		setForeground(Color.BLACK);	
			}
			if (binding.isAdded()) setBackground(Color.WHITE);
			else setBackground(Color.LIGHT_GRAY);
		}
		
		@Override
	    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	    	super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	    	
	    	BindingValue binding = (BindingValue) value;
	    	
	    	if (binding == null) {
	    		setText("");
	    		return this;
	    	}
	    	if (binding.getValue() == null) {
	    		setText("");
	    		return this;
	    	}
	    	
			//Render as qname or URI
			if (showAsQname) setText(qName(binding.getValue()));
			else setText(binding.getValue());
	    	
	    	return this;
	    }
	}
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new Dashboard();
					window.frmSepaDashboard.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Dashboard() {
		loadProperties();
		
		initialize();
		
		vizProfile.load("Visualizer.sap");
		classMonitor = new ClassMonitor(vizProfile,"CLASSES");
		propertyMonitor = new PropertyMonitor(vizProfile,"PROPERTIES");
	}
	
	private void loadProperties() {
		FileInputStream in = null;
		try {
			in = new FileInputStream("resources/config.properties");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			appProperties.load(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	private void storeProperties() {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream("resources/config.properties");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			appProperties.store(out,"Last used values");
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	private class DashboardFileFilter extends FileFilter {
		private ArrayList<String> extensions = new ArrayList<String>();
		private String title = "Title";
		
		public DashboardFileFilter(String title,String ext) {
			super();
			extensions.add(ext);
			this.title = title;
		}
		
		public DashboardFileFilter(String title,ArrayList<String> ext) {
			super();
			extensions.addAll(ext);
			this.title = title;
		}
		
		@Override
		public boolean accept(File f) {
			for (String ext : extensions) if (f.getName().contains(ext)) return true;
			return false;
		}

		@Override
		public String getDescription() {
			return title;
		}
		
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {		
		namespacesDM = new DefaultTableModel(0, 0){
			/**
			 * 
			 */
			private static final long serialVersionUID = 6788045463932990156L;
		
			@Override
			public boolean isCellEditable(int row, int column)
		    {   
		        return false;
		    }
		};
		namespacesDM.setColumnIdentifiers(namespacesHeader);		
		
		propertiesDM = new DefaultTableModel(0, 0){
		
			/**
			 * 
			 */
			private static final long serialVersionUID = -5161490469556412655L;

			@Override
			public boolean isCellEditable(int row, int column)
		    {   
		        return false;
		    }
		};
		propertiesDM.setColumnIdentifiers(propertiesHeader);
		
		frmSepaDashboard = new JFrame();
		frmSepaDashboard.setTitle("SEPA Dashboard");
		frmSepaDashboard.setBounds(100, 100, 925, 768);
		frmSepaDashboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmSepaDashboard.getContentPane().setLayout(new BorderLayout(0, 0));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmSepaDashboard.getContentPane().add(tabbedPane);
		
		JPanel SPARQLPanel = new JPanel();
		SPARQLPanel.setBorder(null);
		tabbedPane.addTab("SPARQL", null, SPARQLPanel, null);
		GridBagLayout gbl_SPARQLPanel = new GridBagLayout();
		gbl_SPARQLPanel.columnWidths = new int[]{0, 0};
		gbl_SPARQLPanel.rowHeights = new int[] {48, 45, 96, 69};
		gbl_SPARQLPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_SPARQLPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 1.0};
		SPARQLPanel.setLayout(gbl_SPARQLPanel);
		
		JPanel configuration = new JPanel();
		configuration.setBorder(new TitledBorder(null, "Configuration", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_configuration = new GridBagConstraints();
		gbc_configuration.insets = new Insets(0, 0, 5, 0);
		gbc_configuration.fill = GridBagConstraints.BOTH;
		gbc_configuration.gridx = 0;
		gbc_configuration.gridy = 0;
		SPARQLPanel.add(configuration, gbc_configuration);
		GridBagLayout gbl_configuration = new GridBagLayout();
		gbl_configuration.columnWidths = new int[]{37, 165, 38, 28, 33, 78, 47, 37, 130, 0};
		gbl_configuration.rowHeights = new int[]{26, 0};
		gbl_configuration.columnWeights = new double[]{0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_configuration.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		configuration.setLayout(gbl_configuration);
		
		JLabel lblIp = new JLabel("IP");
		GridBagConstraints gbc_lblIp = new GridBagConstraints();
		gbc_lblIp.insets = new Insets(0, 0, 0, 5);
		gbc_lblIp.gridx = 0;
		gbc_lblIp.gridy = 0;
		configuration.add(lblIp, gbc_lblIp);
		
		textFieldIP = new JTextField();
		textFieldIP.setText(appProperties.getProperty("ip"));
		GridBagConstraints gbc_textFieldIP = new GridBagConstraints();
		gbc_textFieldIP.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldIP.insets = new Insets(0, 0, 0, 5);
		gbc_textFieldIP.gridx = 1;
		gbc_textFieldIP.gridy = 0;
		configuration.add(textFieldIP, gbc_textFieldIP);
		textFieldIP.setColumns(10);
		
		JLabel lblPort = new JLabel("PORT");
		GridBagConstraints gbc_lblPort = new GridBagConstraints();
		gbc_lblPort.anchor = GridBagConstraints.WEST;
		gbc_lblPort.insets = new Insets(0, 0, 0, 5);
		gbc_lblPort.gridx = 2;
		gbc_lblPort.gridy = 0;
		configuration.add(lblPort, gbc_lblPort);
		
		textFieldPORT = new JTextField();
		textFieldPORT.setText(appProperties.getProperty("port"));
		GridBagConstraints gbc_textFieldPORT = new GridBagConstraints();
		gbc_textFieldPORT.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldPORT.anchor = GridBagConstraints.NORTH;
		gbc_textFieldPORT.insets = new Insets(0, 0, 0, 5);
		gbc_textFieldPORT.gridx = 3;
		gbc_textFieldPORT.gridy = 0;
		configuration.add(textFieldPORT, gbc_textFieldPORT);
		textFieldPORT.setColumns(10);
		
		JLabel lblName = new JLabel("NAME");
		GridBagConstraints gbc_lblName = new GridBagConstraints();
		gbc_lblName.anchor = GridBagConstraints.WEST;
		gbc_lblName.insets = new Insets(0, 0, 0, 5);
		gbc_lblName.gridx = 4;
		gbc_lblName.gridy = 0;
		configuration.add(lblName, gbc_lblName);
		
		txtFieldName = new JTextField();
		txtFieldName.setText(appProperties.getProperty("name"));
		GridBagConstraints gbc_txtFieldName = new GridBagConstraints();
		gbc_txtFieldName.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtFieldName.anchor = GridBagConstraints.NORTH;
		gbc_txtFieldName.insets = new Insets(0, 0, 0, 5);
		gbc_txtFieldName.gridx = 5;
		gbc_txtFieldName.gridy = 0;
		configuration.add(txtFieldName, gbc_txtFieldName);
		txtFieldName.setColumns(10);
		
		JButton btnJoin = new JButton("Join");
		btnJoin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (btnJoin.getText().equals("Join")) {
					kp = new GenericClient(textFieldIP.getText(),Integer.parseInt(textFieldPORT.getText()),txtFieldName.getText(),window);
					if (kp.join()) {
						btnJoin.setText("Leave");
						storeProperties();
						btnUpdate.setEnabled(true);
						btnSubscribe.setEnabled(true);
						btnQuery.setEnabled(true);
						textFieldIP.setEnabled(false);
						textFieldPORT.setEnabled(false);
						txtFieldName.setEnabled(false);
						btnLoadRdfxmlDataset.setEnabled(true);
						lblInfo.setText("Joined @ "+textFieldIP.getText()+":"+textFieldPORT.getText()+" \""+txtFieldName.getText()+"\"");
					}
					else {
						lblInfo.setText("Not joined...please check the connection parameters");
					}
				}
				else {
					if (kp.leave()) {
						btnJoin.setText("Join");
						btnUpdate.setEnabled(false);
						btnSubscribe.setEnabled(false);
						btnQuery.setEnabled(false);
						textFieldIP.setEnabled(true);
						textFieldPORT.setEnabled(true);
						txtFieldName.setEnabled(true);
						btnLoadRdfxmlDataset.setEnabled(true);
						lblInfo.setText("Left @ "+textFieldIP.getText()+":"+textFieldPORT.getText()+" \""+txtFieldName.getText()+" \"");
					}
				}
			}
		});
		GridBagConstraints gbc_btnJoin = new GridBagConstraints();
		gbc_btnJoin.insets = new Insets(0, 0, 0, 5);
		gbc_btnJoin.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnJoin.anchor = GridBagConstraints.NORTH;
		gbc_btnJoin.gridx = 6;
		gbc_btnJoin.gridy = 0;
		configuration.add(btnJoin, gbc_btnJoin);
		
		JButton btnLoadXmlProfile = new JButton("Load SAP profile");
		btnLoadXmlProfile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final JFileChooser fc = new JFileChooser(appProperties.getProperty("appProfile"));
				DashboardFileFilter filter = new DashboardFileFilter("SAP Profile (.sap)",".sap");
				fc.setFileFilter(filter);
				int returnVal = fc.showOpenDialog(frmSepaDashboard);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					String fileName = fc.getSelectedFile().getPath();
					appProperties.setProperty("appProfile", fileName);
					
					SPARQLSubscribe.setText("");
					SPARQLUpdate.setText("");
					namespacesDM.getDataVector().clear();
					updateListDM.clear();
					subscribeListDM.clear();
					updateForcedBindingsDM.clearBindings();
					subscribeForcedBindingsDM.clearBindings();
					
					if(appProfile.load(fileName)) {
						storeProperties();
						frmSepaDashboard.setTitle("SEPA Dashboard" + " - " + fileName);
						//Loading namespaces
						for(String prefix : appProfile.getPrefixes()) {
							Vector<String> row = new Vector<String>();
							row.add(prefix);
							row.addElement(appProfile.getNamespaceURI(prefix));
							namespacesDM.addRow(row);
						}
						//Loading updates
						for(String update : appProfile.getUpdateIds()) {
							updateListDM.addElement(update);
						}
						//Loading updates
						for(String subscribe : appProfile.getSubscribeIds()) {
							subscribeListDM.addElement(subscribe);
						}
					}
				}
			}
		});
		GridBagConstraints gbc_btnLoadXmlProfile = new GridBagConstraints();
		gbc_btnLoadXmlProfile.insets = new Insets(0, 0, 0, 5);
		gbc_btnLoadXmlProfile.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnLoadXmlProfile.anchor = GridBagConstraints.NORTH;
		gbc_btnLoadXmlProfile.gridx = 7;
		gbc_btnLoadXmlProfile.gridy = 0;
		configuration.add(btnLoadXmlProfile, gbc_btnLoadXmlProfile);
		
		btnLoadRdfxmlDataset = new JButton("Load RDF/XML ontology");
		btnLoadRdfxmlDataset.addActionListener(new ActionListener() {
			private String readFile(String fileName) throws IOException {
			    BufferedReader br = new BufferedReader(new FileReader(fileName));
			    try {
			        StringBuilder sb = new StringBuilder();
			        String line = br.readLine();

			        while (line != null) {
			            sb.append(line);
			            sb.append("\n");
			            line = br.readLine();
			        }
			        return sb.toString();
			    } finally {
			        br.close();
			    }
			}
			
			public void actionPerformed(ActionEvent e) {
				final JFileChooser fc = new JFileChooser(appProperties.getProperty("RDFXMLDataset"));
				ArrayList<String> extensions = new ArrayList<String>();
				extensions.add(".owl");
				extensions.add(".rdf");
				extensions.add(".rdfs");
				extensions.add(".xml");
				DashboardFileFilter filter = new DashboardFileFilter("RDF/XML Ontology (.xml,.owl,.rdf,.rdfs)",extensions);
				fc.setFileFilter(filter);
				int returnVal = fc.showOpenDialog(frmSepaDashboard);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					String fileName = fc.getSelectedFile().getPath();
					appProperties.setProperty("RDFXMLDataset", fileName);
					storeProperties();
					String dataset;
					
					try {
						dataset = readFile(fileName);
					} catch (IOException e1) {
						e1.printStackTrace();
						Logger.log(VERBOSITY.ERROR, tag, "Error reading RDF/XML file: "+fileName);
						return;
					}
					
					long start = System.currentTimeMillis();
					if(kp.loadRDFXMLOntology(dataset)) {
						long stop = System.currentTimeMillis();
						lblInfo.setText(fileName+" loaded in "+(stop-start)+ " ms");
					}
				}
			}
		});
		btnLoadRdfxmlDataset.setEnabled(false);
		GridBagConstraints gbc_btnLoadRdfxmlDataset = new GridBagConstraints();
		gbc_btnLoadRdfxmlDataset.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnLoadRdfxmlDataset.anchor = GridBagConstraints.NORTH;
		gbc_btnLoadRdfxmlDataset.gridx = 8;
		gbc_btnLoadRdfxmlDataset.gridy = 0;
		configuration.add(btnLoadRdfxmlDataset, gbc_btnLoadRdfxmlDataset);
		
		JPanel namespaces = new JPanel();
		namespaces.setBorder(new TitledBorder(null, "Namespaces", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_namespaces = new GridBagConstraints();
		gbc_namespaces.fill = GridBagConstraints.BOTH;
		gbc_namespaces.insets = new Insets(0, 0, 5, 0);
		gbc_namespaces.gridx = 0;
		gbc_namespaces.gridy = 1;
		SPARQLPanel.add(namespaces, gbc_namespaces);
		GridBagLayout gbl_namespaces = new GridBagLayout();
		gbl_namespaces.columnWidths = new int[]{0, 0, 0};
		gbl_namespaces.rowHeights = new int[]{43, 0};
		gbl_namespaces.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_namespaces.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		namespaces.setLayout(gbl_namespaces);
		
		JScrollPane scrollPane_4 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_4 = new GridBagConstraints();
		gbc_scrollPane_4.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPane_4.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_4.gridx = 0;
		gbc_scrollPane_4.gridy = 0;
		namespaces.add(scrollPane_4, gbc_scrollPane_4);
		
		namespacesTable = new JTable(namespacesDM);
		scrollPane_4.setViewportView(namespacesTable);
		
		JPanel panel_2 = new JPanel();
		GridBagConstraints gbc_panel_2 = new GridBagConstraints();
		gbc_panel_2.fill = GridBagConstraints.BOTH;
		gbc_panel_2.gridx = 1;
		gbc_panel_2.gridy = 0;
		namespaces.add(panel_2, gbc_panel_2);
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.columnWidths = new int[]{0, 0, 0};
		gbl_panel_2.rowHeights = new int[]{43, 0, 0, 0};
		gbl_panel_2.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_panel_2.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_2.setLayout(gbl_panel_2);
		
		JLabel lblPrefix = new JLabel("Prefix");
		GridBagConstraints gbc_lblPrefix = new GridBagConstraints();
		gbc_lblPrefix.anchor = GridBagConstraints.EAST;
		gbc_lblPrefix.insets = new Insets(0, 0, 5, 5);
		gbc_lblPrefix.gridx = 0;
		gbc_lblPrefix.gridy = 0;
		panel_2.add(lblPrefix, gbc_lblPrefix);
		
		prefix = new JTextField();
		GridBagConstraints gbc_prefix = new GridBagConstraints();
		gbc_prefix.fill = GridBagConstraints.HORIZONTAL;
		gbc_prefix.insets = new Insets(0, 0, 5, 0);
		gbc_prefix.gridx = 1;
		gbc_prefix.gridy = 0;
		panel_2.add(prefix, gbc_prefix);
		prefix.setColumns(10);
		
		JLabel lblSuffix = new JLabel("Namespace");
		GridBagConstraints gbc_lblSuffix = new GridBagConstraints();
		gbc_lblSuffix.anchor = GridBagConstraints.EAST;
		gbc_lblSuffix.insets = new Insets(0, 0, 5, 5);
		gbc_lblSuffix.gridx = 0;
		gbc_lblSuffix.gridy = 1;
		panel_2.add(lblSuffix, gbc_lblSuffix);
		
		namespace = new JTextField();
		GridBagConstraints gbc_namespace = new GridBagConstraints();
		gbc_namespace.insets = new Insets(0, 0, 5, 0);
		gbc_namespace.fill = GridBagConstraints.HORIZONTAL;
		gbc_namespace.gridx = 1;
		gbc_namespace.gridy = 1;
		panel_2.add(namespace, gbc_namespace);
		namespace.setColumns(10);
		
		JButton btnAdd = new JButton("Add");
		GridBagConstraints gbc_btnAdd = new GridBagConstraints();
		gbc_btnAdd.gridx = 1;
		gbc_btnAdd.gridy = 2;
		panel_2.add(btnAdd, gbc_btnAdd);
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (prefix.getText().equals("")) return;
				if (namespace.getText().equals("")) return;
				Vector<String> row = new Vector<String>();
				row.add(prefix.getText());
				row.addElement(namespace.getText());
				namespacesDM.addRow(row);
			}
		});
		
		JPanel primitives = new JPanel();
		primitives.setBorder(new TitledBorder(null, "Primitives", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_primitives = new GridBagConstraints();
		gbc_primitives.insets = new Insets(0, 0, 5, 0);
		gbc_primitives.fill = GridBagConstraints.BOTH;
		gbc_primitives.gridx = 0;
		gbc_primitives.gridy = 2;
		SPARQLPanel.add(primitives, gbc_primitives);
		GridBagLayout gbl_primitives = new GridBagLayout();
		gbl_primitives.columnWidths = new int[]{0, 0, 0};
		gbl_primitives.rowHeights = new int[]{46, 115, 0, 0};
		gbl_primitives.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_primitives.rowWeights = new double[]{1.0, 1.0, 0.0, Double.MIN_VALUE};
		primitives.setLayout(gbl_primitives);
		
		JPanel panelUpdate = new JPanel();
		GridBagConstraints gbc_panelUpdate = new GridBagConstraints();
		gbc_panelUpdate.fill = GridBagConstraints.BOTH;
		gbc_panelUpdate.insets = new Insets(0, 0, 5, 5);
		gbc_panelUpdate.gridx = 0;
		gbc_panelUpdate.gridy = 0;
		primitives.add(panelUpdate, gbc_panelUpdate);
		GridBagLayout gbl_panelUpdate = new GridBagLayout();
		gbl_panelUpdate.columnWidths = new int[]{142, 0, 0};
		gbl_panelUpdate.rowHeights = new int[]{15, 97, 0};
		gbl_panelUpdate.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_panelUpdate.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panelUpdate.setLayout(gbl_panelUpdate);
		
		JLabel lblUpdates = new JLabel("UPDATEs");
		GridBagConstraints gbc_lblUpdates = new GridBagConstraints();
		gbc_lblUpdates.insets = new Insets(0, 0, 5, 5);
		gbc_lblUpdates.gridx = 0;
		gbc_lblUpdates.gridy = 0;
		panelUpdate.add(lblUpdates, gbc_lblUpdates);
		lblUpdates.setFont(new Font("Lucida Grande", Font.BOLD, 14));
		
		JScrollPane scrollPane_2 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_2 = new GridBagConstraints();
		gbc_scrollPane_2.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPane_2.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_2.gridx = 0;
		gbc_scrollPane_2.gridy = 1;
		panelUpdate.add(scrollPane_2, gbc_scrollPane_2);
		
		JList<String> updatesList = new JList<String>(updateListDM);
		updatesList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() == false) {

			        if (updatesList.getSelectedIndex() != -1) {
			        	String sparql = appProfile.update(updatesList.getSelectedValue());
			        	sparql=sparql.replaceFirst("\n", "");
			        	sparql=sparql.replaceAll("\t", "");
			        	sparql = sparql.trim();
			        	SPARQLUpdate.setText(sparql);
			        	
			        	Bindings bindings = appProfile.updateBindings(updatesList.getSelectedValue());
			        	updateForcedBindingsDM.clearBindings();
			        	if (bindings == null) return;
			        	for (String var : bindings.getVariables()){
			        		updateForcedBindingsDM.addBindings(var, bindings.getBindingValue(var).isLiteral());
			        	}
			        	
			        }
			    }
			}
		});
		scrollPane_2.setViewportView(updatesList);
		updatesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JLabel lblForcedBindings = new JLabel("Forced bindings");
		GridBagConstraints gbc_lblForcedBindings = new GridBagConstraints();
		gbc_lblForcedBindings.anchor = GridBagConstraints.NORTH;
		gbc_lblForcedBindings.insets = new Insets(0, 0, 5, 0);
		gbc_lblForcedBindings.gridx = 1;
		gbc_lblForcedBindings.gridy = 0;
		panelUpdate.add(lblForcedBindings, gbc_lblForcedBindings);
		
		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 1;
		gbc_scrollPane.gridy = 1;
		panelUpdate.add(scrollPane, gbc_scrollPane);
		
		updateForcedBindings = new JTable(updateForcedBindingsDM);
		scrollPane.setViewportView(updateForcedBindings);
		
		JPanel panelSubscribe = new JPanel();
		GridBagConstraints gbc_panelSubscribe = new GridBagConstraints();
		gbc_panelSubscribe.fill = GridBagConstraints.BOTH;
		gbc_panelSubscribe.insets = new Insets(0, 0, 5, 0);
		gbc_panelSubscribe.gridx = 1;
		gbc_panelSubscribe.gridy = 0;
		primitives.add(panelSubscribe, gbc_panelSubscribe);
		GridBagLayout gbl_panelSubscribe = new GridBagLayout();
		gbl_panelSubscribe.columnWidths = new int[]{152, 0, 0};
		gbl_panelSubscribe.rowHeights = new int[]{13, 0, 0};
		gbl_panelSubscribe.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_panelSubscribe.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		panelSubscribe.setLayout(gbl_panelSubscribe);
		
		JLabel lblSubscribes = new JLabel("SUBSCRIBEs");
		GridBagConstraints gbc_lblSubscribes = new GridBagConstraints();
		gbc_lblSubscribes.insets = new Insets(0, 0, 5, 5);
		gbc_lblSubscribes.gridx = 0;
		gbc_lblSubscribes.gridy = 0;
		panelSubscribe.add(lblSubscribes, gbc_lblSubscribes);
		lblSubscribes.setFont(new Font("Lucida Grande", Font.BOLD, 14));
		
		JScrollPane scrollPane_3 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_3 = new GridBagConstraints();
		gbc_scrollPane_3.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPane_3.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_3.gridx = 0;
		gbc_scrollPane_3.gridy = 1;
		panelSubscribe.add(scrollPane_3, gbc_scrollPane_3);
		
		JList<String> subscribesList = new JList<String>(subscribeListDM);
		subscribesList.addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() == false) {

			        if (subscribesList.getSelectedIndex() != -1) {
			        	String sparql = appProfile.subscribe(subscribesList.getSelectedValue());
			        	sparql=sparql.replaceFirst("\n", "");
			        	sparql=sparql.replaceAll("\t", "");
			        	sparql = sparql.trim();
			        	SPARQLSubscribe.setText(sparql);
			        }
			        
			        Bindings bindings = appProfile.subscribeBindings(subscribesList.getSelectedValue());
			        subscribeForcedBindingsDM.clearBindings();
			        if (bindings == null) return;
			        for (String var : bindings.getVariables()){
		        		subscribeForcedBindingsDM.addBindings(var, bindings.getBindingValue(var).isLiteral());
		        	}
			    }
			}
		});
		scrollPane_3.setViewportView(subscribesList);
		subscribesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JLabel lblForcedBindings_1 = new JLabel("Forced bindings");
		GridBagConstraints gbc_lblForcedBindings_1 = new GridBagConstraints();
		gbc_lblForcedBindings_1.anchor = GridBagConstraints.NORTH;
		gbc_lblForcedBindings_1.insets = new Insets(0, 0, 5, 0);
		gbc_lblForcedBindings_1.gridx = 1;
		gbc_lblForcedBindings_1.gridy = 0;
		panelSubscribe.add(lblForcedBindings_1, gbc_lblForcedBindings_1);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 1;
		gbc_scrollPane_1.gridy = 1;
		panelSubscribe.add(scrollPane_1, gbc_scrollPane_1);
		
		subscribeForcedBindings = new JTable(subscribeForcedBindingsDM);
		scrollPane_1.setViewportView(subscribeForcedBindings);
		
		JScrollPane scrollPane_5 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_5 = new GridBagConstraints();
		gbc_scrollPane_5.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_5.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane_5.gridx = 0;
		gbc_scrollPane_5.gridy = 1;
		primitives.add(scrollPane_5, gbc_scrollPane_5);
		
		SPARQLUpdate = new JTextArea();
		scrollPane_5.setViewportView(SPARQLUpdate);
		SPARQLUpdate.setLineWrap(true);
		
		btnUpdate = new JButton("UPDATE");
		btnUpdate.setEnabled(false);
		btnUpdate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Bindings forced = new Bindings();
				for (int index = 0; index < updateForcedBindingsDM.getRowCount(); index++){
					String value = (String) updateForcedBindingsDM.getValueAt(index, 1);
					String var = (String) updateForcedBindingsDM.getValueAt(index, 0);
					boolean literal = (boolean) updateForcedBindingsDM.getValueAt(index, 2);
					if (value.equals("")) {
						lblInfo.setText("Please specify binding value: "+var);
						return;
					};
					
					if (literal) forced.addBinding(var, new BindingLiteralValue(value));
					else forced.addBinding(var, new BindingURIValue(value));
				}
				String prefixes = "";
				for (int index = 0; index < namespacesDM.getRowCount();index++){
					prefixes = prefixes + "PREFIX "+namespacesDM.getValueAt(index, 0).toString()+":<"+namespacesDM.getValueAt(index, 1).toString()+"> ";
				}
				String update = SPARQLUpdate.getText().replaceAll("[\n\t]","");
				
				long start = System.currentTimeMillis();
				boolean result = kp.update(prefixes+update, forced);
				long stop = System.currentTimeMillis();
				
				lblInfo.setText("UPDATE returned "+result+" in "+(stop-start)+ " ms");
			}
		});
		
		JScrollPane scrollPane_6 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_6 = new GridBagConstraints();
		gbc_scrollPane_6.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_6.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_6.gridx = 1;
		gbc_scrollPane_6.gridy = 1;
		primitives.add(scrollPane_6, gbc_scrollPane_6);
		
		SPARQLSubscribe = new JTextArea();
		scrollPane_6.setViewportView(SPARQLSubscribe);
		SPARQLSubscribe.setLineWrap(true);
		GridBagConstraints gbc_btnUpdate = new GridBagConstraints();
		gbc_btnUpdate.insets = new Insets(0, 0, 0, 5);
		gbc_btnUpdate.gridx = 0;
		gbc_btnUpdate.gridy = 2;
		primitives.add(btnUpdate, gbc_btnUpdate);
		
		JPanel panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 1;
		gbc_panel.gridy = 2;
		primitives.add(panel, gbc_panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0, 0};
		gbl_panel.rowHeights = new int[]{0, 0};
		gbl_panel.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);
		
		btnSubscribe = new JButton("SUBSCRIBE");
		btnSubscribe.setEnabled(false);
		btnSubscribe.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (btnSubscribe.getText().equals("SUBSCRIBE")) {
					Bindings forced = new Bindings();
					for (int index = 0; index < subscribeForcedBindings.getRowCount(); index++){
						String value = (String) subscribeForcedBindings.getValueAt(index, 1);
						boolean literal = (boolean) subscribeForcedBindings.getValueAt(index, 2);
						String var = (String) subscribeForcedBindings.getValueAt(index, 0);
						
						if (value.equals("")) {
							lblInfo.setText("Please specify binding value: "+var);
							return;
						};
						
						if (literal) forced.addBinding(var, new BindingLiteralValue(value));
						else forced.addBinding(var, new BindingURIValue(value));
					}
					
					String query = SPARQLSubscribe.getText().replaceAll("[\n\t]","");
					String prefixes = "";
					for (int index = 0; index < namespacesDM.getRowCount();index++){
						prefixes = prefixes + "PREFIX "+namespacesDM.getValueAt(index, 0).toString()+":<"+namespacesDM.getValueAt(index, 1).toString()+"> ";
					}
					
					subID = kp.subscribe(prefixes + query, forced);
					
					if (subID.equals("")) {
						lblInfo.setText("Subscription failed");
						return;
					}
					btnSubscribe.setText("UNSUBSCRIBE");
				}
				else {
					if (kp.unsubscribe()) {
						btnSubscribe.setText("SUBSCRIBE");	
					}
				}
			}
		});
		GridBagConstraints gbc_btnSubscribe = new GridBagConstraints();
		gbc_btnSubscribe.insets = new Insets(0, 0, 0, 5);
		gbc_btnSubscribe.gridx = 0;
		gbc_btnSubscribe.gridy = 0;
		panel.add(btnSubscribe, gbc_btnSubscribe);
		
		btnQuery = new JButton("QUERY");
		btnQuery.setEnabled(false);
		btnQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Bindings forced = new Bindings();
				for (int index = 0; index < subscribeForcedBindings.getRowCount(); index++){
					String value = (String) subscribeForcedBindings.getValueAt(index, 1);
					boolean literal = (boolean) subscribeForcedBindings.getValueAt(index, 2);
					String var = (String) subscribeForcedBindings.getValueAt(index, 0);
					
					if (value.equals("")) {
						lblInfo.setText("Please specify binding value: "+var);
						return;
					}
					
					if (literal) forced.addBinding(var, new BindingLiteralValue(value));
					else forced.addBinding(var, new BindingURIValue(value));
				}
				String query = SPARQLSubscribe.getText().replaceAll("[\n\t]", "");
				String prefixes = "";
				for (int index = 0; index < namespacesDM.getRowCount();index++){
					prefixes = prefixes + "PREFIX "+namespacesDM.getValueAt(index, 0).toString()+":<"+namespacesDM.getValueAt(index, 1).toString()+"> ";
				}
				lblInfo.setText("Running query...");
				long start = System.currentTimeMillis();
				BindingsResults ret = kp.query(prefixes+query, forced);
				long stop = System.currentTimeMillis();
				bindingsDM.clear();
				bindingsDM.setResults(ret);
				lblInfo.setText(ret.getAddedBindings().size()+" bindings results in "+(stop-start)+" ms");
			}
		});
		GridBagConstraints gbc_btnQuery = new GridBagConstraints();
		gbc_btnQuery.gridx = 1;
		gbc_btnQuery.gridy = 0;
		panel.add(btnQuery, gbc_btnQuery);
		
		JPanel results = new JPanel();
		results.setBorder(new TitledBorder(null, "Results", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_results = new GridBagConstraints();
		gbc_results.insets = new Insets(0, 0, 5, 0);
		gbc_results.fill = GridBagConstraints.BOTH;
		gbc_results.gridx = 0;
		gbc_results.gridy = 3;
		SPARQLPanel.add(results, gbc_results);
		GridBagLayout gbl_results = new GridBagLayout();
		gbl_results.columnWidths = new int[]{0, 0};
		gbl_results.rowHeights = new int[]{70, 0, 0};
		gbl_results.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_results.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		results.setLayout(gbl_results);
		
		JScrollPane bindingsResults = new JScrollPane();
		GridBagConstraints gbc_bindingsResults = new GridBagConstraints();
		gbc_bindingsResults.insets = new Insets(0, 0, 5, 0);
		gbc_bindingsResults.fill = GridBagConstraints.BOTH;
		gbc_bindingsResults.gridx = 0;
		gbc_bindingsResults.gridy = 0;
		results.add(bindingsResults, gbc_bindingsResults);
		
		bindingsResultsTable = new JTable(bindingsDM);
		bindingsResults.setViewportView(bindingsResultsTable);
		bindingsResultsTable.setDefaultRenderer(Object.class, bindingsRender);
		bindingsResultsTable.setAutoCreateRowSorter(true);
		
		JPanel panel_1 = new JPanel();
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.gridx = 0;
		gbc_panel_1.gridy = 1;
		results.add(panel_1, gbc_panel_1);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{0, 0, 0, 0};
		gbl_panel_1.rowHeights = new int[]{0, 0};
		gbl_panel_1.columnWeights = new double[]{1.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		panel_1.setLayout(gbl_panel_1);
		
		
		lblInfo = new JLabel("Info");
		GridBagConstraints gbc_lblInfo = new GridBagConstraints();
		gbc_lblInfo.insets = new Insets(0, 0, 0, 5);
		gbc_lblInfo.anchor = GridBagConstraints.WEST;
		gbc_lblInfo.gridx = 0;
		gbc_lblInfo.gridy = 0;
		panel_1.add(lblInfo, gbc_lblInfo);
		
		JButton btnClean = new JButton("Clear");
		btnClean.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bindingsDM.clear();
				lblInfo.setText("Results cleaned");
			}
		});
		GridBagConstraints gbc_btnClean = new GridBagConstraints();
		gbc_btnClean.insets = new Insets(0, 0, 0, 5);
		gbc_btnClean.gridx = 1;
		gbc_btnClean.gridy = 0;
		panel_1.add(btnClean, gbc_btnClean);
		
		qNameCheckbox = new Checkbox("QName");
		qNameCheckbox.setState(true);
		qNameCheckbox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				bindingsRender.showAsQName(qNameCheckbox.getState());
				bindingsDM.fireTableDataChanged();
			}
		});
		GridBagConstraints gbc_qNameCheckbox = new GridBagConstraints();
		gbc_qNameCheckbox.anchor = GridBagConstraints.WEST;
		gbc_qNameCheckbox.gridx = 2;
		gbc_qNameCheckbox.gridy = 0;
		panel_1.add(qNameCheckbox, gbc_qNameCheckbox);
		
		JSplitPane visualizer = new JSplitPane();
		visualizer.setOneTouchExpandable(true);
		visualizer.setContinuousLayout(true);
		tabbedPane.addTab("Explorer", null, visualizer, null);
		
		JPanel classes = new JPanel();
		visualizer.setLeftComponent(classes);
		GridBagLayout gbl_classes = new GridBagLayout();
		gbl_classes.columnWidths = new int[]{275, 0};
		gbl_classes.rowHeights = new int[]{0, 0, 0, 0};
		gbl_classes.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_classes.rowWeights = new double[]{1.0, 1.0, 0.0, Double.MIN_VALUE};
		classes.setLayout(gbl_classes);
		
		JScrollPane scrollPane_7 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_7 = new GridBagConstraints();
		gbc_scrollPane_7.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_7.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_7.gridx = 0;
		gbc_scrollPane_7.gridy = 0;
		classes.add(scrollPane_7, gbc_scrollPane_7);
		
		classTree = new JTree(){
			/**
			 * 
			 */
			private static final long serialVersionUID = -1001045036021299702L;

			@Override
			public String getToolTipText(MouseEvent evt) {
			    if (getRowForLocation(evt.getX(), evt.getY()) == -1) return null;
			    TreePath curPath = getPathForLocation(evt.getX(), evt.getY());
			    OWLClassNodeModel node = (OWLClassNodeModel)curPath.getLastPathComponent();
			    return node.getComment();
			}
		};
		scrollPane_7.setViewportView(classTree);
		classTree.setBorder(new TitledBorder(null, "Class tree", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		classTree.setModel(new DefaultTreeModel(null));
		ToolTipManager.sharedInstance().registerComponent(classTree);
		
		JPanel panel_3 = new JPanel();
		panel_3.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Object properties", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.fill = GridBagConstraints.BOTH;
		gbc_panel_3.insets = new Insets(0, 0, 5, 0);
		gbc_panel_3.gridx = 0;
		gbc_panel_3.gridy = 1;
		classes.add(panel_3, gbc_panel_3);
		GridBagLayout gbl_panel_3 = new GridBagLayout();
		gbl_panel_3.columnWidths = new int[]{275, 0};
		gbl_panel_3.rowHeights = new int[]{0, 0};
		gbl_panel_3.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panel_3.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		panel_3.setLayout(gbl_panel_3);
		
		JScrollPane scrollPane_8 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_8 = new GridBagConstraints();
		gbc_scrollPane_8.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_8.gridx = 0;
		gbc_scrollPane_8.gridy = 0;
		panel_3.add(scrollPane_8, gbc_scrollPane_8);
		
		propertiesTable = new JTable(propertiesDM);
		propertiesTable.setFillsViewportHeight(true);
		propertiesTable.setBorder(null);
		scrollPane_8.setViewportView(propertiesTable);
		
		JButton btnStart = new JButton("Start");
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (btnStart.getText().equals("Start")) {
					classTree.setModel(new DefaultTreeModel(null));
					
					boolean joined = classMonitor.join() && propertyMonitor.join();
					
					if (!joined) return;
					
					boolean subscribed = !classMonitor.subscribe(null).equals("") && !propertyMonitor.subscribe(null).equals("");
					
					if (subscribed) btnStart.setText("Stop");
				}
				else {
					classMonitor.unsubscribe();
					classMonitor.leave();
					propertyMonitor.unsubscribe();
					propertyMonitor.leave();
					
					btnStart.setText("Start");
				}
			}
		});
		GridBagConstraints gbc_btnStart = new GridBagConstraints();
		gbc_btnStart.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnStart.gridx = 0;
		gbc_btnStart.gridy = 2;
		classes.add(btnStart, gbc_btnStart);
		
		JPanel view3d = new JPanel();
		visualizer.setRightComponent(view3d);
		GridBagLayout gbl_view3d = new GridBagLayout();
		gbl_view3d.columnWidths = new int[]{1, 0};
		gbl_view3d.rowHeights = new int[]{1, 0, 0};
		gbl_view3d.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_view3d.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		view3d.setLayout(gbl_view3d);
		
		Canvas canvas = new Canvas();
		GridBagConstraints gbc_canvas = new GridBagConstraints();
		gbc_canvas.insets = new Insets(0, 0, 5, 0);
		gbc_canvas.fill = GridBagConstraints.BOTH;
		gbc_canvas.gridx = 0;
		gbc_canvas.gridy = 0;
		view3d.add(canvas, gbc_canvas);
		
		lblInfoVisualizer = new JLabel("Info");
		GridBagConstraints gbc_lblInfoVisualizer = new GridBagConstraints();
		gbc_lblInfoVisualizer.anchor = GridBagConstraints.EAST;
		gbc_lblInfoVisualizer.gridx = 0;
		gbc_lblInfoVisualizer.gridy = 1;
		view3d.add(lblInfoVisualizer, gbc_lblInfoVisualizer);
		bindingsRender.setNamespaces(namespacesDM);
	}

	@Override
	public void notify(BindingsResults notify) {
		int added = 0;
		int removed = 0;
		
		if (notify.getAddedBindings() != null) added = notify.getAddedBindings().size();
		if (notify.getRemovedBindings() != null) removed = notify.getRemovedBindings().size();
		
		lblInfo.setText("Bindings results Added("+added+") + Removed ("+removed+")");
		
		bindingsDM.setResults(notify);
		
		Comparator<BindingValue> comparator = new Comparator<BindingValue>() {
		    public int compare(BindingValue s1, BindingValue s2) {
		        return s1.getValue().compareTo(s2.getValue());
		    }
		};
		
		TableRowSorter<TableModel>  sorter = new TableRowSorter<TableModel>(bindingsResultsTable.getModel());
		for (int i=0; i < bindingsDM.getColumnCount(); i++) sorter.setComparator(i, comparator);
		bindingsResultsTable.setRowSorter(sorter);
		
	}

	@Override
	public void notifyFirst(BindingsResults notify) {
		notify(notify);
	}
}
