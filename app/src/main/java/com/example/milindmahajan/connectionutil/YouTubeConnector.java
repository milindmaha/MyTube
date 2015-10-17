package com.example.milindmahajan.connectionutil;

import com.example.milindmahajan.application_settings.ApplicationSettings;
import com.example.milindmahajan.dateutils.DateUtil;
import com.example.milindmahajan.model.Auth;
import com.example.milindmahajan.model.File;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.client.util.Joiner;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Created by milind.mahajan on 10/10/15.
 */
public class YouTubeConnector {

    private static final long NUMBER_OF_VIDEOS_RETURNED = 25;

    private static YouTube getYoutubeBuilder () {

        return new YouTube.Builder(new NetHttpTransport(),
                new JacksonFactory(), new HttpRequestInitializer() {

            @Override
            public void initialize(HttpRequest hr) throws IOException {

            }
        }).setApplicationName("CmpE277_Lab2").build();
    }

    public static ArrayList<File> searchVideoWithKeywords(String keywords) {

        YouTube youtube = getYoutubeBuilder();

        try {

            System.out.println("Accesstoken is "+ApplicationSettings.getSharedSettings().getAccessToken());
            YouTube.Search.List searchItems = youtube.search().list("id,snippet")
                    .setMaxResults(NUMBER_OF_VIDEOS_RETURNED)
                    .setFields("items(id/videoId)")
                    .setKey(Auth.APIKEY)
                    .setType("video")
                    .setQ(keywords);
            SearchListResponse searchListResponse = searchItems.execute();
            List<SearchResult> searchList = searchListResponse.getItems();
            List<PlaylistItem> playlistItemList = getVideosInFavorites(youtube);

            ArrayList<File> items = new ArrayList<File>();

            for(SearchResult result:searchList) {

                File item = new File();

                try {

                    List<String> videoIds = new ArrayList<String>();
                    videoIds.add(result.getId().getVideoId());
                    Joiner stringJoiner = Joiner.on(',');
                    String videoId = stringJoiner.join(videoIds);

                    YouTube.Videos.List listVideosRequest = youtube.videos().list("snippet, statistics").setId(videoId);
                    listVideosRequest.setKey(Auth.APIKEY);
                    VideoListResponse listResponse = listVideosRequest.execute();

                    item.setTitle(listResponse.getItems().get(0).getSnippet().getTitle());
                    item.setPublishedDate(DateUtil.convertDate(listResponse.getItems().get(0).getSnippet().getPublishedAt().toString()));
                    item.setThumbnailURL(listResponse.getItems().get(0).getSnippet().getThumbnails().getDefault().getUrl());
                    item.setId(result.getId().getVideoId());
                    item.setFavorite(belongsToFavorites(playlistItemList, result.getId().getVideoId()));
                    item.setNumberOfViews(listResponse.getItems().get(0).getStatistics().getViewCount().toString());
                } catch (Exception exc) {

                    item.setTitle("Exception case");
                    item.setPublishedDate("Exception date");
                    item.setNumberOfViews("Exception count");
                }

                items.add(item);
            }

            return items;
        } catch(IOException e){

            e.printStackTrace();
            return null;
        }
    }

    public static ArrayList <File> getFavorites() {

        YouTube youtube = getYoutubeBuilder();

        try {

            List<PlaylistItem> playlistItemList = getVideosInFavorites(youtube);

            ArrayList<File> items = new ArrayList<File>();

            for(PlaylistItem result:playlistItemList) {

                File item = new File();

                try {

                    List<String> videoIds = new ArrayList<String>();
                    videoIds.add(result.getId());
                    Joiner stringJoiner = Joiner.on(',');
                    String videoId = stringJoiner.join(videoIds);

                    YouTube.Videos.List listVideosRequest = youtube.videos().list("snippet, statistics").setId(videoId);
                    listVideosRequest.setKey(Auth.APIKEY);
                    VideoListResponse listResponse = listVideosRequest.execute();

                    try {

                        item.setTitle(result.getSnippet().getTitle());
                    }
                    catch (Exception exc) {

                        item.setTitle("Exception case");
                    }
                    try {

                        item.setPublishedDate(DateUtil.convertDate(result.getSnippet().getPublishedAt().toString()));
                    }
                    catch (Exception exc) {

                        item.setPublishedDate("Exception date");
                    }
                    try {

                        item.setNumberOfViews(listResponse.getItems().get(0).getStatistics().getViewCount().toString());
                    } catch (Exception exc) {

                        item.setNumberOfViews("Exception count");
                    }

                    item.setThumbnailURL(result.getSnippet().getThumbnails().getDefault().getUrl());
                    item.setId(result.getId());
                } catch (Exception exc) {

                }

                items.add(item);
            }

            return items;

        } catch (Exception exc) {

            exc.printStackTrace();
        }
        return null;
    }

    private static List <PlaylistItem> getVideosInFavorites (YouTube youtube) {

        try {

            YouTube.PlaylistItems.List playlistItems = youtube.playlistItems()
                    .list("snippet")
                    .setMaxResults(NUMBER_OF_VIDEOS_RETURNED)
                    .setKey(Auth.APIKEY)
                    .setPlaylistId(ApplicationSettings.getSharedSettings().getFavoritePlaylistId());

            PlaylistItemListResponse playlistListResponse = playlistItems.execute();
            List<PlaylistItem> playlistItemList = playlistListResponse.getItems();

            return playlistItemList;
        } catch (IOException exc) {

            return null;
        }
    }

    public static boolean removeFromFavorites(String videoId) {

        YouTube youtube = getYoutubeBuilder();

        try {

            YouTube.PlaylistItems.Delete delete = youtube.playlistItems().delete(videoId);
            delete.execute();

            return true;
        } catch (IOException exc) {

            exc.printStackTrace();
            return false;
        }
    }

    public static boolean insertIntoFavorites(File file) {

        YouTube youtube = getYoutubeBuilder();

        try {

            ResourceId resourceId = new ResourceId();
            resourceId.setKind("youtube#video");
            resourceId.setVideoId(file.getId());

            PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
            playlistItemSnippet.setTitle(file.getTitle());
            playlistItemSnippet.setPlaylistId(ApplicationSettings.getSharedSettings().getFavoritePlaylistId());
            playlistItemSnippet.setResourceId(resourceId);

            PlaylistItem playlistItem = new PlaylistItem();
            playlistItem.setSnippet(playlistItemSnippet);

            YouTube.PlaylistItems.Insert insert = youtube.playlistItems()
                    .insert("snippet,contentDetails", playlistItem);
            PlaylistItem returnedPlaylistItem = insert.execute();

            return (returnedPlaylistItem == null);

        } catch (IOException exc) {

            exc.printStackTrace();
            return false;
        }
    }

    private static boolean belongsToFavorites (List<PlaylistItem> playlistItemList, String videoId) {

        boolean isPresent = false;
        for(PlaylistItem result:playlistItemList) {

            System.out.println("Searching for ID "+videoId);
            System.out.println("List ITEM ID "+result.getSnippet().getResourceId().getVideoId());
            if (result.getSnippet().getResourceId().getVideoId().equals(videoId)) {

                isPresent = true;
                break;
            }
        }

        return isPresent;
    }
}