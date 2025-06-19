package de.jklein.fabric.utils;

import com.owlike.genson.Genson;

public final class JsonUtil {

    private static final Genson GENSON = new Genson();

    private JsonUtil() {
    }

    public static <T> T fromJson(final String jsonString, final Class<T> clazz) {
        return GENSON.deserialize(jsonString, clazz);
    }

    public static String toJson(final Object object) {
        return GENSON.serialize(object);
    }
}
