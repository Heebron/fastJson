/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobi.thepratts.util.json;

/**
 *
 * @author kpratt
 */
class JsonEscaped<T> implements JValue<T> {

    private final T value;

    public JsonEscaped(T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "\"" + JsonObject.escape(value.toString()) + "\"";
    }

    @Override
    public T getValue() {
        return value;
    }

}
