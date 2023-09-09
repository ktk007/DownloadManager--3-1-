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

    private JFrame Filter; // Declare a JFrame for the new window

    private Connection connection;
    //add history
    private ArrayList<Download> downloadHistory;
    // Add these fields to store download lists for different types
    private ArrayList<Download> pdfDownloads;
    private ArrayList<Download> documentDownloads;
    private ArrayList<Download> musicDownloads;
    private ArrayList<Download> videoDownloads;
    private ArrayList<Download> otherDownloads;


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
        // ImageIcon img=new ImageIcon("icon3.png");

        //Initialize the downloadHistory list
        downloadHistory = new ArrayList<>();
        // Set up database connection
        initializeDatabaseConnection();
        // Set application title.
        setTitle("Download Manager");

        // Set window size.
        setSize(1000, 800);

        // Handle window closing events.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionExit();
            }
        });
        // Initialize download lists
        pdfDownloads = new ArrayList<>();
        documentDownloads = new ArrayList<>();
        musicDownloads = new ArrayList<>();
        videoDownloads = new ArrayList<>();
        otherDownloads = new ArrayList<>();


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
        addTextField = new JTextField(50);
        addPanel.add(addTextField);
        JButton addButton = new JButton("Add Download");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionAdd();
            }
        });
        addPanel.add(addButton);

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
        //downloadsPanel.setPreferredSize(new Dimension(1000,500));
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


        // Create a Color object for Powder Blue
        Color powderBlue = new Color(176, 224, 230);
        //set another panel
        JPanel newPanel = new JPanel();
        newPanel.setPreferredSize(new Dimension(200, 590));
        newPanel.setBackground(powderBlue);
        JButton checkInternetButton = new JButton("Check Internet");
        JButton filters = new JButton("Filters");
        newPanel.add(Box.createVerticalStrut(250)); // Adjust the spacing (10 pixels) as needed
        newPanel.add(checkInternetButton);
        newPanel.add(Box.createVerticalStrut(20)); // Adjust the spacing (10 pixels) as needed
        newPanel.add(filters);
        newPanel.add(Box.createVerticalStrut(20));
        checkInternetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (InternetConnectionCheck.isInternetAvailable()) {
                    JOptionPane.showMessageDialog(null, "Internet is ON");
                } else {
                    JOptionPane.showMessageDialog(null, "Internet is OFF");
                }
            }
        });
        filters.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openFilters(); // Open the new window when the button is clicked
            }
        });
        JButton historyButton = new JButton("All Downloads");
        historyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionHistory();
            }
        });

        newPanel.add(historyButton);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        // Add panels to display.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(addPanel, BorderLayout.NORTH);
        getContentPane().add(downloadsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
        getContentPane().add(newPanel, BorderLayout.WEST);


    }

    private void openFilters() {
        if (Filter == null) {
            Filter = new JFrame("Filters Window");
            Filter.setSize(800, 300);
// Create a Color object for Powder Blue
            Color powderBlue = new Color(176, 224, 230);
            Color teal = new Color(90, 200, 208);

            // Create a single panel for buttons
            JPanel buttonPanel = new JPanel();
            buttonPanel.setBackground(powderBlue);
// Set the layout manager for the panel to make buttons align vertically
            //buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

            // Create buttons for each category
            JButton pdfButton = new JButton("PDF");
            JButton docButton = new JButton("Documents");
            JButton musicButton = new JButton("Music");
            JButton videoButton = new JButton("Video");
            JButton othersButton = new JButton("Others");

            // Add buttons to the button panel
            buttonPanel.add(Box.createVerticalStrut(300)); // Adjust the spacing (10 pixels) as needed
            buttonPanel.add(pdfButton);
            buttonPanel.add(Box.createHorizontalStrut(20)); // Adjust the spacing (10 pixels) as needed
            buttonPanel.add(docButton);
            buttonPanel.add(Box.createHorizontalStrut(20));
            buttonPanel.add(musicButton);
            buttonPanel.add(Box.createHorizontalStrut(20));
            buttonPanel.add(videoButton);
            buttonPanel.add(Box.createHorizontalStrut(20));
            buttonPanel.add(othersButton);
            // Add action listeners to the buttons
            pdfButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showCategoryDownloads("PDF Documents");
                }
            });

            docButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showCategoryDownloads("Documents");
                }
            });


            musicButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showCategoryDownloads("Music");
                }
            });

            videoButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showCategoryDownloads("Videos");
                }
            });


            othersButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showCategoryDownloads("Others");
                }
            });

            /*// Create a JTextArea for displaying download links
            JTextArea linkTextArea = new JTextArea(20, 40);
            linkTextArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(linkTextArea);

            // Create a split pane to hold the button panel and link text area
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buttonPanel, scrollPane);
            splitPane.setResizeWeight(1.0); // Adjust the split pane width

            // Add the split pane to the FiltersWindow*/
            Filter.add(buttonPanel);


            Filter.setVisible(true);
        }
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
        String urlText = addTextField.getText().trim();
        if (urlText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "URL field is empty.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        URL verifiedUrl = verifyUrl(addTextField.getText());
        if (verifiedUrl != null) {
            File downloadFolder = chooseDownloadFolder();
            if (downloadFolder != null) {
                //determine the file type based on the file extension
                String fileType = getFileType(String.valueOf(verifiedUrl));

                Download download = new Download(verifiedUrl, downloadFolder, fileType);

                tableModel.addDownload(download);

               /* // Add download to the appropriate category based on file type
                if ("pdf".equalsIgnoreCase(fileType)) {
                    pdfDownloads.add(download);
                } else if ("doc".equalsIgnoreCase(fileType) || "docx".equalsIgnoreCase(fileType)) {
                    documentDownloads.add(download);
                } else if ("mp3".equalsIgnoreCase(fileType)) {
                    musicDownloads.add(download);
                } else if ("mp4".equalsIgnoreCase(fileType)) {
                    videoDownloads.add(download);
                } else {
                    otherDownloads.add(download);
                }*/
                System.out.println("DownloadManager.actionAdd() - Download added to tableModel.");

            }
            addTextField.setText(""); // reset add text field
        } else {
            JOptionPane.showMessageDialog(this,
                    "Invalid Download URL", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "file"; }// Default fileType*/

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
    // Method to show downloads for a specific category
    private void showCategoryDownloads(String categoryName) {
        ArrayList<Download> downloads = null;

        try {
            downloads = loadDownloadsFromDatabase(categoryName);
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle the exception appropriately
        }

        if (downloads != null) {
            StringBuilder message = new StringBuilder();
            message.append("Category: ").append(categoryName).append("\n\n");

            for (Download download : downloads) {
                message.append("URL: ").append(download.getUrl()).append("\n");
            }

            JTextArea textArea = new JTextArea(message.toString());
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(600, 400));
            JOptionPane.showMessageDialog(this, scrollPane, categoryName, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private ArrayList<Download> loadDownloadsFromDatabase(String categoryName) throws SQLException {
        ArrayList<Download> downloads = new ArrayList<>();
        String sql = "SELECT * FROM download_history WHERE category = ?";

        try (
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, categoryName);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String url = resultSet.getString("url");
                long size = resultSet.getLong("size");
                float progress = resultSet.getFloat("progress");
                int status = resultSet.getInt("status");
                String fileType = resultSet.getString("fileType");

                Download download = new Download(new URL(url), downloadFolder, fileType);
                download.setSize(size);
                download.setStatus(status);
                download.setDownloaded((long) (size * progress / 100));
                downloads.add(download);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        return downloads;
    }



    // Method to retrieve download history from the database.
    private ArrayList<Download> fetchDownloadHistoryFromDatabase() throws SQLException {
        ArrayList<Download> downloadHistory = new ArrayList<>();
        String sql = "SELECT * FROM download_history";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String url = resultSet.getString("url");
                long size = resultSet.getLong("size");
                float progress = resultSet.getFloat("progress");
                int status = resultSet.getInt("status");
                // Now, you need to determine the fileType based on the URL or other means.
                String fileType = determineFileType(url);
                // Create a Download object with the correct fileType
                Download download = new Download(new URL(url), downloadFolder, fileType);
                download.setSize(size);
                download.setStatus(status);
                download.setDownloaded((long) (size * progress / 100));
                downloadHistory.add(download);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return downloadHistory;
    }
    private String determineFileType(String url) {
        String[] parts = url.split("\\.");
        if (parts.length > 0) {
            String extension = parts[parts.length - 1];
            return extension.toLowerCase(); // Return the file extension in lowercase
        }
        return "file"; // Return an empty string if no extension found
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
        ImageIcon img = new ImageIcon("icon3.png");
        manager.setIconImage(img.getImage());
        manager.show();
        System.out.println("main method executing");

    }
}

//https://gfgc.kar.nic.in/sirmv-science/GenericDocHandler/138-a2973dc6-c024-4d81-be6d-5c3344f232ce.pdf
//https://www.cs.cmu.edu/afs/cs.cmu.edu/user/gchen/www/download/java/LearnJava.pdf
//https://file-examples.com/storage/fe235481fb64f1ca49a92b5/2017/11/file_example_MP3_700KB.mp3
//https://file-examples.com/storage/fe235481fb64f1ca49a92b5/2017/11/file_example_MP3_5MG.mp3
//https://file-examples.com/storage/fe235481fb64f1ca49a92b5/2017/10/file_example_JPG_100kB.jpg
//https://file-examples.com/storage/fe235481fb64f1ca49a92b5/2017/10/file_example_PNG_1MB.png
//https://file-examples.com/storage/fe235481fb64f1ca49a92b5/2017/04/file_example_MP4_640_3MG.mp4