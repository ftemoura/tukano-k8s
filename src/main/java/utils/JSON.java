package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

final public class JSON {
	public final static ObjectMapper mapper = new ObjectMapper()
			.registerModule(new JavaTimeModule());

	synchronized public static final String encode(Object obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return "";
		}
	}

	synchronized public static final <T> T decode(String json, Class<T> classOf) {
		try {
			var res = mapper.readValue(json, classOf);
			return res;
		} catch (JsonProcessingException e) {
			System.err.println("Error decoding JSON: " + json);
			e.printStackTrace();
			return null;
		}
	}

	synchronized public static final <T> T decode(String json, TypeReference<T> typeOf) {
		try {
			var res = mapper.readValue(json, typeOf);
			return res;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}
}