package me.ele.amigo.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodUtils {

    public static Object invokeMethod(Object object, String methodName, Object[] args, Class<?>[] parameterTypes)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        parameterTypes = Utils.nullToEmpty(parameterTypes);
        args = Utils.nullToEmpty(args);
        Method method = getMatchedMethod(object.getClass(), methodName, parameterTypes);
        method.setAccessible(true);
        if (method == null) {
            throw new NoSuchMethodException("No such accessible method: "
                    + methodName + "() on object: "
                    + object.getClass().getName());
        }
        return method.invoke(object, args);
    }

    public static Object invokeStaticMethod(Class clazz, String methodName, Object[] args, Class<?>[] parameterTypes)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        parameterTypes = Utils.nullToEmpty(parameterTypes);
        args = Utils.nullToEmpty(args);
        Method method = getMatchedMethod(clazz, methodName, parameterTypes);
        method.setAccessible(true);
        if (method == null) {
            throw new NoSuchMethodException("No such accessible method: "
                    + methodName + "() on object: "
                    + clazz.getName());
        }
        return method.invoke(null, args);
    }

    public static Object invokeStaticMethod(Class clazz, String methodName, Object... args)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        args = Utils.nullToEmpty(args);
        Class<?>[] parameterTypes = Utils.toClass(args);
        return invokeStaticMethod(clazz, methodName, args, parameterTypes);
    }

    public static Object invokeMethod(Object object, String methodName, Object... args)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        args = Utils.nullToEmpty(args);
        Class<?>[] parameterTypes = Utils.toClass(args);
        return invokeMethod(object, methodName, args, parameterTypes);
    }


    public static Method getMatchedMethod(Class<?> cls, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {

        try {
            final Method method = cls.getDeclaredMethod(methodName, parameterTypes);
            MemberUtils.setAccessibleWorkaround(method);
            return method;
        } catch (final NoSuchMethodException e) {
            //ignore
        }
        // search through all methods
        Method bestMatch = null;
        final Method[] methods = cls.getDeclaredMethods();
        for (final Method method : methods) {
            // compare name and parameters
            if (method.getName().equals(methodName) && MemberUtils.isAssignable(parameterTypes, method.getParameterTypes(), true)) {
                bestMatch = method;
                final Method accessibleMethod = getMethodFromElse(method);
                if (accessibleMethod != null && (bestMatch == null || MemberUtils.compareParameterTypes(
                        accessibleMethod.getParameterTypes(),
                        bestMatch.getParameterTypes(),
                        parameterTypes) < 0)) {
                    bestMatch = accessibleMethod;
                }
            }
        }
        if (bestMatch != null) {
            MemberUtils.setAccessibleWorkaround(bestMatch);
        }
        return bestMatch;

    }

    private static Method getMethodFromElse(Method method) {
        // If the declaring class is public, we are done
        final Class<?> cls = method.getDeclaringClass();
        final String methodName = method.getName();
        final Class<?>[] parameterTypes = method.getParameterTypes();

        // Check the implemented interfaces and subinterfaces
        method = getAccessibleMethodFromInterfaceNest(cls, methodName, parameterTypes);

        // Check the superclass chain
        if (method == null) {
            method = getAccessibleMethodFromSuperclass(cls, methodName, parameterTypes);
        }
        return method;
    }

    private static Method getAccessibleMethodFromSuperclass(final Class<?> cls, final String methodName, final Class<?>... parameterTypes) {
        Class<?> parentClass = cls.getSuperclass();
        while (parentClass != null) {
            try {
                return parentClass.getDeclaredMethod(methodName, parameterTypes);
            } catch (final NoSuchMethodException e) {

            }
            parentClass = parentClass.getSuperclass();
        }
        return null;
    }


    private static Method getAccessibleMethodFromInterfaceNest(Class<?> cls, final String methodName, final Class<?>... parameterTypes) {
        // Search up the superclass chain
        for (; cls != null; cls = cls.getSuperclass()) {

            // Check the implemented interfaces of the parent class
            final Class<?>[] interfaces = cls.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                // Is this interface public?
                // Does the method exist on this interface?
                try {
                    return interfaces[i].getDeclaredMethod(methodName, parameterTypes);
                } catch (final NoSuchMethodException e) { // NOPMD
                    /*
                     * Swallow, if no method is found after the loop then this
                     * method returns null.
                     */
                }
                // Recursively check our parent interfaces
                Method method = getAccessibleMethodFromInterfaceNest(interfaces[i], methodName, parameterTypes);
                if (method != null) {
                    return method;
                }
            }
        }
        return null;
    }

}
