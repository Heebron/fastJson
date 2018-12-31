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
package mobi.thepratts.util.json;

import java.util.List;
import java.util.StringJoiner;

/**
 *
 * @author kpratt
 */
class JSONArray implements JValue<List<?>> {

    public JSONArray(List<?> value) {
        this.value = value;
    }
    List<?> value;

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(",", "[", "]");

        // Optimized output.
        value.stream().forEach(e -> {
            if (e == null) {
                sj.add("{}");
            } else if (e instanceof JValue) { // This means a JSONObject
                sj.add(((JValue) e).toString());
            } else if (e instanceof Boolean) {
                sj.add(e.toString());
            } else if (e instanceof Number) {
                sj.add(e.toString());
            } else {
                sj.add("\"" + JSONObject.escape(e.toString()) + "\"");
            }
        });
        return sj.toString();
    }

    @Override
    public List getValue() {
        return value;
    }

    public Object getValue(int index) {
        return value.get(index);
    }
}
