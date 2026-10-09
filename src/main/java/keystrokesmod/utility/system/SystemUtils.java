package keystrokesmod.utility.system;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import keystrokesmod.utility.Utils;

public class SystemUtils {

    public static String getHardwareIdForLoad(String url) {
        String hashedId = "";
        try {
            MessageDigest instance = MessageDigest.getInstance("MD5");
            String input = System.currentTimeMillis() / 20000L + 29062381L + "J{LlrPhHgj8zy:uB" + System.getenv("COMPUTERNAME") + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_LEVEL") + Runtime.getRuntime().availableProcessors() + url;
            return String.format("%032x", new BigInteger(1, instance.digest(input.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception exception) {
            exception.printStackTrace();
            return hashedId;
        }
    }

    public static void addToClipboard(String string) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection stringSelection = new StringSelection(string);
            clipboard.setContents(stringSelection, null);
        } catch (Exception exception2) {
            Utils.sendMessage("&cFailed to copy &b" + string);
        }
    }
}
