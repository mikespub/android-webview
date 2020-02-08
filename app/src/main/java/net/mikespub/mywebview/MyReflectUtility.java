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

// http://tutorials.jenkov.com/java-reflection/getters-setters.html
class MyReflectUtility {
    private static final String TAG = "Reflect";

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
                    Log.e(TAG, methodName + ": " + e.toString());
                }
            }
        }
        return hashMap;
    }

    static boolean isGetter(Method method){
        if(!method.getName().startsWith("get"))      return false;
        if(method.getParameterTypes().length != 0)   return false;
        return !void.class.equals(method.getReturnType());
    }

    static boolean isSetter(Method method){
        if(!method.getName().startsWith("set")) return false;
        return method.getParameterTypes().length == 1;
    }

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
                Log.d(TAG, message + e.toString());
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
                    Log.d(TAG, message + e.toString());
                }
            } else {
                Log.d(TAG, message);
            }
        }
    }

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
