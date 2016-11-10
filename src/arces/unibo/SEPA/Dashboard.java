package arces.unibo.SEPA;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import javax.swing.ListSelectionModel;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

public class Dashboard implements GenericClient.Notification {
	Properties appProperties = new Properties();
	
	private DefaultTableModel namespacesDM;
	private String namespacesHeader[] = new String[] {"Prefix", "URI"};
	
	private BindingsTableModel bindingsDM = new BindingsTableModel();
	private BindingsRender render = new BindingsRender();
	
	private ForcedBindingsTableModel updateForcedBindingsDM = new ForcedBindingsTableModel();
	private ForcedBindingsTableModel queryForcedBindingsDM = new ForcedBindingsTableModel();
	
	private GenericClient kp;
	
	private JFrame frmSepaDashboard;
	private JTable tableBindingsResults;
	private JTextField textField_IP;
	private JTextField textField_PORT;
	private JTextField txtIot;
	private JTable tableNamespaces;
	private JTextField textField_prefix;
	private JTextField textField_suffix;
	private JLabel lblBindingsResults;
	
	static Dashboard window;
	private JTable tableQueryBindings;
	private JTable tableUpdateBindings;
	
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
			if (columnIndex == 1 || columnIndex == 2) return true;
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
				if (ret == null) return new BindingLiteralValue("");
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
		
		public BindingsRender() { super(); }
	       
