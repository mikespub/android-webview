package net.mikespub.mywebview;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Reflect Utility Methods
 */
// http://tutorials.jenkov.com/java-reflection/getters-setters.html
class MyReflectUtility {
    private static final String TAG = "Reflect";
    private static final Map<String,Class> builtInMap = new HashMap<>();

    static {
        builtInMap.put("int", Integer.class );
        builtInMap.put("long", Long.class );
        builtInMap.put("double", Double.class );
        builtInMap.put("float", Float.class );
        builtInMap.put("boolean", Boolean.class );
        builtInMap.put("char", Character.class );
        builtInMap.put("byte", Byte.class );
        builtInMap.put("void", Void.class );
        builtInMap.put("short", Short.class );
    }

    /**
     * @param var           variable to compare
     * @param methodName    method for comparison
     * @param value         value to compare with
     * @return              comparison
     */
    // https://stackoverflow.com/questions/160970/how-do-i-invoke-a-java-method-when-given-the-method-name-as-a-string
    // TODO: create hashmap of methods?
    static boolean stringCompare(String var, String methodName, String value) {
        if (var == null) {
            return false;
        }
        // with single parameter, return boolean
        try {
            Method method;
            if (methodName.equals("equals")) {
                method = var.getClass().getMethod(methodName, Object.class);
            } else if (methodName.equals("contains")) {
                method = var.getClass().getMethod(methodName, CharSequence.class);
            } else {
                method = var.getClass().getMethod(methodName, value.getClass());
            }
            boolean result = (boolean) method.invoke(var, value); // pass arg
            Log.d(TAG, var + " " + methodName + " " + value + " = " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, var + " " + methodName + " " + value + " = ERROR", e);
            return false;
        }
    }

    /**
     * Get all getter values for an object instance
     *
     * @param objectInstance    object instance
     * @return                  getter values
     */
    static Map<String, Object> getValues(Object objectInstance) {
        Map<String, Object> hashMap = new HashMap<>();
        //Class aClass = MyObject.class;
        Class aClass = objectInstance.getClass();
        //String className = aClass.getName();
        //Log.d(TAG, "Class: " + className);
        //int modifiers = aClass.getModifiers();
        Method[] methods = aClass.getMethods();
        for (Method method: methods) {
            String methodName = method.getName();
            if (methodName.equals("getClass")) {
                continue;
            }
            //int modifiers = method.getModifiers();
            //Object returnValue = method.invoke(objectInstance, "parameter-value1");
            if (isGetter(method)) {
                try {
                    Object returnValue = method.invoke(objectInstance);
                    hashMap.put(methodName.substring(3), returnValue);
                } catch (Exception e) {
                    Log.e(TAG, methodName, e);
                }
            }
        }
        return hashMap;
    }

    /**
     * Is this object method a getter
     *
     * @param method    object method
     * @return          is getter
     */
    static boolean isGetter(Method method){
        if(!method.getName().startsWith("get"))      return false;
        if(method.getParameterTypes().length != 0)   return false;
        return !void.class.equals(method.getReturnType());
    }

    /**
     * Is this object method a setter
     *
     * @param method    object method
     * @return          is setter
     */
    static boolean isSetter(Method method){
        if(!method.getName().startsWith("set")) return false;
        return method.getParameterTypes().length == 1;
    }

    /**
     * Get a value via getter for an object instance
     *
     * @param objectInstance    object instance
     * @param getName           getter name without "get"
     */
    static Object get(Object objectInstance, String getName) {
        Class aClass = objectInstance.getClass();
        //Method method = aClass.getMethod("get" + getName);
        Method[] methods = aClass.getMethods();
        for (Method method: methods) {
            String methodName = method.getName();
            if (!methodName.equals("get" + getName)) {
                continue;
            }
            if (!isGetter(method)) {
                continue;
            }
            try {
                Object returnValue = method.invoke(objectInstance);
                return returnValue;
            } catch (Exception e) {
                Log.e(TAG, methodName, e);
                return null;
            }
        }
        return null;
    }

    /**
     * Set a value via setter for an object instance
     *
     * @param objectInstance    object instance
     * @param setName           setter name without "set"
     * @param value             value to set
     */
    static void set(Object objectInstance, String setName, Object value) {
        Class aClass = objectInstance.getClass();
        //Method method = aClass.getMethod("set" + setName);
        Method[] methods = aClass.getMethods();
        for (Method method: methods) {
            String methodName = method.getName();
            if (!methodName.equals("set" + setName)) {
                continue;
            }
            if (!isSetter(method)) {
                continue;
            }
            Class[] parameterTypes = method.getParameterTypes();
            //Type[] genericParameterTypes = method.getGenericParameterTypes();
            // https://stackoverflow.com/questions/54446200/is-there-a-way-to-compare-a-primitive-type-with-its-corresponding-wrapper-type
            // https://stackoverflow.com/questions/180097/dynamically-find-the-class-that-represents-a-primitive-java-type/180139#180139
            if (parameterTypes[0].isPrimitive()) {
                Log.d(TAG, parameterTypes[0].getName());
                Class equivClass = builtInMap.get(parameterTypes[0].getName());
                if (equivClass.isInstance(value)) {
                    Log.d(TAG, "Method: " + methodName + " - Param Type: " + equivClass.getName() + " - Value: " + value.getClass() + " MATCH");
                    try {
                        method.invoke(objectInstance, value);
                    } catch (Exception e) {
                        Log.e(TAG, methodName, e);
                    }
                    return;
                } else {
                    Log.d(TAG, "Method: " + methodName + " - Param Type: " + equivClass.getName() + " - Value: " + value.getClass() + " NO MATCH");
                }
            } else {
                if (parameterTypes[0].isInstance(value)) {
                    Log.d(TAG, "Method: " + methodName + " - Param Type: " + parameterTypes[0] + " - Value: " + value.getClass() + " MATCH");
                    if (parameterTypes[0].getName().contains("$")) {
                        //Annotation[] annotations = method.getDeclaredAnnotations();
                        //for (Annotation annotation: annotations) {
                        //    Log.d(TAG, "Annotation: " + annotation.toString());
                        //}
                        try {
                            Method getValues = parameterTypes[0].getMethod("values");
                            Object[] values = (Object[]) getValues.invoke(null);
                            Log.d(TAG, "Enum: " + Arrays.toString(values));
                        } catch (Exception e) {
                            Log.e(TAG, methodName, e);
                        }
                    }
                    try {
                        method.invoke(objectInstance, value);
                    } catch (Exception e) {
                        Log.e(TAG, methodName, e);
                    }
                    return;
                } else {
                    Log.d(TAG, "Method: " + methodName + " - Param Type: " + parameterTypes[0] + " - Value: " + value.getClass() + " NO MATCH");
                }
            }
        }
    }

