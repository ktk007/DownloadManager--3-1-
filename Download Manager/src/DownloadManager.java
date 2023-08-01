import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;


// The Download Manager.
public class DownloadManager extends JFrame
        implements Observer {

    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/download_manager_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "admin";
    private File downloadFolder;


    private Connection connection;
    //add history
    private ArrayList<Download> downloadHistory;


    // Add download text field.
    private JTextField addTextField;

    // Download table's data model.
    private DownloadsTableModel tableModel;

    // Table listing downloads.
    private JTable table;

    // These are the buttons for managing the selected download.
    private JButton pauseButton, resumeButton;
    private JButton cancelButton, clearButton;
    //private JButton historyButton;

    // Currently selected download.
    private Download selectedDownload;

    // Flag for whether or not table selection is being cleared.
    private boolean clearing;
    private File chooseDownloadFolder() {
        JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        fileChooser.setDialogTitle("Select Download Folder");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }

        return null;
    }


    // Constructor for Download Manager.
    public DownloadManager() {

        // Initialize the downloadHistory list
        downloadHistory = new ArrayList<>();
        // Set up database connection
        initializeDatabaseConnection();


        // Set application title.
        setTitle("Download Manager");

        // Set window size.
        setSize(640, 480);

        // Handle window closing events.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionExit();
            }
        });

        // Set up file menu.
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem fileExitMenuItem = new JMenuItem("Exit",
                KeyEvent.VK_X);
        fileExitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionExit();
            }
        });
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Set up add panel.
        JPanel addPanel = new JPanel();
        addTextField = new JTextField(30);
        addPanel.add(addTextField);
        JButton addButton = new JButton("Add Download");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionAdd();
            }
        });
        addPanel.add(addButton);
        // Add panel for folder selection.


        // Set up Downloads table.
        tableModel = new DownloadsTableModel();
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                tableSelectionChanged();
            }
        });
        // Allow only one row at a time to be selected.
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set up ProgressBar as renderer for progress column.
        ProgressRenderer renderer = new ProgressRenderer(0, 100);
        renderer.setStringPainted(true); // show progress text
        table.setDefaultRenderer(JProgressBar.class, renderer);

        // Set table's row height large enough to fit JProgressBar.
        table.setRowHeight(
                (int) renderer.getPreferredSize().getHeight());

        // Set up downloads panel.
        JPanel downloadsPanel = new JPanel();
        downloadsPanel.setBorder(
                BorderFactory.createTitledBorder("Downloads"));
        downloadsPanel.setLayout(new BorderLayout());
        downloadsPanel.add(new JScrollPane(table),
                BorderLayout.CENTER);

        // Set up buttons panel.
        JPanel buttonsPanel = new JPanel();
        downloadHistory = new ArrayList<>();
        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionPause();
            }
        });
        pauseButton.setEnabled(false);
        buttonsPanel.add(pauseButton);
        resumeButton = new JButton("Resume");
        resumeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionResume();
            }
        });
        resumeButton.setEnabled(false);
        buttonsPanel.add(resumeButton);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionCancel();
            }
        });
        cancelButton.setEnabled(false);
        buttonsPanel.add(cancelButton);
        clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionClear();
            }
        });
        clearButton.setEnabled(false);
        buttonsPanel.add(clearButton);

        JButton historyButton = new JButton("History");
        historyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionHistory();
            }
        });
        buttonsPanel.add(historyButton);

        // Add panels to display.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(addPanel, BorderLayout.NORTH);
        getContentPane().add(downloadsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void initializeDatabaseConnection() {
        try {
            Properties props = new Properties();
            props.put("user", DB_USER);
            props.put("password", DB_PASSWORD);
            connection = DriverManager.getConnection(DB_URL, props);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to connect to the database.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Exit this program.
    private void actionExit() {
        // Delete all downloads from the database before exiting
        try {
            Download[] downloadList = new Download[0];
            for (Download download : downloadList) {
                deleteDownloadFromDatabase(download);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to delete download history from the database.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        System.exit(0);
    }

    // Add a new download.
    private void actionAdd() {
        URL verifiedUrl = verifyUrl(addTextField.getText());
        if (verifiedUrl != null) {
            File downloadFolder = chooseDownloadFolder();
            if (downloadFolder != null) {
                tableModel.addDownload(new Download(verifiedUrl, downloadFolder));
                System.out.println("DownloadManager.actionAdd() - Download added to tableModel.");

            }
            addTextField.setText(""); // reset add text field
        } else {
            JOptionPane.showMessageDialog(this,
                    "Invalid Download URL", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }



    // Verify download URL.
    private URL verifyUrl(String url) {
        // Only allow HTT P URLs.
        if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://"))
            return null;

        // Verify format of URL.
        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(url);
        } catch (Exception e) {
            return null;
        }

        // Make sure URL specifies a file.
        if (verifiedUrl.getFile().length() < 2)
            return null;

        return verifiedUrl;
    }


    // Called when table row selection changes.
    private void tableSelectionChanged() {
        /*
         * Unregister from receiving notifications
         * from the last selected download.
         */
        if (selectedDownload != null)
            selectedDownload.deleteObserver(DownloadManager.this);

        /*
         * If not in the middle of clearing a download,
         * set the selected download and register to
         * receive notifications from it.
         */
        if (!clearing) {
            selectedDownload = tableModel.getDownload(table.getSelectedRow());
            selectedDownload.addObserver(DownloadManager.this);
            updateButtons();
            // Check if the selected download is completed and not in history yet
            if (selectedDownload.getStatus() == Download.COMPLETE && !downloadHistory.contains(selectedDownload)) {
                tableModel.addDownloadToHistory(selectedDownload);
                downloadHistory.add(selectedDownload);
                selectedDownload.addObserver(DownloadManager.this); // Add observer to the newly added history download

            }
        }
    }

    // Pause the selected download.
    private void actionPause() {
        selectedDownload.pause();
        updateButtons();
    }

    // Resume the selected download.
    private void actionResume() {
        selectedDownload.resume();
        updateButtons();
    }

    // Cancel the selected download.
    private void actionCancel() {
        selectedDownload.cancel();
        updateButtons();
    }

    // Clear the selected download.
    private void actionClear() {
        // Delete the selected download from the database
        try {
            if (selectedDownload != null) {
                deleteDownloadFromDatabase(selectedDownload);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to delete the download from the database.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        clearing = true;
        tableModel.clearDownload(table.getSelectedRow());
        clearing = false;
        selectedDownload = null;
        updateButtons();
    }

    // Method to delete a download from the database.
    private void deleteDownloadFromDatabase(Download download) throws SQLException {
        String sql = "DELETE FROM download_history WHERE url = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, download.getUrl());
            statement.executeUpdate();
        }
    }

    //history button
    private void actionHistory()  {
        try {

            // Retrieve download history from the database
            ArrayList<Download> downloadHistory = fetchDownloadHistoryFromDatabase();
            StringBuilder historyMessage = new StringBuilder();
            for (Download download : downloadHistory) {
                historyMessage.append("URL: ").append(download.getUrl()).append("\n");
                historyMessage.append("Size: ").append(download.getSize()).append(" bytes\n");
                historyMessage.append("Progress: ").append(download.getProgress()).append("%\n");
                historyMessage.append("Status: ").append(Download.STATUSES[download.getStatus()]).append("\n");
                historyMessage.append("------------------------------------------------\n");
            }

            if (downloadHistory.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No past downloads.", "Download History", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JTextArea textArea = new JTextArea(historyMessage.toString());
                textArea.setEditable(false);
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(600, 400));
                JOptionPane.showMessageDialog(this, scrollPane, "Download History", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to retrieve download history from the database.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    // Method to retrieve download history from the database.
    private ArrayList<Download> fetchDownloadHistoryFromDatabase() throws SQLException {
        ArrayList<Download> downloadHistory = new ArrayList<>();
        String sql = "SELECT * FROM download_history";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String url = resultSet.getString("url");
                long size = resultSet.getInt("size");
                float progress = resultSet.getFloat("progress");
                int status = resultSet.getInt("status");
                Download download = new Download(new URL(url),downloadFolder);
                download.setSize((int) size);
                download.setStatus(status);
                download.setDownloaded((long) (size * progress / 100));
                downloadHistory.add(download);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return downloadHistory;
    }

    /*
     * Update each button's state based off of the
     * currently selected download's status.
     */
    private void updateButtons() {
        if (selectedDownload != null) {
            int status = selectedDownload.getStatus();
            switch (status) {
                case Download.DOWNLOADING:
                    pauseButton.setEnabled(true);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    clearButton.setEnabled(false);
                   // historyButton.setEnabled(true);
                    break;
                case Download.PAUSED:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    clearButton.setEnabled(false);
                    //historyButton.setEnabled(true);
                    break;
                case Download.ERROR:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
                    //historyButton.setEnabled(true);
                    break;
                default: // COMPLETE or CANCELLED
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
                    //historyButton.setEnabled(true);
            }
        } else {
            // No download is selected in table.
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(false);
            cancelButton.setEnabled(false);
            clearButton.setEnabled(false);
            //historyButton.setEnabled(true);
        }
    }

    /*
     * Update is called when a Download notifies its
     * observers of any changes.
     */
    public void update(Observable o, Object arg) {
        // Update buttons if the selected download has changed.
        if (selectedDownload != null && selectedDownload.equals(o))
            updateButtons();
    }

    // Run the Download Manager.
    public static void main(String[] args) {
        DownloadManager manager = new DownloadManager();
        ImageIcon img = new ImageIcon("icon.png");
        manager.setIconImage(img.getImage());
        manager.show();
        System.out.println("main method executing");

    }
}
