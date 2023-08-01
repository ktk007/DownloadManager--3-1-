import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

// This class manages the download table's data.
class DownloadsTableModel extends AbstractTableModel
        implements Observer {

    // These are the names for the table's columns.
    private static final String[] columnNames = { "URL", "Size in MB", "Progress", "Speed in KB/s",
            "Avg Speed in KB/s", "Elapsed Time", "Remaining Time", "Status" };

    // These are the classes for each column's values.
    private static final Class[] columnClasses = { String.class, String.class,
            JProgressBar.class, String.class, String.class, String.class, String.class, String.class };

    // The table's list of downloads.
    private ArrayList<Download> downloadList = new ArrayList<Download>();
    // The table's list of completed downloads.
    private ArrayList<Download> downloadHistory = new ArrayList<>();

    public ArrayList<Download> getDownloadList() {
        return downloadList;
    }




    // Add a new download to the table.
    public void addDownload(Download download) {

        // Register to be notified when the download changes.
        download.addObserver((Observer) this);

        downloadList.add(download);

        // Fire table row insertion notification to table.
        fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
    }

    // Get a download for the specified row.
    public Download getDownload(int row) {
        return downloadList.get(row);
    }

    // Remove a download from the list.
    public void clearDownload(int row) {
        downloadList.remove(row);

        // Fire table row deletion notification to table.
        fireTableRowsDeleted(row, row);
    }

    // Get table's column count.
    public int getColumnCount() {
        return columnNames.length;
    }

    // Get a column's name.
    public String getColumnName(int col) {
        return columnNames[col];
    }

    // Get a column's class.
    public Class getColumnClass(int col) {
        return columnClasses[col];
    }

    // Get table's row count.
    public int getRowCount() {
        return downloadList.size() + downloadHistory.size();
    }

    // Get value for a specific row and column combination.
    public Object getValueAt(int row, int col) {
        if (row < downloadList.size()){
        Download download = downloadList.get(row);
        switch (col) {
            case 0: // URL
                return download.getUrl();
            case 1: // Size
                long size = download.getSize();
                return (size == -1) ? "" : Float.toString((float) size / 1048576);
            case 2: // Progress
                return new Float(download.getProgress());
            case 3: // Speed
                return download.getSpeed();
            case 4: // Avg Speed
                return download.getAvgSpeed();
            case 5: // Elapsed Time
                return download.getElapsedTime();
            case 6: // Remaining Time
                return download.getRemainingTime();
            case 7: // Status
                return Download.STATUSES[download.getStatus()];
        }}
        else{
                Download download = downloadHistory.get(row - (getRowCount() - downloadHistory.size()));
            //Download download = downloadHistory.get(row - downloadList.size());

            switch (col) {
                    case 0: // URL
                        return download.getUrl();
                    case 1: // Size
                        long size = download.getSize();
                        return (size == -1) ? "" : Float.toString((float) size / 1048576);
                    case 2: // Progress
                        return new Float(download.getProgress());
                    case 3: // Speed
                        return download.getSpeed();
                    case 4: // Avg Speed
                        return download.getAvgSpeed();
                    case 5: // Elapsed Time
                        return download.getElapsedTime();
                    case 6: // Remaining Time
                        return download.getRemainingTime();
                    case 7: // Status
                        return Download.STATUSES[download.getStatus()];
                }}
                return "";
            }

    // Add a completed download to the history.
    public void addDownloadToHistory(Download download) {
        downloadHistory.add(download);
        fireTableDataChanged(); // Notify the table that data has changed
        System.out.println("Download added to history: " + download.getUrl());
    }

    /*
     * Update is called when a Download notifies its
     * observers of any changes
     */
    public void update(Observable o, Object arg) {
        int index = downloadList.indexOf(o);
        // Fire table row update notification to table.
        fireTableRowsUpdated(index, index);
    }
}