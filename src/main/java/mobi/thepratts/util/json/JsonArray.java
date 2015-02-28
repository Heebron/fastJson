/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        value.stream().forEach(e -> {
            if (e instanceof JValue) {
                sj.add(((JValue) e).toString());
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
}
