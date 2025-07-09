package pt.ulisboa.tecnico.sec.depchain.common.logging;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Properties;

public final class LoggerFactory {

    public static Logger getLogger(String name) {
        try {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            if (trace.length >= 4) {
                Class<?> caller = Class.forName(trace[3].getClassName());
                try (InputStream in = caller.getResourceAsStream("/logger.properties")) {
                    if (in != null) {
                        Properties props = new Properties();
                        props.load(in);
                        String clazzName = props.getProperty("logger");
                        Class<?> clazz = Class.forName(clazzName);
                        Constructor<?> c = clazz.getDeclaredConstructor(String.class);
                        Object instance = c.newInstance(name);
                        if (instance instanceof Logger logger) {
                            return logger;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new NopLogger();
    }

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getSimpleName());
    }

}
