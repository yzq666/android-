//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtil {
    public ReflectUtil() {
    }

    public static Object getValue(Object instance, String fieldName) throws IllegalAccessException, NoSuchFieldException {
        Field field = getField(instance.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }

    public static Field getField(Class<?> thisClass, String fieldName) throws NoSuchFieldException {
        if(thisClass == null) {
            throw new NoSuchFieldException("Error field !");
        } else {
            try {
                return thisClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException var3) {
                return getField(thisClass.getSuperclass(), fieldName);
            }
        }
    }

    public static Method getMethod(Object instance, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
        Method accessMethod = getMethod(instance.getClass(), methodName, parameterTypes);
        accessMethod.setAccessible(true);
        return accessMethod;
    }

    private static Method getMethod(Class<?> thisClass, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
        if(thisClass == null) {
            throw new NoSuchMethodException("Error method !");
        } else {
            try {
                return thisClass.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException var4) {
                return getMethod(thisClass.getSuperclass(), methodName, parameterTypes);
            }
        }
    }

    public static Object invokeMethod(Object instance, String methodName, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class[] parameterTypes = null;
        if(args != null) {
            parameterTypes = new Class[args.length];

            for(int i = 0; i < args.length; ++i) {
                if(args[i] != null) {
                    parameterTypes[i] = args[i].getClass();
                }
            }
        }

        return getMethod(instance, methodName, parameterTypes).invoke(instance, args);
    }
}
