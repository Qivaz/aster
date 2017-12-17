package io.qivaz.aster.runtime.bundle.serialize;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.qivaz.aster.runtime.bundle.BundleRegistry;

/**
 * @author Qinghua Zhang @create 2017/7/7.
 */

public class GsonUtil {
    private static final Gson gson = new Gson();

    public static <T> String listToString(List<BundleRegistry.BundleItem> list) {
        return gson.toJson(list);
    }

    public static <T> List<T> stringToList(String s, Class<T[]> clazz) {
        T[] arr = gson.fromJson(s, clazz);
        return new LinkedList<>(Arrays.asList(arr)); //or return Arrays.asList(new Gson().fromJson(s, clazz)); for a one-liner
    }

    public static String mapToString(Map map) {
        return gson.toJson(map);
    }

    public static Map stringToMap(String json) {
        Type typeOfHashMap = new TypeToken<Map<String, ArrayList<String>>>() {
        }.getType();
        Map map = gson.fromJson(json, typeOfHashMap); // This type must match TypeToken
        return map;
    }
}
