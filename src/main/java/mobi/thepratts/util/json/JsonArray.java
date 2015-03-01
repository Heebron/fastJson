/*
 * Copyright (C) 2015 kpratt
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package mobi.thepratts.util.json;

import java.util.List;
import java.util.StringJoiner;

/**
 *
 * @author kpratt
 */
class JsonArray implements JValue<List<?>> {

    public JsonArray(List<?> value) {
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
                sj.add("\"" + JsonObject.escape(e.toString()) + "\"");
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
