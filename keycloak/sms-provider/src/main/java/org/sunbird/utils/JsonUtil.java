package org.sunbird.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class JsonUtil {

    public static Map<String, String> readFromJson(String jsonFile) {
        ObjectMapper mapper = new ObjectMapper();

        // read JSON from a file
        Map<String, String> map = null;

        try {
            map = mapper.readValue(
                    new File(jsonFile),
                    new TypeReference<Map<String, String>>() {
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }


    public static String toJson(Object object) {
        Gson gsonObj = new Gson();

        return gsonObj.toJson(object);
    }

}
