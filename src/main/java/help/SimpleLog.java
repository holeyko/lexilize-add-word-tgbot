package help;

import java.util.Date;

public class SimpleLog {
    public static void log(String message) {
        System.out.println("[ " + new Date() + " ]: " + message);
    }
}
