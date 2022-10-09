package help;

import java.util.Date;

public class SimpleLog {
    public static void log(String message) {
        System.out.println("INFO " + "[ " + new Date() + " ]: " + message);
    }

    public static void err(String message) {
        System.err.println("ERROR " + "[ " + new Date() + " ]: " + message);
    }
}
