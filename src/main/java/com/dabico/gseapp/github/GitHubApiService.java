package com.dabico.gseapp.github;

import com.dabico.gseapp.util.interval.DateInterval;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.google.gson.JsonParser.parseString;

public class GitHubApiService {
    private OkHttpClient client;

    public GitHubApiService(){
        this.client = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .build();
    }

    public Response searchRepositories(String language,
                                       DateInterval interval,
                                       Integer page,
                                       String accessToken,
                                       Boolean update) throws IOException, InterruptedException {
        Request request = new Request.Builder()
                .url(Endpoints.SEARCH_REPOS.getUrl() + "?q=language:" + language + (update ? "+updated:" : "+created:")
                     + interval + "+fork:true+is:public&page=" + page + "&per_page=100")
                .addHeader("Authorization", "token " + accessToken)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build();

        Call call = client.newCall(request);
        Response response =  call.execute();
        //TODO Remove guards when done
        Thread.sleep(1000);
        return response;
    }

    public boolean isTokenLimitExceeded(String token) throws IOException {
        Request request = new Request.Builder()
                .url(Endpoints.LIMIT.getUrl())
                .addHeader("Authorization", "token " + token)
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();
        ResponseBody responseBody = response.body();
        if (response.isSuccessful() && responseBody != null){
            JsonObject bodyJson = parseString(responseBody.string()).getAsJsonObject();
            response.close();
            JsonObject search = bodyJson.get("resources").getAsJsonObject().get("search").getAsJsonObject();
            int remaining = search.get("remaining").getAsInt();
            return remaining <= 0;
        } else {
            //TODO Replace with a custom exception
            //or something like, "no connection exception"
            throw new RuntimeException();
        }
    }

    public Response searchRepoLabels(String name, String token) throws IOException, InterruptedException {
        Request request = new Request.Builder()
                .url(generateLabelsURL(name))
                .addHeader("Authorization", "token " + token)
                .build();

        Call call = client.newCall(request);
        Response response =  call.execute();
        //TODO Remove guards when done
        Thread.sleep(1000);
        return response;
    }

    public Response searchRepoLanguages(String name, String token) throws IOException, InterruptedException {
        Request request = new Request.Builder()
                .url(generateLanguagesURL(name))
                .addHeader("Authorization", "token " + token)
                .build();

        Call call = client.newCall(request);
        Response response =  call.execute();
        //TODO Remove guards when done
        Thread.sleep(1000);
        return response;
    }

    private String generateRepoURL(String name){ return Endpoints.REPOS.getUrl() + "/" + name; }

    private String generateLabelsURL(String name){ return generateRepoURL(name) + "/labels"; }

    private String generateLanguagesURL(String name){ return generateRepoURL(name) + "/languages"; }
}
