package crucial.execution;

import java.io.*;

class ByteMarshaller {
    static byte[] toBytes(Object o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        return baos.toByteArray();
    }

    static <T> T fromBytes(byte[] input) throws IOException, ClassNotFoundException {
        return (T) new ObjectInputStream(new ByteArrayInputStream(input)).readObject();
    }
}
