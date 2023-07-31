package usi.si.seart.github;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import usi.si.seart.exception.github.GitHubAPIException;
import usi.si.seart.util.Ranges;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ConstantConditions")
@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GitHubAPIConnector {

    @NonFinal
    @Value("${app.crawl.minimum-stars}")
    Integer minimumStars;

    Pattern headerLinkPattern;

    OkHttpClient client;

    RetryTemplate retryTemplate;

    Function<Date, String> dateStringMapper;

    GitHubTokenManager gitHubTokenManager;

    ConversionService conversionService;

    public JsonObject searchRepositories(String language, Range<Date> dateRange, Integer page) {
        Map<String, String> query = ImmutableMap.<String, String>builder()
                .put("language", URLEncoder.encode(language, StandardCharsets.UTF_8))
                .put("pushed", Ranges.toString(dateRange, dateStringMapper))
                .put("stars", String.format(">=%d", minimumStars))
                .put("fork", "true")
                .put("is", "public")
                .build();

        String joined = Joiner.on("+").withKeyValueSeparator(":").join(query);

        URL url = HttpUrl.get(Endpoint.SEARCH_REPOSITORIES)
                .newBuilder()
                .setEncodedQueryParameter("q", joined)
                .addQueryParameter("page", page.toString())
                .addQueryParameter("per_page", "100")
                .build()
                .url();

        Triple<HttpStatus, Headers, JsonElement> response = fetch(url);
        return response.getRight().getAsJsonObject();
    }


    public JsonObject fetchRepoInfo(String name) {
        URL url = Endpoint.REPOSITORY.toURL(name.split("/"));
        Triple<HttpStatus, Headers, JsonElement> response = fetch(url);
        return response.getRight().getAsJsonObject();
    }

    public GitCommit fetchLastCommitInfo(String name) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_COMMITS.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "1")
                .build()
                .url();
        Triple<HttpStatus, Headers, JsonElement> response = fetch(url);
        JsonArray commits = response.getRight().getAsJsonArray();
        try {
            JsonObject latest = commits.get(0).getAsJsonObject();
            return conversionService.convert(latest, GitCommit.class);
        } catch (IndexOutOfBoundsException ignored) {
            /*
             * It might be possible for a repository to have no commits.
             * However, such repositories should never appear in the search,
             * because we target repositories written in a specific language!
             * Still, better safe than sorry...
            */
            return GitCommit.NULL_COMMIT;
        }
    }

    public Long fetchNumberOfCommits(String name) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_COMMITS.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "1")
                .build()
                .url();
        return fetchLastPageNumberFromHeader(url);
    }

    public Long fetchNumberOfBranches(String name) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_BRANCHES.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "1")
                .build()
                .url();
        return fetchLastPageNumberFromHeader(url);
    }

    public Long fetchNumberOfReleases(String name) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_RELEASES.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "1")
                .build()
                .url();
        return fetchLastPageNumberFromHeader(url);
    }

    public Long fetchNumberOfContributors(String name) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_CONTRIBUTORS.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "1")
                .build()
                .url();
        return fetchLastPageNumberFromHeader(url);
    }

    public Long fetchNumberOfOpenIssuesAndPulls(String name) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_ISSUES.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "1")
                .addQueryParameter("state", "open")
                .build()
                .url();
        return fetchLastPageNumberFromHeader(url);
    }

    public Long fetchNumberOfAllIssuesAndPulls(String name) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_ISSUES.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "1")
                .addQueryParameter("state", "all")
                .build()
                .url();
        return fetchLastPageNumberFromHeader(url);
    }

    public Long fetchNumberOfOpenPulls(String name) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_PULLS.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "1")
                .addQueryParameter("state", "open")
                .build()
                .url();
        return fetchLastPageNumberFromHeader(url);
    }

    public Long fetchNumberOfAllPulls(String name) {
        
        URL url = HttpUrl.get(Endpoint.REPOSITORY_PULLS.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "1")
                .addQueryParameter("state", "all")
                .build()
                .url();
        return fetchLastPageNumberFromHeader(url);
    }

    public Long fetchNumberOfLabels(String name) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_LABELS.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "1")
                .build()
                .url();
        return fetchLastPageNumberFromHeader(url);
    }

    public Long fetchNumberOfLanguages(String name) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_LANGUAGES.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "1")
                .build()
                .url();
        return fetchLastPageNumberFromHeader(url);
    }

    public Long fetchNumberOfTopics(String name) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_TOPICS.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "1")
                .build()
                .url();
        return fetchLastPageNumberFromHeader(url);
    }

    private Long fetchLastPageNumberFromHeader(URL url) {
        Triple<HttpStatus, Headers, JsonElement> response = fetch(url);
        HttpStatus status = response.getLeft();

        Long count;
        if (status == HttpStatus.FORBIDDEN) {
            /*
             * Response status code 403, two possibilities:
             * (1) The rate limit for the current token is exceeded
             * (2) The request is too expensive for GitHub to compute
             * (e.g. https://api.github.com/repos/torvalds/linux/contributors)
             *
             * Since we make use of guards for the former case,
             * then the latter is always the response cause.
             * As a result we return null value to denote the metric as unobtainable.
             */
            count = null;
        } else {
            JsonElement element = response.getRight();
            Headers headers = response.getMiddle();
            String link = headers.get("link");
            if (link != null) {
                Map<String, String> links = new HashMap<>();
                Matcher matcher = headerLinkPattern.matcher(link);
                while (matcher.find()) {
                    links.put(matcher.group(2), matcher.group(1));
                }
                HttpUrl last = HttpUrl.get(links.get("last"));
                count = Long.parseLong(last.queryParameter("page"));
            } else if (element.isJsonArray()) {
                count = (long) element.getAsJsonArray().size();
            } else if (element.isJsonObject()) {
                count = (long) element.getAsJsonObject().size();
            } else {
                count = 1L;
            }
        }
        return count;
    }

    public JsonArray fetchRepoLabels(String name, Integer page) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_LABELS.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", page.toString())
                .addQueryParameter("per_page", "100")
                .build()
                .url();
        Triple<HttpStatus, Headers, JsonElement> response = fetch(url);
        return response.getRight().getAsJsonArray();
    }

    public JsonObject fetchRepoLanguages(String name, Integer page) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_LANGUAGES.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", page.toString())
                .addQueryParameter("per_page", "100")
                .build()
                .url();
        Triple<HttpStatus, Headers, JsonElement> response = fetch(url);
        return response.getRight().getAsJsonObject();
    }

    public JsonObject fetchRepoTopics(String name, Integer page) {
        URL url = HttpUrl.get(Endpoint.REPOSITORY_TOPICS.toURL(name.split("/")))
                .newBuilder()
                .addQueryParameter("page", page.toString())
                .addQueryParameter("per_page", "100")
                .build()
                .url();
        Triple<HttpStatus, Headers, JsonElement> response = fetch(url);
        return response.getRight().getAsJsonObject();
    }

    Triple<HttpStatus, Headers, JsonElement> fetch(URL url) {
        try {
            Triple<HttpStatus, Headers, JsonElement> result = retryTemplate.execute(new APIFetchCallback(url));
            TimeUnit.MILLISECONDS.sleep(250);
            return result;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GitHubAPIException("API call has been interrupted", ex);
        } catch (Exception ex) {
            String message = String.format("Request to %s failed", url);
            throw new GitHubAPIException(message, ex);
        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private class APIFetchCallback implements RetryCallback<Triple<HttpStatus, Headers, JsonElement>, Exception> {

        URL url;

        @Override
        @SuppressWarnings("resource")
        public Triple<HttpStatus, Headers, JsonElement> doWithRetry(RetryContext context) throws Exception {
            Request.Builder builder = new Request.Builder();
            builder.url(url);
            String currentToken = gitHubTokenManager.getCurrentToken();
            if (currentToken != null)
                builder.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + currentToken);
            Request request = builder.build();
            Response response = client.newCall(request).execute();

            HttpStatus status = HttpStatus.valueOf(response.code());
            HttpStatus.Series series = status.series();
            Headers headers = response.headers();
            String body = response.body().string();
            JsonElement element = conversionService.convert(body, JsonElement.class);

            switch (series) {
                case SUCCESSFUL:
                    return Triple.of(status, headers, element);
                case INFORMATIONAL:
                case REDIRECTION:
                    return Triple.of(status, headers, JsonNull.INSTANCE);
                case CLIENT_ERROR:
                    return handleClientError(status, headers, element.getAsJsonObject());
                case SERVER_ERROR:
                    return handleServerError(status, element.getAsJsonObject());
                default:
            }

            throw new IllegalStateException("This line should never be reached");
        }

        private Triple<HttpStatus, Headers, JsonElement> handleServerError(HttpStatus status, JsonObject json) {
            ErrorResponse errorResponse = conversionService.convert(json, ErrorResponse.class);
            throw new HttpServerErrorException(status, errorResponse.getMessage());
        }

        @SuppressWarnings("java:S128")
        private Triple<HttpStatus, Headers, JsonElement> handleClientError(
                HttpStatus status, Headers headers, JsonObject json
        ) throws InterruptedException {
            ErrorResponse errorResponse = conversionService.convert(json, ErrorResponse.class);
            switch (status) {
                case UNAUTHORIZED:
                    /*
                     * Here we should not call `replaceTokenIfExpired()`
                     * since it would lead to an infinite loop,
                     * because we are checking the Rate Limit API
                     * with the very same unauthorized token.
                     */
                    gitHubTokenManager.replaceToken();
                    break;
                case TOO_MANY_REQUESTS:
                    TimeUnit.MINUTES.sleep(5);
                    break;
                case FORBIDDEN:
                    /*
                     * Response status code 403, two possibilities:
                     * (1) The rate limit for the current token is exceeded
                     * (2) The request is too expensive for GitHub to compute
                     * (e.g. https://api.github.com/repos/torvalds/linux/contributors)
                     */
                    String header = "X-RateLimit-Remaining";
                    int remaining = Optional.ofNullable(headers.get(header))
                            .map(Integer::parseInt)
                            .orElse(-1);
                    if (remaining == -1) {
                        String template = "The '%s' header could not be found, application logic needs an update";
                        String message = String.format(template, header);
                        throw new IllegalStateException(message);
                    } else if (remaining == 0) {
                        gitHubTokenManager.replaceTokenIfExpired();
                        break;
                    } else {
                        /*
                         * Case (2) encountered, so we propagate error upwards
                         * @see fetchLastPageNumberFromHeader
                         */
                        return Triple.of(status, headers, json);
                    }
                default:
                    // TODO: 30.07.23 Add any other special logic here
            }
            throw new HttpClientErrorException(status, errorResponse.getMessage());
        }
    }
}