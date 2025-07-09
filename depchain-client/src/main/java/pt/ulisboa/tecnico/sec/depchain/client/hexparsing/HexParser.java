package pt.ulisboa.tecnico.sec.depchain.client.hexparsing;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import pt.ulisboa.tecnico.sec.depchain.client.hexparsing.parsers.BigIntegerHexParser;
import pt.ulisboa.tecnico.sec.depchain.client.hexparsing.parsers.StringHexParser;

public class HexParser {

    private final Map<Class<?>, HexTypeAdapter<?>> adapters;

    private HexParser(Map<Class<?>, HexTypeAdapter<?>> adapters) {
        this.adapters = adapters;
    }

    public <T> String toHexString(T object) {
        Class<?> clazz = object.getClass();
        @SuppressWarnings("unchecked")
        HexTypeAdapter<? super T> adapter = (HexTypeAdapter<? super T>) this.adapters.get(clazz);
        return adapter.toHexString(object);
    }

    public boolean hasAdapter(Class<?> clazz) {
        return this.adapters.containsKey(clazz);
    }

    public static HexParserBuilder builder() {
        return new HexParserBuilder();
    }

    public static final class HexParserBuilder {

        private final Map<Class<?>, HexTypeAdapter<?>> adapters = new HashMap<>();

        public HexParserBuilder defaultAdapters() {
            registerTypeAdapter(BigInteger.class, new BigIntegerHexParser());
            registerTypeAdapter(String.class, new StringHexParser());
            return this;
        }

        public <T> HexParserBuilder registerTypeAdapter(Class<T> clazz, HexTypeAdapter<? super T> adapter) {
            this.adapters.put(clazz, adapter);
            return this;
        }

        public HexParser build() {
            return new HexParser(adapters);
        }
    }

}
