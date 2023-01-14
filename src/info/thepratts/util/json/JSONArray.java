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

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringJoiner;

import static info.thepratts.util.json.JSON._indent;
import static info.thepratts.util.json.JSON.escape;

/**
 * This is not thread safe.
 *
 * @author Ken Pratt &lt;kenpratt@comcast.net&gt;
 * @param <T> type of contained elements
 */
public class JSONArray<T> extends ArrayList<T> {

    public JSONArray() {
        super();
    }

    public JSONArray(Collection<? extends T> c) {
        super(c);
    }

    protected String _toString(int c, int indent) {
        if (isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        forEach(v -> {
            if (sb.length() > 2) {
                sb.append(",\n");
            }
            _indent(sb, (1 + c) * indent);

            if (v == null) {
                sb.append("null");
            } else if (v instanceof String) {
                sb.append('"').append(escape((String) v)).append('"');
            } else if (v instanceof JSONObject) {
                sb.append(((JSONObject) v)._toString(c + 1, indent));
            } else if (v instanceof JSONArray) {
                sb.append(((JSONArray<?>) v)._toString(c + 1, indent));
            } else {
                sb.append(v);
            }
        });
        sb.append('\n');
        _indent(sb, c * indent);
        sb.append(']');

        return sb.toString();
    }

    public String toString(int indent) {
        return _toString(0, indent);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        this.forEach(e -> {
            if (e == null) {
                sj.add("null");
            } else if (e instanceof String) {
                sj.add("\"" + escape(e.toString()) + "\"");
            } else {
                sj.add(e.toString());
            }
        });
        return sj.toString();
    }
}
