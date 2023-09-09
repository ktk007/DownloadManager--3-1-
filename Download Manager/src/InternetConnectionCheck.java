import java.net.InetAddress;
import java.net.UnknownHostException;

public class InternetConnectionCheck {
    public static boolean isInternetAvailable() {
        try {
            InetAddress address = InetAddress.getByName("www.google.com");
            // Possibly use InetAddress.isReachable(timeout) here
            return !address.equals("");
        } catch (UnknownHostException e) {
            // Host not found means no internet connection
            return false;
        }
    }
}
