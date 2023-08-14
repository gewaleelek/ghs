package ch.usi.si.seart.github;

import com.google.gson.JsonElement;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import okhttp3.Headers;
import org.springframework.http.HttpStatus;

/**
 * Represents a {@link Response} obtained from REST API calls.
 * Apart from the JSON representation of the response body,
 * this class also contains information on the returned HTTP status and headers.
 *
 * @since 1.6.3
 * @author Ozren Dabić
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RestResponse extends Response {

    HttpStatus status;
    Headers headers;

    public RestResponse(JsonElement jsonElement, HttpStatus status, Headers headers) {
        super(jsonElement);
        this.status = status;
        this.headers = headers;
    }
}