package pt.ulisboa.tecnico.sec.depchain.common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import pt.ulisboa.tecnico.sec.depchain.common.exceptions.SerializationException;

public class ByteUtils {

    public static byte[] longToBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    public static byte[] serialize(Object... objects) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            for (Object o : objects) {
                oos.writeObject(o);
            }
            oos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    public static Object deserialize(byte[] bytes) throws ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

}