		public void setValue(Object value) {
			BindingValue binding = (BindingValue) value;
			setText(binding.getValue());
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
		
	    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	    	super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	    	
	    	BindingValue binding = (BindingValue) value;
	    	
	    	setText(binding.getValue());
	    	
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
		FileInputStream in = null;
		try {
			in = new FileInputStream("resources/config.properties");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			appProperties.load(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		appProperties.list(System.out);
		
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {			
		frmSepaDashboard = new JFrame();
		frmSepaDashboard.setTitle("SEPA Dashboard");
		frmSepaDashboard.setBounds(100, 100, 925, 768);
		frmSepaDashboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{389, 76, 0};
		gridBagLayout.rowHeights = new int[]{27, 0, 58, 0, 0, 0, 72, 136, 23, 79, 0, 34, 88, 0};
		gridBagLayout.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		frmSepaDashboard.getContentPane().setLayout(gridBagLayout);
		
		JPanel panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridwidth = 2;
		gbc_panel.insets = new Insets(0, 0, 5, 0);
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 0;
		frmSepaDashboard.getContentPane().add(panel, gbc_panel);
		
		JLabel lblNewLabel_1 = new JLabel("IP");
		panel.add(lblNewLabel_1);
		
		textField_IP = new JTextField();
		panel.add(textField_IP);
		textField_IP.setText(appProperties.getProperty("ip"));
		textField_IP.setColumns(10);
		
		JLabel lblNewLabel_2 = new JLabel("PORT");
		panel.add(lblNewLabel_2);
		
		textField_PORT = new JTextField();
		panel.add(textField_PORT);
		textField_PORT.setText(appProperties.getProperty("port"));
		textField_PORT.setColumns(10);
		
		JLabel lblNewLabel_3 = new JLabel("Name");
		panel.add(lblNewLabel_3);
		
		txtIot = new JTextField();
		panel.add(txtIot);
		txtIot.setText(appProperties.getProperty("name"));
		txtIot.setColumns(10);
		
		JLabel lblNamespaces = new JLabel("Namespaces");
		lblNamespaces.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_lblNamespaces = new GridBagConstraints();
		gbc_lblNamespaces.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNamespaces.insets = new Insets(0, 0, 5, 5);
		gbc_lblNamespaces.gridx = 0;
		gbc_lblNamespaces.gridy = 1;
		frmSepaDashboard.getContentPane().add(lblNamespaces, gbc_lblNamespaces);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 2;
		frmSepaDashboard.getContentPane().add(scrollPane_1, gbc_scrollPane_1);
		
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
		
		tableNamespaces = new JTable(namespacesDM);
		tableNamespaces.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int selRow = tableNamespaces.getSelectedRow();
				textField_prefix.setText(tableNamespaces.getValueAt(selRow, 0).toString());
				textField_suffix.setText(tableNamespaces.getValueAt(selRow, 1).toString());
			}
		});
		scrollPane_1.setViewportView(tableNamespaces);
		
		JPanel panel_3 = new JPanel();
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.insets = new Insets(0, 0, 5, 0);
		gbc_panel_3.fill = GridBagConstraints.BOTH;
		gbc_panel_3.gridx = 1;
		gbc_panel_3.gridy = 2;
		frmSepaDashboard.getContentPane().add(panel_3, gbc_panel_3);
		GridBagLayout gbl_panel_3 = new GridBagLayout();
		gbl_panel_3.columnWidths = new int[]{75, 93, 21, 0, 0};
		gbl_panel_3.rowHeights = new int[]{29, 28, 0};
		gbl_panel_3.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_panel_3.rowWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		panel_3.setLayout(gbl_panel_3);
		
		JLabel lblPrefix = new JLabel("Prefix");
		GridBagConstraints gbc_lblPrefix = new GridBagConstraints();
		gbc_lblPrefix.fill = GridBagConstraints.VERTICAL;
		gbc_lblPrefix.anchor = GridBagConstraints.EAST;
		gbc_lblPrefix.insets = new Insets(0, 0, 5, 5);
		gbc_lblPrefix.gridx = 0;
		gbc_lblPrefix.gridy = 0;
		panel_3.add(lblPrefix, gbc_lblPrefix);
		
		textField_prefix = new JTextField();
		GridBagConstraints gbc_textField_prefix = new GridBagConstraints();
		gbc_textField_prefix.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_prefix.insets = new Insets(0, 0, 5, 5);
		gbc_textField_prefix.gridx = 1;
		gbc_textField_prefix.gridy = 0;
		panel_3.add(textField_prefix, gbc_textField_prefix);
		textField_prefix.setColumns(5);
		
		final JButton btnRemove = new JButton("Remove");
		GridBagConstraints gbc_btnRemove = new GridBagConstraints();
		gbc_btnRemove.insets = new Insets(0, 0, 5, 5);
		gbc_btnRemove.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnRemove.gridx = 2;
		gbc_btnRemove.gridy = 0;
		panel_3.add(btnRemove, gbc_btnRemove);
		btnRemove.setEnabled(false);
		btnRemove.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				for(int row=0; row < namespacesDM.getRowCount(); row++) {
					String prefix = namespacesDM.getValueAt(row, 0).toString();
					if (prefix.equals(textField_prefix.getText())) {
						namespacesDM.removeRow(row);
						if (kp!=null) kp.removeNamespace(textField_prefix.getText());
						return;
					}
				}
			}
		});
		
		final JButton btnAdd = new JButton("Add");
		GridBagConstraints gbc_btnAdd = new GridBagConstraints();
		gbc_btnAdd.insets = new Insets(0, 0, 5, 0);
		gbc_btnAdd.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnAdd.gridx = 3;
		gbc_btnAdd.gridy = 0;
		panel_3.add(btnAdd, gbc_btnAdd);
		btnAdd.setEnabled(false);
		btnAdd.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				Vector<Object> data = new Vector<Object>();
		        data.add(textField_prefix.getText());
		        data.add(textField_suffix.getText());
		        for(int row=0; row < namespacesDM.getRowCount(); row++) {
		        	if (namespacesDM.getValueAt(row, 0).equals(textField_prefix.getText())){
		        		namespacesDM.setValueAt(textField_suffix.getText(), row, 1);
		        		if (kp!=null) kp.addNamespace(textField_prefix.getText(), textField_suffix.getText());
		        		return;
		        	}
		        }
				namespacesDM.addRow(data);
				if (kp!=null) kp.addNamespace(textField_prefix.getText(), textField_suffix.getText());
			}
		});
		
		JLabel lblUri = new JLabel("URI");
		GridBagConstraints gbc_lblUri = new GridBagConstraints();
		gbc_lblUri.anchor = GridBagConstraints.EAST;
		gbc_lblUri.insets = new Insets(0, 0, 0, 5);
		gbc_lblUri.gridx = 0;
		gbc_lblUri.gridy = 1;
		panel_3.add(lblUri, gbc_lblUri);
		
		textField_suffix = new JTextField();
		GridBagConstraints gbc_textField_suffix = new GridBagConstraints();
		gbc_textField_suffix.gridwidth = 3;
		gbc_textField_suffix.insets = new Insets(0, 0, 0, 5);
		gbc_textField_suffix.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_suffix.anchor = GridBagConstraints.NORTH;
		gbc_textField_suffix.gridx = 1;
		gbc_textField_suffix.gridy = 1;
		panel_3.add(textField_suffix, gbc_textField_suffix);
		textField_suffix.setColumns(25);
		
		JLabel lblSparql = new JLabel("SPARQL Query");
		lblSparql.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_lblSparql = new GridBagConstraints();
		gbc_lblSparql.anchor = GridBagConstraints.SOUTH;
		gbc_lblSparql.insets = new Insets(0, 0, 5, 5);
		gbc_lblSparql.gridx = 0;
		gbc_lblSparql.gridy = 4;
		frmSepaDashboard.getContentPane().add(lblSparql, gbc_lblSparql);
		
		JLabel lblSparqlUpdate = new JLabel("SPARQL Update");
		GridBagConstraints gbc_lblSparqlUpdate = new GridBagConstraints();
		gbc_lblSparqlUpdate.insets = new Insets(0, 0, 5, 0);
		gbc_lblSparqlUpdate.gridx = 1;
		gbc_lblSparqlUpdate.gridy = 4;
		frmSepaDashboard.getContentPane().add(lblSparqlUpdate, gbc_lblSparqlUpdate);
		lblSparqlUpdate.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		
		JScrollPane scrollPane_5 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_5 = new GridBagConstraints();
		gbc_scrollPane_5.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_5.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_5.gridx = 1;
		gbc_scrollPane_5.gridy = 6;
		frmSepaDashboard.getContentPane().add(scrollPane_5, gbc_scrollPane_5);
		
		JScrollPane scrollPane_4 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_4 = new GridBagConstraints();
		gbc_scrollPane_4.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_4.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane_4.gridx = 0;
		gbc_scrollPane_4.gridy = 6;
		frmSepaDashboard.getContentPane().add(scrollPane_4, gbc_scrollPane_4);
		
		JScrollPane scrollPane_2 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_2 = new GridBagConstraints();
		gbc_scrollPane_2.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_2.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane_2.gridx = 0;
		gbc_scrollPane_2.gridy = 7;
		frmSepaDashboard.getContentPane().add(scrollPane_2, gbc_scrollPane_2);
		
		final JTextArea sparqlQuery = new JTextArea();
		sparqlQuery.setLineWrap(true);
		scrollPane_2.setViewportView(sparqlQuery);
		
		final DefaultListModel<String> subscribeListModel = new DefaultListModel<>();
		final JList<String> listSubscribes = new JList<String>(subscribeListModel);
		listSubscribes.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;
				String SPARQL = SPARQLApplicationProfile.subscribe(listSubscribes.getModel().getElementAt(listSubscribes.getSelectedIndex()).toString());
				SPARQL = SPARQL.replace("\n", "");
				SPARQL = SPARQL.replace("\r", "");
				SPARQL = SPARQL.replace("\t", "");
				SPARQL = SPARQL.trim();
				sparqlQuery.setText(SPARQL);
				
				queryForcedBindingsDM.clearBindings();
				
				Bindings bindings = SPARQLApplicationProfile.subscribeBindings(listSubscribes.getModel().getElementAt(listSubscribes.getSelectedIndex()).toString());
				if (bindings == null) return;
				for (String variable : bindings.getVariables()) {
					queryForcedBindingsDM.addBindings(variable, bindings.getBindingValue(variable).isLiteral());
				}
			}
		});

		scrollPane_4.setViewportView(listSubscribes);
		listSubscribes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		final DefaultListModel<String> updateListModel = new DefaultListModel<>();
		
		JScrollPane scrollPane_3 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_3 = new GridBagConstraints();
		gbc_scrollPane_3.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_3.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_3.gridx = 1;
		gbc_scrollPane_3.gridy = 7;
		frmSepaDashboard.getContentPane().add(scrollPane_3, gbc_scrollPane_3);
		
		final JTextArea sparqlUpdate = new JTextArea();
		sparqlUpdate.setLineWrap(true);
		scrollPane_3.setViewportView(sparqlUpdate);
		
		JLabel lblForcedBindings = new JLabel("Forced bindings");
		lblForcedBindings.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_lblForcedBindings = new GridBagConstraints();
		gbc_lblForcedBindings.anchor = GridBagConstraints.SOUTHWEST;
		gbc_lblForcedBindings.insets = new Insets(0, 0, 5, 5);
		gbc_lblForcedBindings.gridx = 0;
		gbc_lblForcedBindings.gridy = 8;
		frmSepaDashboard.getContentPane().add(lblForcedBindings, gbc_lblForcedBindings);
		
		JLabel lblForcedBindings_1 = new JLabel("Forced bindings");
		lblForcedBindings_1.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_lblForcedBindings_1 = new GridBagConstraints();
		gbc_lblForcedBindings_1.anchor = GridBagConstraints.SOUTHWEST;
		gbc_lblForcedBindings_1.insets = new Insets(0, 0, 5, 0);
		gbc_lblForcedBindings_1.gridx = 1;
		gbc_lblForcedBindings_1.gridy = 8;
		frmSepaDashboard.getContentPane().add(lblForcedBindings_1, gbc_lblForcedBindings_1);
		
		JScrollPane scrollPane_6 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_6 = new GridBagConstraints();
		gbc_scrollPane_6.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_6.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane_6.gridx = 0;
		gbc_scrollPane_6.gridy = 9;
		frmSepaDashboard.getContentPane().add(scrollPane_6, gbc_scrollPane_6);
		
		tableQueryBindings = new JTable(queryForcedBindingsDM);
		tableQueryBindings.setShowVerticalLines(false);
		tableQueryBindings.setShowHorizontalLines(false);
		tableQueryBindings.setShowGrid(false);
		tableQueryBindings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tableQueryBindings.setRowSelectionAllowed(false);
		scrollPane_6.setViewportView(tableQueryBindings);
		
		JScrollPane scrollPane_7 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_7 = new GridBagConstraints();
		gbc_scrollPane_7.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_7.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_7.gridx = 1;
		gbc_scrollPane_7.gridy = 9;
		frmSepaDashboard.getContentPane().add(scrollPane_7, gbc_scrollPane_7);
		
		tableUpdateBindings = new JTable(updateForcedBindingsDM);
		tableUpdateBindings.setShowVerticalLines(false);
		tableUpdateBindings.setShowHorizontalLines(false);
		tableUpdateBindings.setShowGrid(false);
		tableUpdateBindings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tableUpdateBindings.setRowSelectionAllowed(false);
		scrollPane_7.setViewportView(tableUpdateBindings);
		
		JPanel panel_4 = new JPanel();
		GridBagConstraints gbc_panel_4 = new GridBagConstraints();
		gbc_panel_4.anchor = GridBagConstraints.EAST;
		gbc_panel_4.insets = new Insets(0, 0, 5, 5);
		gbc_panel_4.fill = GridBagConstraints.VERTICAL;
		gbc_panel_4.gridx = 0;
		gbc_panel_4.gridy = 10;
		frmSepaDashboard.getContentPane().add(panel_4, gbc_panel_4);
		
		final JButton btnQuery = new JButton("Query");
		panel_4.add(btnQuery);
		btnQuery.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (kp != null) {
					lblBindingsResults.setText("Bindings results (0)");
					
					Bindings bindings = new Bindings();
					
					for (int index = 0; index < queryForcedBindingsDM.getRowCount(); index++) {
						BindingValue value;
						
						String stringValue = (String)queryForcedBindingsDM.getValueAt(index, 1);	
						
						if (stringValue.equals("")) continue;
						
						if ((Boolean) queryForcedBindingsDM.getValueAt(index, 2))
							value = new BindingLiteralValue(stringValue);
						else
							value = new BindingURIValue(stringValue);
						bindings.addBinding((String)queryForcedBindingsDM.getValueAt(index, 0), value);
					}
					
					bindingsDM.clear();
					
					BindingsResults ret = kp.query(sparqlQuery.getText(),bindings);
					
					if (ret == null) return;
					
					if (ret.getAddedBindings().size() == 0) return;

					lblBindingsResults.setText("Bindings results ("+ret.getAddedBindings().size()+")");
										
					bindingsDM.setResults(ret);
					
					System.out.println(ret.toString());
					
					Comparator<BindingValue> comparator = new Comparator<BindingValue>() {
					    public int compare(BindingValue s1, BindingValue s2) {
					        return s1.getValue().compareTo(s2.getValue());
					    }
					};
					
					TableRowSorter<TableModel>  sorter = new TableRowSorter<TableModel>(tableBindingsResults.getModel());
					for (int i=0; i < bindingsDM.getColumnCount(); i++) sorter.setComparator(i, comparator);
					tableBindingsResults.setRowSorter(sorter);
				}
			}
		});
		btnQuery.setEnabled(false);
		
		final JButton btnSubscribe = new JButton("Subscribe");
		panel_4.add(btnSubscribe);
		btnSubscribe.setEnabled(false);
		btnSubscribe.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (kp != null) {
					lblBindingsResults.setText("Bindings results (0)");
					
					if (btnSubscribe.getText().equals("Subscribe")) {
						
						Bindings bindings = new Bindings();
						
						for (int index = 0; index < queryForcedBindingsDM.getRowCount(); index++) {
							BindingValue value;
							
							String stringValue = (String)queryForcedBindingsDM.getValueAt(index, 1);	
							
							if (stringValue.equals("")) continue;
							
							if ((Boolean) queryForcedBindingsDM.getValueAt(index, 2))
								value = new BindingLiteralValue(stringValue);
							else
								value = new BindingURIValue(stringValue);
							bindings.addBinding((String)queryForcedBindingsDM.getValueAt(index, 0), value);
						}
						
						bindingsDM.clear();
						
						String ret = kp.subscribe(sparqlQuery.getText(),bindings);
						
						if (ret == null) return;
						
						btnSubscribe.setText("Unsubscribe");
						btnQuery.setEnabled(false);
					}
					else {
						kp.unsubscribe();
						btnSubscribe.setText("Subscribe");
						btnQuery.setEnabled(true);
					}
				}	
				
			}
		});
		
		final JButton btnUpdate = new JButton("Update");
		btnUpdate.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (kp != null){
					Bindings bindings = new Bindings();
					
					for (int index = 0; index < updateForcedBindingsDM.getRowCount(); index++) {
						BindingValue value;
						String stringValue = (String)updateForcedBindingsDM.getValueAt(index, 1);
						
						if (stringValue.equals("")) continue;
						
						if ((Boolean) updateForcedBindingsDM.getValueAt(index, 2))
							value = new BindingLiteralValue(stringValue);
						else
							value = new BindingURIValue(stringValue);
						bindings.addBinding((String)updateForcedBindingsDM.getValueAt(index, 0), value);
					}
					
					boolean ret = kp.update(sparqlUpdate.getText(),bindings);
					
					if (ret) lblBindingsResults.setText("Update done");
					else lblBindingsResults.setText("Update ERROR");
				}
			}
		});
		btnUpdate.setEnabled(false);
		GridBagConstraints gbc_btnUpdate = new GridBagConstraints();
		gbc_btnUpdate.anchor = GridBagConstraints.EAST;
		gbc_btnUpdate.insets = new Insets(0, 0, 5, 0);
		gbc_btnUpdate.gridx = 1;
		gbc_btnUpdate.gridy = 10;
		frmSepaDashboard.getContentPane().add(btnUpdate, gbc_btnUpdate);
		
		lblBindingsResults = new JLabel("Bindings results (0)");
		lblBindingsResults.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_lblBindingsResults = new GridBagConstraints();
		gbc_lblBindingsResults.fill = GridBagConstraints.VERTICAL;
		gbc_lblBindingsResults.anchor = GridBagConstraints.WEST;
		gbc_lblBindingsResults.insets = new Insets(0, 0, 5, 5);
		gbc_lblBindingsResults.gridx = 0;
		gbc_lblBindingsResults.gridy = 11;
		frmSepaDashboard.getContentPane().add(lblBindingsResults, gbc_lblBindingsResults);
		
		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridwidth = 2;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 12;
		frmSepaDashboard.getContentPane().add(scrollPane, gbc_scrollPane);
			
		/////// BINDINGS TABLE
		tableBindingsResults = new JTable(bindingsDM);		
		tableBindingsResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tableBindingsResults.setCellSelectionEnabled(true);
		scrollPane.setViewportView(tableBindingsResults);
		tableBindingsResults.setAutoCreateRowSorter(true);
		tableBindingsResults.setDefaultRenderer(Object.class, render);
		
		final JButton btnJoinLeave = new JButton("Join");
		panel.add(btnJoinLeave);
		
		final JButton btnLoadProfile = new JButton("Load profile");
		btnLoadProfile.setEnabled(false);
		panel.add(btnLoadProfile);
		btnLoadProfile.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				//Create a file chooser
				final JFileChooser fc = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Application profile","xml");
				fc.setFileFilter(filter);
				if (appProperties.containsKey("appProfile"))
					fc.setCurrentDirectory(new File(appProperties.getProperty("appProfile")));
				int returnVal = fc.showOpenDialog(frmSepaDashboard);
				
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fc.getSelectedFile();         
				
					if(SPARQLApplicationProfile.load(file.getPath())){;
						String title = frmSepaDashboard.getTitle();
						String[] name = title.split("-");
						if (name.length == 2) {
							frmSepaDashboard.setTitle(name[0]+"-"+file.getPath());
						}
						else frmSepaDashboard.setTitle(title+"-"+file.getPath());
					
						appProperties.setProperty("appProfile", file.getPath());
						FileOutputStream out = null;
						try {
							out = new FileOutputStream("resources/config.properties");
						} catch (FileNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							appProperties.store(out, "---Last used values---");
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							out.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						
						subscribeListModel.clear();
						for (String element : SPARQLApplicationProfile.getSubscribeIds()) subscribeListModel.addElement(element);
						
						Set<String> list = null;
						list = SPARQLApplicationProfile.getUpdateIds();
						
						updateListModel.clear();
						for (String element : list) updateListModel.addElement(element);
						
						if (kp!= null) kp.clearNamespaces();
						
						for (String prefix : SPARQLApplicationProfile.getPrefixes()) {
							Vector<Object> data = new Vector<Object>();
						 	data.add(prefix);
					        data.add(SPARQLApplicationProfile.getNamespaceURI(prefix));
							namespacesDM.addRow(data);
							
							if (kp!=null) kp.addNamespace(prefix, SPARQLApplicationProfile.getNamespaceURI(prefix));
						}
					}
				}
			}
		});
		
		btnJoinLeave.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (txtIot.isEnabled()) {
					kp = new GenericClient(textField_IP.getText(),Integer.decode(textField_PORT.getText()),txtIot.getText(),window);
					if (kp.join()) {
						textField_IP.setEnabled(false);
						textField_PORT.setEnabled(false);
						txtIot.setEnabled(false);
						btnJoinLeave.setText("Leave");
						btnQuery.setEnabled(true);
						btnUpdate.setEnabled(true);
						btnAdd.setEnabled(true);
						btnRemove.setEnabled(true);
						btnSubscribe.setEnabled(true);
						btnLoadProfile.setEnabled(true);
						//Save properties
						appProperties.setProperty("ip", textField_IP.getText());
						appProperties.setProperty("port", textField_PORT.getText());
						appProperties.setProperty("name", txtIot.getText());
						FileOutputStream out = null;
						try {
							out = new FileOutputStream("resources/config.properties");
						} catch (FileNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							appProperties.store(out, "---Last used values---");
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							out.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				else {
					if (kp.isJoined()) kp.leave();
					kp = null;
					textField_IP.setEnabled(true);
					textField_PORT.setEnabled(true);
					txtIot.setEnabled(true);
					btnJoinLeave.setText("Join");
					btnQuery.setEnabled(false);
					btnUpdate.setEnabled(false);
					btnAdd.setEnabled(false);
					btnRemove.setEnabled(false);
					btnSubscribe.setEnabled(false);
					btnLoadProfile.setEnabled(false);
				}
			}
		});
	
		JPanel panel_1 = new JPanel();
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_1.anchor = GridBagConstraints.NORTH;
		gbc_panel_1.insets = new Insets(0, 0, 5, 0);
		gbc_panel_1.gridx = 1;
		gbc_panel_1.gridy = 5;
		frmSepaDashboard.getContentPane().add(panel_1, gbc_panel_1);
		panel_1.setLayout(new GridLayout(0, 3, 0, 0));
		
		final JList<String> listUpdates = new JList<String>(updateListModel);
		listUpdates.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;
				
				updateForcedBindingsDM.clearBindings();
				
				if (listUpdates.isSelectionEmpty()) {
					sparqlUpdate.setText("");
					return;
				}
				
				Bindings bindings = null;
				
				String SPARQL = SPARQLApplicationProfile.update(listUpdates.getModel().getElementAt(listUpdates.getSelectedIndex()).toString());
				SPARQL = SPARQL.replace("\n", "");
				SPARQL = SPARQL.replace("\r", "");
				SPARQL = SPARQL.replace("\t", "");
				SPARQL = SPARQL.trim();
				sparqlUpdate.setText(SPARQL);
				bindings = SPARQLApplicationProfile.updateBindings(listUpdates.getModel().getElementAt(listUpdates.getSelectedIndex()).toString());
				
				if (bindings == null) return;
				
				for (String variable : bindings.getVariables()) updateForcedBindingsDM.addBindings(variable, bindings.getBindingValue(variable).isLiteral());
			}
		});
		
		scrollPane_5.setViewportView(listUpdates);
		listUpdates.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	@Override
	public void notify(BindingsResults notify) {
		int added = 0;
		int removed = 0;
		
		if (notify.getAddedBindings() != null) added = notify.getAddedBindings().size();
		if (notify.getRemovedBindings() != null) removed = notify.getRemovedBindings().size();
		
		lblBindingsResults.setText("Bindings results Added("+added+") + Removed ("+removed+")");
		
		bindingsDM.setResults(notify);
		
		System.out.println(notify.toString());
		
		Comparator<BindingValue> comparator = new Comparator<BindingValue>() {
		    public int compare(BindingValue s1, BindingValue s2) {
		        return s1.getValue().compareTo(s2.getValue());
		    }
		};
		
		TableRowSorter<TableModel>  sorter = new TableRowSorter<TableModel>(tableBindingsResults.getModel());
		for (int i=0; i < bindingsDM.getColumnCount(); i++) sorter.setComparator(i, comparator);
		tableBindingsResults.setRowSorter(sorter);
	}

	@Override
	public void notifyFirst(BindingsResults notify) {
		notify(notify);
	}
}
