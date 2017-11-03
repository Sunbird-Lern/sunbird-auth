package org.sunbird.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

//        String json = null;
//        try {
//            JSONParser parser = new JSONParser();
//            //Use JSONObject for simple JSON and JSONArray for array of JSON.
//            JSONObject data = (JSONObject) parser.parse(new FileReader(jsonFile));//path to the JSON file.
//             json = data.toString();
//        } catch (IOException | ParseException e) {
//            e.printStackTrace();
//        }

        return map;
    }

}
