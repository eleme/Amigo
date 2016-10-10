package me.ele.amigo.compat;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static me.ele.amigo.reflect.MethodUtils.getMatchedMethod;

public class RCompat {

    private static Resources resources;

    private static final String[] classNames = {"R$anim", "R$attr", "R$bool", "R$color",
            "R$dimen", "R$drawable", "R$id", "R$integer",
            "R$layout", "R$menu", "R$mipmap", "R$string",
            "R$style", "R$styleable"};
    private static final List<Class> classes = new ArrayList<>(classNames.length);
    private static final Map<Class, Map<Field, Integer>> allFields = new HashMap<>();

    public static int getIdentifier(Context context, int id) {
        Resources resources = getResources(context);
        Pair<String, String> pair = getIdInfo(context, id);
        return resources.getIdentifier(pair.first, pair.second, context.getPackageName());
    }

    private static Pair<String, String> getIdInfo(Context context, int id) {
        initClasses(context);
        initAllFields();
        Field hitField = null;
        Iterator<Map.Entry<Class, Map<Field, Integer>>> iterator = allFields.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Class, Map<Field, Integer>> entry = iterator.next();
            Map<Field, Integer> map = entry.getValue();
            if (!map.isEmpty()) {
                Iterator<Map.Entry<Field, Integer>> it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Field, Integer> en = it.next();
                    if (en.getValue() == id) {
                        hitField = en.getKey();
                        break;
                    }
                }
            }
            if (hitField != null) {
                break;
            }
        }

        if (hitField == null) {
            return null;
        }

        String name = hitField.getName();
        String defType = hitField.getDeclaringClass().getSimpleName();
        return new Pair<>(name, defType);
    }

    private static void initClasses(Context context) {
        if (classes.size() == 0) {
            for (int i = 0; i < classNames.length; i++) {
                try {
                    classes.add(Class.forName(context.getPackageName() + "." + classNames[i]));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void initAllFields() {
        if (allFields.isEmpty()) {
            for (Class<?> clazz : classes) {
                if (allFields.get(clazz) == null) {
                    allFields.put(clazz, new HashMap<Field, Integer>());
                }
                Map<Field, Integer> map = allFields.get(clazz);
                Field[] fields = clazz.getDeclaredFields();
                if (fields.length > 0) {
                    for (Field field : fields) {
                        try {
                            Object value = field.get(null);
                            if (value.getClass() == int.class || value.getClass() == Integer.class) {
                                map.put(field, (int) value);
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        }
    }


    private static Resources getResources(Context context) {
        if (resources != null) {
            return resources;
        }

        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = getMatchedMethod(AssetManager.class, "addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            addAssetPath.invoke(assetManager, context.getApplicationInfo().sourceDir);
            return resources = new Resources(assetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
