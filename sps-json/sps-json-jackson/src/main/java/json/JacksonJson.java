package json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kildeen.sps.ExceptionUtils;
import com.kildeen.sps.json.Json;

public class JacksonJson implements Json {
    private final static ObjectMapper MAPPER = new ObjectMapper();


    @Override
    public <T> T readValue(String json, Class<T> clazz) {
        try {
            return JacksonJson.MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }
    @Override
    public byte[] writeValueAsBytes(Object object) {
        try {
            return JacksonJson.MAPPER.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }
    @Override
    public String write(Object object) {
        try {
            return JacksonJson.MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

}
