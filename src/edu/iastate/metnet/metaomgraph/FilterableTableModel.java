package edu.iastate.metnet.metaomgraph;

import edu.iastate.metnet.metaomgraph.ui.NoneditableTableModel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class FilterableTableModel extends AbstractTableModel implements DocumentListener, TableModelListener {
	private TableModel model;
	private Object[][] filteredData;
	private TreeMap<Integer, Integer> rowMap;
	private boolean available;
	private String filterText;
	private boolean manualFilter;
	private boolean caseSensitive;

	public FilterableTableModel(TableModel model) {
		this.model = model;
		filteredData = new Object[model.getRowCount()][model.getColumnCount()];
		for (int x = 0; x < filteredData.length; x++)
			for (int y = 0; y < filteredData[x].length; y++)
				filteredData[x][y] = model.getValueAt(x, y);
		available = true;
		this.model.addTableModelListener(this);
		caseSensitive = false;
	}

	public void applyFilter(String newFilter) {
		if ((newFilter == null) || (newFilter.equals(""))) {
			filterText = "";
			clearFilter();
		} else {
			filterText = newFilter;
			String[] filters = null;
			boolean matchAll = false;
			// match any or all
			filters = newFilter.split(";|\\^");
			// JOptionPane.showMessageDialog(null, Arrays.toString(filters));
			for (int i = 0; i < filters.length; i++) {
				filters[i] = filters[i].trim();
			}

			applyFilter(filters);
		}
	}

	// look at this for adding buttons
	public void applyFilterold(String[] values) {
		TreeSet<Integer> hits = new TreeSet();

		for (int row = 0; row < model.getRowCount(); row++) {
			for (int col = 0; col < model.getColumnCount(); col++) {
				String thisValue = model.getValueAt(row, col) + "";
				if (!caseSensitive) {
					thisValue = thisValue.toLowerCase();
				}

				for (String findMe : values) {
					if (!caseSensitive) {
						findMe = findMe.toLowerCase();
					}
					if (thisValue.indexOf(findMe) >= 0) {
						hits.add(Integer.valueOf(row));
					}
				}
			}
		}
		filterToRows(hits);
		manualFilter = false;
		fireTableChanged(new TableModelEvent(this));
	}

	/**
	 * @author urmi modified urmi add columns to query for better searching
	 * @param values
	 * 
	 */
	public void applyFilter(String[] values) {
		// query contating column info is like "searchterm":::"colnumber" where
		// colnumber starts from zero
		String delim = ":::";
		// if query ends with --C: means case sensitive
		String caseFlag = "--C";
		caseSensitive = false;
		TreeSet<Integer> hits = new TreeSet();
		try {
			for (String findMe : values) {

				boolean colFlag = false;
				int colInt = -1;
				// delim should be present and should have values on both sides
				if (findMe.indexOf(delim) > -1 && findMe.indexOf(delim) < findMe.length() - delim.length()) {
					String col = findMe.split(delim)[1];
					// check if col is a valid integer
					try {
						colInt = Integer.parseInt(col);
						colFlag = true;

					} catch (NumberFormatException e) {
						// not an integer!
						colFlag = false;
					}
					if (colFlag) {
						// check if col is in range of tables
						if (colInt < 0 || colInt >= model.getColumnCount()) {
							colFlag = false;
						}
					}

				}

				if (colFlag) {
					// search only specified column
					findMe = findMe.split(delim)[0];
					// JOptionPane.showMessageDialog(null, "searching:" + findMe + " col:" +
					// colInt);

					if (findMe.endsWith(caseFlag)) {
						caseSensitive = true;
						findMe = findMe.split(caseFlag)[0];
					}

					for (int row = 0; row < model.getRowCount(); row++) {
						// for (int col = 0; col <= colInt; col++) {
						String thisValue = model.getValueAt(row, colInt) + "";
						if (!caseSensitive) {
							thisValue = thisValue.toLowerCase();
							findMe = findMe.toLowerCase();
						}
						if (thisValue.indexOf(findMe) >= 0) {
							hits.add(Integer.valueOf(row));
						}

						// }
					}
				} else {
					if (findMe.endsWith(caseFlag)) {
						caseSensitive = true;
						findMe = findMe.split(caseFlag)[0];
					}
					// search all columns
					for (int row = 0; row < model.getRowCount(); row++) {
						for (int col = 0; col < model.getColumnCount(); col++) {
							String thisValue = model.getValueAt(row, col) + "";
							if (!caseSensitive) {
								thisValue = thisValue.toLowerCase();
								findMe = findMe.toLowerCase();
							}

							if (thisValue.indexOf(findMe) >= 0) {
								hits.add(Integer.valueOf(row));
							}

						}
					}
				}
			}
		} catch (NullPointerException | ArrayIndexOutOfBoundsException ae) {

		}

		filterToRows(hits);
		manualFilter = false;
		fireTableChanged(new TableModelEvent(this));
	}

	public synchronized void filterToRows(int[] rows) {
		if ((rows == null) || (rows.length <= 0)) {
			throw new InvalidParameterException("rows must be non-null and have length>0");
		}
		while (!available) {
			try {
				wait();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
		available = false;
		filteredData = new Object[rows.length][model.getColumnCount()];
		rowMap = new TreeMap();
		for (int row = 0; row < rows.length; row++) {
			rowMap.put(Integer.valueOf(row), Integer.valueOf(rows[row]));
			for (int col = 0; col < filteredData[row].length; col++) {
				filteredData[row][col] = model.getValueAt(rows[row], col);
			}
		}
		available = true;
		notifyAll();
		fireTableChanged(new TableModelEvent(this));
		manualFilter = true;
	}

	public synchronized void filterToRows(Collection<Integer> rows) {
		while (!available) {
			try {
				wait();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
		available = false;
		filteredData = new Object[rows.size()][model.getColumnCount()];
		rowMap = new TreeMap();
		int row = 0;
		for (Iterator localIterator = rows.iterator(); localIterator.hasNext();) {
			int thisRow = ((Integer) localIterator.next()).intValue();
			rowMap.put(Integer.valueOf(row), Integer.valueOf(thisRow));
			for (int col = 0; col < filteredData[row].length; col++) {
				filteredData[row][col] = model.getValueAt(thisRow, col);
			}
			row++;
		}
		available = true;
		notifyAll();
		fireTableChanged(new TableModelEvent(this));
		manualFilter = true;
	}

	public synchronized void clearFilter() {
		while (!available) {
			try {
				wait();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
		available = false;
		rowMap = null;
		filteredData = new Object[model.getRowCount()][model.getColumnCount()];
		for (int x = 0; x < filteredData.length; x++)
			for (int y = 0; y < filteredData[x].length; y++)
				filteredData[x][y] = model.getValueAt(x, y);
		available = true;
		notifyAll();
		fireTableChanged(new TableModelEvent(this));
	}

	@Override
	public void fireTableChanged(TableModelEvent e) {
		TableModelListener[] listeners = getTableModelListeners();
		for (int x = 0; x < listeners.length; x++) {
			listeners[x].tableChanged(e);
		}
	}

	@Override
	public Object getValueAt(int row, int col) {
		while (!available) {
			try {
				Thread.sleep(10L);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}

		Object result;
		synchronized (filteredData) { // Object result;
			if ((filteredData == null) || (row >= getRowCount()) || (col >= getColumnCount())) {
				result = null;
			} else { // Object result;
				if ((row < 0) || (col < 0)) {
					result = null;
				} else {
					result = filteredData[row][col];
				}
			}
		}
		// Object result;
		return result;
	}

	@Override
	public int getRowCount() {
		if (filteredData == null)
			return 0;
		return filteredData.length;
	}

	public int getUnfilteredRowCount() {
		return model.getRowCount();
	}

	public Object getUnfilteredValueAt(int row, int col) {
		return model.getValueAt(row, col);
	}

	public int[] getUnfilteredRows(int[] selectedRows) {
		if (rowMap == null)
			return selectedRows;
		int[] result = new int[selectedRows.length];
		for (int x = 0; x < result.length; x++)
			result[x] = rowMap.get(new Integer(x)).intValue();
		return result;
	}

	public int getUnfilteredRow(int selectedRow) {
		if (rowMap == null)
			return selectedRow;
		return rowMap.get(new Integer(selectedRow)).intValue();
	}

	public static void main(String[] args) {
		JFrame f = new JFrame("Filterable Table Test");
		final JTextField filterField = new JTextField();
		String[][] data = new String[100][3];
		try {
			RandomAccessFile dataIn = new RandomAccessFile("z:\\supercluster75line.txt", "r");
			for (int x = 0; x < data.length; x++) {
				for (int y = 0; y < data[x].length; y++) {
					data[x][y] = dataIn.readString(' ', true);
				}
			}

			dataIn.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] headers = { "col1", "col2", "col3" };
		final FilterableTableModel model = new FilterableTableModel(new NoneditableTableModel(data, headers));
		TableSorter sorter = new TableSorter(model);
		final JTable table = new JTable(sorter);
		sorter.setTableHeader(table.getTableHeader());
		f.getContentPane().add(filterField, "First");
		final JScrollPane scrolly = new JScrollPane(table);
		f.getContentPane().add(scrolly, "Center");
		f.setSize(800, 600);
		f.setDefaultCloseOperation(3);
		filterField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.applyFilter(filterField.getText());
				scrolly.setViewportView(table);
			}

		});
		f.setVisible(true);
	}

	@Override
	public int getColumnCount() {
		return model.getColumnCount();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return model.isCellEditable(rowIndex, columnIndex);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		Class result = null;
		// check if all values are number
		String thisHeader = getColumnName(columnIndex);
		result = MetaOmGraph.getActiveProject().getInfoColType(thisHeader);
		if (result != null) {
			// JOptionPane.showMessageDialog(null," filterabletb hdr:"+thisHeader+"v:"+result.toString());
			return result;

		}
		//JOptionPane.showMessageDialog(null, "return null filterablemodel");
		return model.getColumnClass(columnIndex);
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		while (!available) {
			try {
				Thread.sleep(10L);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
		if (rowMap != null) {
			int trueRow = rowMap.get(Integer.valueOf(rowIndex)).intValue();
			filteredData[rowIndex][columnIndex] = aValue;
			model.setValueAt(aValue, trueRow, columnIndex);
		} else {
			filteredData[rowIndex][columnIndex] = aValue;
			model.setValueAt(aValue, rowIndex, columnIndex);
		}
	}

	@Override
	public String getColumnName(int columnIndex) {
		return model.getColumnName(columnIndex);
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		documentChange(e.getDocument());
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		documentChange(e.getDocument());
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		documentChange(e.getDocument());
	}

	private void documentChange(Document doc) {
		try {
			String text = doc.getText(0, doc.getLength());
			applyFilter(text);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	public String getFilterText() {
		return filterText;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		if (!manualFilter) {
			applyFilter(filterText);
		}
		fireTableDataChanged();
	}
}
