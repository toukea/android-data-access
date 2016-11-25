package istat.android.data.access.sqlite.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by istat on 31/10/16.
 */

public class Toolkit {
    public static final Class<?> getGenericTypeClass(Class<?> baseClass, int genericIndex) {
        try {
            String className = ((ParameterizedType) baseClass
                    .getGenericSuperclass()).getActualTypeArguments()[genericIndex]
                    .toString().replaceFirst("class", "").trim();
            Class<?> clazz = Class.forName(className);
            return clazz;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Class is not parametrized with generic type!!! Please use extends <> ");
        }
    }

    public static List<Field> getAllFieldIncludingPrivateAndSuper(Class<?> klass) {
        List<Field> fields = new ArrayList<Field>();
        while (!klass.equals(Object.class)) {
            for (Field field : klass.getDeclaredFields()) {
                if (field != null && field.toString().contains("static")) {
                    continue;
                }
                fields.add(field);
            }
            klass = klass.getSuperclass();
        }
        return fields;
    }
}
