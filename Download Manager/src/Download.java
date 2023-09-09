import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Observable;




// This class downloads a file from a URL.
class Download extends Observable implements Runnable {


    private String fileType; // Add this attribute

    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/download_manager_db";
        String username = "root";
        String password = "admin";

        return DriverManager.getConnection(url, username, password);
    }



    // Max size of download buffer.
    private static final int MAX_BUFFER_SIZE = 1024;

    // These are the status names.
    public static final String STATUSES[] = { "Downloading",
            "Paused", "Completed", "Cancelled", "Error" };

    // These are the status codes.
    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;

    private URL url; // download URL
    private long size; // size of download in bytes
    private long downloaded; // number of bytes downloaded
    private int status; // current status of download
    private long initTime; // inital time when download started or resumed
    private long startTime; // start time for current bytes
    private long readSinceStart; // number of bytes downloaded since startTime
    private long elapsedTime = 0; // elapsed time till now
    private long prevElapsedTime = 0; // time elapsed before resuming download
    private long remainingTime = -1; // time remaining to finish download
    private float avgSpeed = 0; // average download speed in KB/s
    private float speed = 0; // download speed in KB/s
   // private static File downloadFolder;// Add a variable to store the download folder
// Add a new field to store the download folder
   private File downloadFolder;

    // Constructor for Download.

    public Download(URL url, File downloadFolder,String fileType) {
        this.url = url;
        this.downloadFolder = downloadFolder;
        this.fileType=fileType;

        // Set the download folder
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;
        // Begin the download.
        download();
    }



    // Get this download's URL.
    public String getUrl() {
        return url.toString();
    }

    // Get this download's size.
    public long getSize() {
        return size;
    }

    // Get download speed.
    public float getSpeed() {
        return speed;
    }

    // Get average speed
    public float getAvgSpeed() {
        return avgSpeed;
    }

    // Get elapsed time
    public String getElapsedTime() {
        return formatTime(elapsedTime / 1000000000);
    }

    // Get remaining time
    public String getRemainingTime() {
        if (remainingTime < 0)
            return "Unknown";
        else
            return formatTime(remainingTime);
    }

    // Format time
    public String formatTime(long time) { // time in seconds
        String s = "";
        s += (String.format("%02d", time / 3600)) + ":";
        time %= 3600;
        s += (String.format("%02d", time / 60)) + ":";
        time %= 60;
        s += String.format("%02d", time);
        return s;
    }

    // Get this download's progress.
    public float getProgress() {
        return ((float) downloaded / size) * 100;
    }

    // Get this download's status.
    public int getStatus() {
        return status;
    }

    // Pause this download.
    public void pause() {
        prevElapsedTime = elapsedTime;
        status = PAUSED;
        stateChanged();
    }

    // Resume this download.
    public void resume() {
        status = DOWNLOADING;
        stateChanged();
        download();
    }

    // Cancel this download.
    public void cancel() {
        prevElapsedTime = elapsedTime;
        status = CANCELLED;
        stateChanged();
    }

    // Mark this download as having an error.
    private void error() {
        prevElapsedTime = elapsedTime;
        status = ERROR;
        stateChanged();
    }




    // Start or resume downloading.
    private void download() {
        Thread thread = new Thread(this);
        thread.start();
    }
    // Called when the download is complete.
    private void downloadComplete() {
        status = COMPLETE;
        stateChanged();

        // Insert download data into the database
        try {
            String categoryName = mapFileTypeToCategory(getFileType()); // Set the categoryName based on your category mapping
            insertDownloadDataIntoDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    // Get file name portion of URL.
     String getFileName(URL url) {
         String fileName = this.url.getFile();
         String fileExtension = fileType != null && !fileType.isEmpty() ? "." + fileType : "";

        // Extract the file name without extension
        String nameWithoutExtension = fileName.substring(fileName.lastIndexOf('/') + 1);

        // Sanitize the name (remove invalid characters)
        nameWithoutExtension = nameWithoutExtension.replaceAll("[^a-zA-Z0-9.-]", "_");

        return nameWithoutExtension + fileExtension;
        //return fileName.substring(fileName.lastIndexOf('/') + 1);
    }

    // Download file.
    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;

        try {

            // Open connection to URL.
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Specify what portion of file to download.
            connection.setRequestProperty("Range",
                    "bytes=" + downloaded + "-");

            // Connect to server.
            connection.connect();

            // Make sure response code is in the 200 range.
            if (connection.getResponseCode() / 100 != 2) {
                error();
            }

            // Check for valid content length.
            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                error();
            }

            /*
             * Set the size for this download if it
             * hasn't been already set.
             */
            if (size == -1) {
                size = contentLength;
                stateChanged();
            }
            // used to update speed at regular intervals
            int i = 0;
            // Open file and seek to the end of it.
            file = new RandomAccessFile(new File(downloadFolder, getFileName(url)), "rw");
            file.seek(downloaded);

            stream = connection.getInputStream();
            initTime = System.nanoTime();
            while (status == DOWNLOADING) {
                /*
                 * Size buffer according to how much of the
                 * file is left to download.
                 */
                if (i == 0) {
                    startTime = System.nanoTime();
                    readSinceStart = 0;
                }
                byte buffer[];
                if (size - downloaded > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[(int) (size - downloaded)];
                }
                // Read from server into buffer.
                int read = stream.read(buffer);
                if (read == -1)
                    break;
                // Write buffer to file.
                file.write(buffer, 0, read);
                downloaded += read;
                readSinceStart += read;
                // update speed
                i++;
                if (i >= 50) {
                    speed = (readSinceStart * 976562.5f) / (System.nanoTime() - startTime);
                    if (speed > 0)
                        remainingTime = (long) ((size - downloaded) / (speed * 1024));
                    else
                        remainingTime = -1;
                    elapsedTime = prevElapsedTime + (System.nanoTime() - initTime);
                    avgSpeed = (downloaded * 976562.5f) / elapsedTime;
                    i = 0;
                }
                stateChanged();
            }

            /*
             * Change status to complete if this point was
             * reached because downloading has finished.
             */
            if (status == DOWNLOADING) {
                status = COMPLETE;
                downloadComplete();
                stateChanged();

            }
        } catch (Exception e) {
            System.out.println(e);
            error();
        } finally {
            // Close file.
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {
                }
            }

            // Close connection to server.
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                }
            }
        }
        System.out.println("Download.run() method called.");

    }

    private void insertDownloadDataIntoDatabase() throws SQLException {
        String sql = "INSERT INTO download_history (url, size, progress, status, fileType, category) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, getUrl());
            statement.setLong(2, getSize());
            statement.setFloat(3, getProgress());
            statement.setInt(4, getStatus());
            statement.setString(5, getFileType());
            String categoryName = mapFileTypeToCategory(getFileType());
            statement.setString(6, categoryName);

            statement.executeUpdate();
            System.out.println("Download inserted into the database: " + getUrl());
        }
    }

    // Notify observers that this download's status has changed.
    private void stateChanged() {
        setChanged();
        notifyObservers();
    }

    public void setStatus(int status) throws SQLException {
        this.status = status;
        // Insert download data into the database
        //updateDatabase();// Insert or update database based on status
        stateChanged();
    }

    public void setSize(long size) {
        this.size = size;
        stateChanged();
    }

    public void setDownloaded(long downloaded) {
        this.downloaded = downloaded;
        stateChanged();
    }
    private String mapFileTypeToCategory(String fileType) {
        if (fileType == null) {
            return "Others";
        }
        switch (fileType.toLowerCase()) {
            case "pdf":
                return "PDF Documents";
            case "mp3":
            case "wav":
            case "aac":
                return "Music";
            case "mp4":
            case "mkv":
            case "avi":
                return "Videos";
            case "doc":
            case "docx":
                return "Documents";
            default:
                return "Others";
        }
    }

    // Getter for fileType
    public String getFileType() {
        return fileType;
    }


}