    /**
     * Show details of an object instance via reflection
     *
     * @param objectInstance object instance
     */
    static void showObject(Object objectInstance) {
        //Class aClass = MyObject.class;
        Class aClass = objectInstance.getClass();
        String className = aClass.getName();
        String simpleClassName = aClass.getSimpleName();
        Log.d(TAG, "Class: " + className);
        //Package aPackage = aClass.getPackage();
        //Log.d(TAG, "Package: " + aPackage.toString());
        //int modifiers = aClass.getModifiers();
        Field[] fields = aClass.getFields();
        for (Field field: fields) {
            String message = simpleClassName;
            message += showField(field);
            //field.set(objectInstance, value);
            try {
                Object value = field.get(objectInstance);
                Log.d(TAG, message + value);
            } catch (Exception e) {
                Log.e(TAG, message, e);
            }
        }
        Method[] methods = aClass.getMethods();
        for (Method method: methods) {
            String message = simpleClassName;
            message += showMethod(method);
            //Object returnValue = method.invoke(objectInstance, "parameter-value1");
            if (isGetter(method)) {
                try {
                    Object returnValue = method.invoke(objectInstance);
                    Log.d(TAG, message + returnValue);
                } catch (Exception e) {
                    Log.e(TAG, message, e);
                }
            } else {
                Log.d(TAG, message);
            }
        }
    }

    /**
     * Get description of a class field
     *
     * @param field class field
     * @return      description
     */
    static String showField(Field field) {
        String fieldName = field.getName();
        Object fieldType = field.getType();
        Type genericFieldType = field.getGenericType();
        String message = " Field: " + fieldName;
        //field.set(objectInstance, value);
        if(genericFieldType instanceof ParameterizedType) {
            ParameterizedType aType = (ParameterizedType) genericFieldType;
            Type[] fieldArgTypes = aType.getActualTypeArguments();
            Class[] fieldArgClasses = new Class[fieldArgTypes.length];
            int i = 0;
            for (Type fieldArgType : fieldArgTypes) {
                if (fieldArgType instanceof WildcardType) {
                    fieldArgType = WildcardType.class;
                }
                Class fieldArgClass = (Class) fieldArgType;
                fieldArgClasses[i] = fieldArgClass;
                i++;
            }
            message += " - Generic: " + genericFieldType + " - Classes: " + Arrays.toString(fieldArgClasses);
        } else if (field.getType().isArray()) {
            Class arrayComponentType = field.getType().getComponentType();
            // for two-dimensional arrays like MyAppWebViewClient.myMatchCompare (if it was public)
            if (arrayComponentType.isArray()) {
                Class arraySubcomponentType = arrayComponentType.getComponentType();
                message += " - Type: " + fieldType + " - Array: " + arrayComponentType + " of " + arraySubcomponentType;
            } else {
                message += " - Type: " + fieldType + " - Array: " + arrayComponentType;
            }
        } else {
            message += " - Type: " + fieldType;
        }
        message += " - Value: ";
        return message;
    }

    /**
     * Get description of a class method
     *
     * @param method    class method
     * @return          description
     */
    static String showMethod(Method method) {
        String methodName = method.getName();
        Class[] parameterTypes = method.getParameterTypes();
        //Type[] genericParameterTypes = method.getGenericParameterTypes();
        Class returnType = method.getReturnType();
        Type genericReturnType = method.getGenericReturnType();
        int modifiers = method.getModifiers();
        //Object returnValue = method.invoke(objectInstance, "parameter-value1");
        String message = "";
        if (isGetter(method)) {
            message += " Getter: ";
        } else if (isSetter(method)) {
            message += " Setter: ";
        } else {
            message += " Method: ";
        }
        if (Modifier.isStatic(modifiers)) {
            message += "static ";
        }
        message += methodName;
        if(genericReturnType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) genericReturnType;
            Type[] typeArguments = type.getActualTypeArguments();
            Class[] typeArgClasses = new Class[typeArguments.length];
            int i = 0;
            for (Type typeArgument : typeArguments) {
                if (typeArgument instanceof WildcardType) {
                    typeArgument = WildcardType.class;
                }
                Class typeArgClass = (Class) typeArgument;
                typeArgClasses[i] = typeArgClass;
                i++;
            }
            message += " - Generic: " + genericReturnType + " - Classes: " + Arrays.toString(typeArgClasses);
        } else {
            message += " - Type: " + returnType;
        }
        if (isGetter(method)) {
            message += " - Value: ";
        } else if (isSetter(method)) {
            message += " - Param: " + parameterTypes[0];
        } else {
            message += " - Param: " + Arrays.toString(parameterTypes);
        }
        return message;
    }
}
