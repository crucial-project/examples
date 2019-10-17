package crucial.withkeep.aws;

import com.amazonaws.util.IOUtils;

import java.io.*;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

/**
 * Serialize objects to string.
 *
 * @author Daniel
 */
public final class Serializer {
    private Serializer() {
    }

    public static String serialize(final Object obj) {
        String strs;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();
            strs = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            strs = "ERROR";
        }

        return strs;
    }

    public static List<String> serialize(
            final List<? extends Serializable> objectList) {
        List<String> strs = new LinkedList<>();
        for (Object obj : objectList) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(obj);
                oos.close();
                strs.add(Base64.getEncoder()
                        .encodeToString(baos.toByteArray()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return strs;
    }

    public static <T extends Serializable> List<T> deserialize(
            final List<String> strs) {
        List<T> objects = new LinkedList<>();
        byte[] data;
        for (String str : strs) {
            try {
                data = Base64.getDecoder().decode(str);
                ObjectInputStream ois =
                        new ObjectInputStream(new ByteArrayInputStream(data));
                objects.add((T) ois.readObject());
                ois.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return objects;
    }

    public static <T extends Serializable> T deserialize(final String str) {
        Object object = null;
        byte[] data;
        try {
            data = Base64.getDecoder().decode(str);
            ObjectInputStream ois =
                    new ObjectInputStream(new ByteArrayInputStream(data));
            object = ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return (T) object;
    }

    public static <T extends Serializable> T deserialize(
            final InputStream data) {
        Object object = null;
        try {
            InputStream is = new ObjectInputStream(
                    new ByteArrayInputStream(IOUtils.toByteArray(data)));
            ObjectInputStream ois = new ObjectInputStream(is);
            object = ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return (T) object;
    }
}
