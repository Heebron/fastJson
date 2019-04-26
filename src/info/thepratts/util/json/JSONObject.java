/*
 * MIT License
 *
 * Copyright (c) 2015 Ken Pratt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package info.thepratts.util.json;

import static info.thepratts.util.json.JSON._indent;
import static info.thepratts.util.json.JSON.escape;
import java.util.HashMap;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Represents a JSON object as a veneer on top of a Hash Map. This contains
 * convenience methods to read JSON related values based on a hash key and to
 * coerce number formats for easy processing.
 * <p>
 * This is not thread safe.
 *
 * @author Ken Pratt &lt;kenpratt@comcast.net&gt;
 *
 */
public class JSONObject extends HashMap<String, Object> {

    public JSONObject getJSONObject(String key) {
        return (JSONObject) get(key);
    }

    public JSONObject getJSONObject(String... key) {
        return (JSONObject) get(key);
    }

    public JSONArray getJSONArray(String key) {
        return (JSONArray) get(key);
    }

    public JSONArray getJSONArray(String... key) {
        return (JSONArray) get(key);
    }

    /**
     * Coerces any recognized number to a Long.
     *
     * @param key name of key
     * @return the value as a Java Long. This may involve rounding or
     * truncation.
     */
    public long getLong(String key) {
        Number n = (Number) super.get(key);
        return n.longValue();
    }

    /**
     * Coerces any recognized number to an Int.
     *
     * @param key name of key
     * @return the value as a Java Integer. This may involve rounding or
     * truncation.
     */
    public int getInt(String key) {
        Number n = (Number) super.get(key);
        return n.intValue();
    }

    /**
     * Coerces any recognized number to a Float.
     *
     * @param key name of key
     * @return the value as a Java Float. This may involve rounding or
     * truncation.
     */
    public float getFloat(String key) {
        Number n = (Number) super.get(key);
        return n.floatValue();
    }

    /**
     * Coerces any recognized number to a Double.
     *
     * @param key name of key
     * @return the value as a Java Double.
     */
    public double getDouble(String key) {
        Number n = (Number) super.get(key);
        return n.doubleValue();
    }

    /**
     * Returns the value mapped to key.
     *
     * @param <T> the Java type of the object mapped to key
     * @param key name of key
     * @return the value mapped to key
     */
    public <T> T get(String key) {
        return (T) super.get(key);
    }

    /**
     * Returns an Optional containing the value of type T mapped to key.
     *
     * @param <T> the Java type of the object mapped to key
     * @param key name of key
     * @return the value mapped to key
     * @see
     * <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html">Java
     * Optional</a>
     */
    public <T> Optional<T> opt(String key) {
        return Optional.ofNullable((T) get(key));
    }

    public <T> T get(String... keys) {
        Object v = get(keys[0]);
        for (int i = 1; i < keys.length; i++) {
            v = ((JSONObject) v).get(keys[i]);
        }
        return (T) v;
    }

    public <T> Optional<T> opt(String... key) {
        return Optional.ofNullable((T) get(key));
    }

    public String toString(int indent) {
        return _toString(0, indent);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(",", "{", "}");

        entrySet().forEach((e) -> {
            Object v = e.getValue();
            if (v == null) {
                sj.add(String.format("\"%s\":null", e.getKey()));
            } else if (v instanceof String) {
                sj.add(String.format("\"%s\":\"%s\"", e.getKey(), escape((String) e.getValue())));
            } else {
                sj.add(String.format("\"%s\":%s", e.getKey(), v.toString()));
            }
        });

        return sj.toString();
    }

    protected String _toString(int c, int indent) {
        if (isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        entrySet().forEach(e -> {
            if (sb.length() > 2) {
                sb.append(",\n");
            }
            _indent(sb, (1 + c) * indent);
            Object v = e.getValue();
            if (v == null) {
                sb.append(String.format("\"%s\":null", e.getKey()));
            } else if (v instanceof String) {
                sb.append(String.format("\"%s\":\"%s\"", e.getKey(), escape((String) e.getValue())));
            } else if (v instanceof JSONObject) {
                sb.append(String.format("\"%s\":%s", e.getKey(), ((JSONObject) v)._toString(c + 1, indent)));
            } else if (v instanceof JSONArray) {
                sb.append(String.format("\"%s\":%s", e.getKey(), ((JSONArray) v)._toString(c + 1, indent)));
            } else {
                sb.append(String.format("\"%s\":%s", e.getKey(), v.toString()));
            }
        });
        sb.append('\n');
        _indent(sb, c * indent);
        sb.append("}");
        return sb.toString();
    }
}
