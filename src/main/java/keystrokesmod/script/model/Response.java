package keystrokesmod.script.model;

import java.util.Collections;
import java.util.List;

public class Response {

    private final int responseCode;

    private final String contents;

    private final List<String[]> headers;

    public Response(int responseCode, String contents, List<String[]> headers) {
        this.responseCode = responseCode;
        this.contents = contents;
        this.headers = headers == null ? Collections.emptyList() : headers;
    }

    public Response(int responseCode, String contents) {
        this(responseCode, contents, null);
    }

    public int code() {
        return this.responseCode;
    }

    public String string() {
        return this.contents;
    }

    public Json json() {
        return this.contents == null ? null : Json.parse(this.contents);
    }

    public List<String[]> headers() {
        return this.headers;
    }

    @Override
    public String toString() {
        return "Response(" + this.responseCode + ")";
    }
}
