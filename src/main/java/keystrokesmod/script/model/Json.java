package keystrokesmod.script.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

public class Json {

    private final JsonElement element;

    private final Json.Type type;

    private Json(JsonElement element) {
        this.element = element;
        if (element.isJsonObject()) {
            this.type = Json.Type.OBJECT;
        } else if (element.isJsonArray()) {
            this.type = Json.Type.ARRAY;
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive prim = element.getAsJsonPrimitive();
            if (prim.isBoolean()) {
                this.type = Json.Type.BOOLEAN;
            } else if (prim.isNumber()) {
                this.type = Json.Type.NUMBER;
            } else {
                this.type = Json.Type.STRING;
            }
        } else if (element.isJsonNull()) {
            this.type = Json.Type.NULL;
        } else {
            this.type = Json.Type.NULL;
        }
    }

    public static Json parse(String jsonString) {
        return new Json(new JsonParser().parse(jsonString));
    }

    public static Json object() {
        return new Json(new JsonObject());
    }

    public static Json array() {
        return new Json(new JsonArray());
    }

    public static Json string(String value) {
        return new Json(new JsonPrimitive(value));
    }

    public static Json number(Number value) {
        return new Json(new JsonPrimitive(value));
    }

    public static Json booleanValue(boolean value) {
        return new Json(new JsonPrimitive(value));
    }

    public static Json nullValue() {
        return new Json(JsonNull.INSTANCE);
    }

    public Json.Type type() {
        return this.type;
    }

    private void ensureObject() {
        if (this.type != Json.Type.OBJECT) {
            throw new IllegalStateException("Not a JSON object: " + this.type);
        }
    }

    public Json add(String key, Json value) {
        this.ensureObject();
        this.element.getAsJsonObject().add(key, value.element);
        return this;
    }

    public Json add(String key, String val) {
        return this.add(key, string(val));
    }

    public Json add(String key, Number val) {
        return this.add(key, number(val));
    }

    public Json add(String key, boolean val) {
        return this.add(key, booleanValue(val));
    }

    public Json get(String key) {
        this.ensureObject();
        JsonElement child = this.element.getAsJsonObject().get(key);
        return child == null ? nullValue() : new Json(child);
    }

    public boolean has(String key) {
        this.ensureObject();
        return this.element.getAsJsonObject().has(key);
    }

    private void ensureArray() {
        if (this.type != Json.Type.ARRAY) {
            throw new IllegalStateException("Not a JSON array: " + this.type);
        }
    }

    public Json add(Json value) {
        this.ensureArray();
        this.element.getAsJsonArray().add(value.element);
        return this;
    }

    public Json add(String val) {
        return this.add(string(val));
    }

    public Json add(Number val) {
        return this.add(number(val));
    }

    public Json add(boolean val) {
        return this.add(booleanValue(val));
    }

    public List<Json> asArray() {
        this.ensureArray();
        List<Json> list = new ArrayList<>();
        for (JsonElement el : this.element.getAsJsonArray()) {
            list.add(new Json(el));
        }
        return list;
    }

    private void ensurePrimitive() {
        if (this.type != Json.Type.STRING && this.type != Json.Type.NUMBER && this.type != Json.Type.BOOLEAN) {
            throw new IllegalStateException("Not a primitive: " + this.type);
        }
    }

    public String asString() {
        this.ensurePrimitive();
        return this.element.getAsString();
    }

    public int asInt() {
        this.ensurePrimitive();
        return this.element.getAsInt();
    }

    public double asDouble() {
        this.ensurePrimitive();
        return this.element.getAsDouble();
    }

    public long asLong() {
        this.ensurePrimitive();
        return this.element.getAsLong();
    }

    public float asFloat() {
        this.ensurePrimitive();
        return this.element.getAsFloat();
    }

    public boolean asBoolean() {
        this.ensurePrimitive();
        return this.element.getAsBoolean();
    }

    public LinkedHashSet<String> keys() {
        this.ensureObject();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (Entry<String, JsonElement> e : this.element.getAsJsonObject().entrySet()) {
            out.add(e.getKey());
        }
        return out;
    }

    public Json remove(String key) {
        this.ensureObject();
        this.element.getAsJsonObject().remove(key);
        return this;
    }

    public Json remove(int index) {
        this.ensureArray();
        if (index < 0) {
            return this;
        } else {
            JsonArray arr = this.element.getAsJsonArray();
            int i = 0;
            for (Iterator<JsonElement> it = arr.iterator(); it.hasNext(); i++) {
                it.next();
                if (i == index) {
                    it.remove();
                    break;
                }
            }
            return this;
        }
    }

    @Override
    public String toString() {
        return this.element.toString();
    }

    public static enum Type {

        OBJECT,
        ARRAY,
        STRING,
        NUMBER,
        BOOLEAN,
        NULL
    }
}
