package ch.usi.si.seart.github;

import org.springframework.retry.RetryCallback;

/**
 * The base abstract class for all GitHub API connector callbacks.
 * This class implements the Spring {@link RetryCallback} interface,
 * providing a structure for handling retries of failed operations
 * that yield instances of {@link JsonResponse} subclasses.
 *
 * @param <R> The type of response that this callback operates on.
 * @author Ozren Dabić
 */
public interface Callback<R extends JsonResponse> extends RetryCallback<R, Exception> {
}
