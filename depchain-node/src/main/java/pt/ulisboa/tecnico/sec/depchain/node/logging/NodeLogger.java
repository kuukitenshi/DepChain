package pt.ulisboa.tecnico.sec.depchain.node.logging;

import java.text.SimpleDateFormat;
import java.util.Date;

import pt.ulisboa.tecnico.sec.depchain.common.logging.ANSIColors;
import pt.ulisboa.tecnico.sec.depchain.common.logging.LogLevel;
import pt.ulisboa.tecnico.sec.depchain.common.logging.Logger;

public class NodeLogger implements Logger {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private final String name;

    public NodeLogger(String name) {
        this.name = name;
    }

    @Override
    public void log(LogLevel level, String format, Object... args) {
        Date now = new Date();
        String formattedDate = DATE_FORMAT.format(now);
        String message = String.format(format, args);
        String threadName = Thread.currentThread().getName();
        String log = String.format("[%s] (%s) %s %s > %s", formattedDate, threadName, level, this.name, message);
        String color = switch (level) {
            case DEBUG -> ANSIColors.MAGENTA_FG;
            case INFO -> ANSIColors.GREEN_FG;
            case WARNING -> ANSIColors.YELLOW_FG;
            case ERROR -> ANSIColors.RED_FG;
            default -> ANSIColors.RESET;
        };
        System.out.println(color + log + ANSIColors.RESET);
    }

}
