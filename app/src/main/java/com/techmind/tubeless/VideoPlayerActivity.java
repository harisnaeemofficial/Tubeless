package com.techmind.tubeless;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.techmind.tubeless.Sqlite.PostsDatabaseHelper;
import com.techmind.tubeless.adapters.CommentAdapter;
import com.techmind.tubeless.adapters.VideoPostAdapter;
import com.techmind.tubeless.config.AppController;
import com.techmind.tubeless.config.ConstURL;
import com.techmind.tubeless.interfaces.OnItemClickListener;
import com.techmind.tubeless.models.YoutubeCommentModel;
import com.techmind.tubeless.models.YoutubeDataModel;
import com.techmind.tubeless.util.Localization;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import at.huber.youtubeExtractor.YouTubeUriExtractor;
import at.huber.youtubeExtractor.YtFile;

import static com.techmind.tubeless.config.ConstURL.CHANNEL_GET_URL;
import static com.techmind.tubeless.config.ConstURL.GOOGLE_YOUTUBE_API_KEY;
import static com.techmind.tubeless.config.ConstURL.VIDEOS_TYPE;


public class VideoPlayerActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener ,   View.OnClickListener,
        View.OnLongClickListener{
    private static final int READ_STORAGE_PERMISSION_REQUEST_CODE = 1;
    private YoutubeDataModel youtubeDataModel = null;
    TextView videoTitleTextView;
    TextView textViewDes;
    TextView textViewDate;
    private ImageView uploaderThumb;
    JSONObject jsonObjUserDetail = new JSONObject();
    // ImageView imageViewIcon;
    public static final String VIDEO_ID = "c2UNv38V6y4";
    private YouTubePlayerView mYoutubePlayerView = null;
    private YouTubePlayer mYoutubePlayer = null;
    private ArrayList<YoutubeDataModel> mListData = new ArrayList<>();
    private CommentAdapter mAdapter = null;
    private RecyclerView mList_videos = null;
    private String pageToken;
    private VideoPostAdapter adapter = null;
    ArrayList<YoutubeDataModel> mList = new ArrayList<>();
    private ImageButton img_bookmark;
    private boolean bookmarkedId = false;
    private ImageView videoTitleToggleArrow;
    private LinearLayout videoDescriptionRootLayout;
    private View videoTitleRoot;
    private TextView uploaderTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        youtubeDataModel = getIntent().getParcelableExtra(YoutubeDataModel.class.toString());
        mYoutubePlayerView = (YouTubePlayerView) findViewById(R.id.youtube_player);
        mYoutubePlayerView.initialize(GOOGLE_YOUTUBE_API_KEY, this);

        uploaderTextView = findViewById(R.id.detail_uploader_text_view);
        videoTitleRoot = findViewById(R.id.detail_title_root_layout);
        videoTitleTextView = findViewById(R.id.detail_video_title_view);
        videoDescriptionRootLayout = findViewById(R.id.detail_description_root_layout);
        videoTitleToggleArrow = findViewById(R.id.detail_toggle_description_view);
        textViewDes = (TextView) findViewById(R.id.detail_description_view);
        img_bookmark = findViewById(R.id.img_bookmark);
        // imageViewIcon = (ImageView) findViewById(R.id.imageView);
        textViewDate = (TextView) findViewById(R.id.detail_upload_date_view);
        uploaderThumb = findViewById(R.id.detail_uploader_thumbnail_view);

        videoTitleTextView.setText(youtubeDataModel.getTitle());
        textViewDes.setText(youtubeDataModel.getDescription());
//        textViewDate.setText(youtubeDataModel.getPublishedAt());
        if (!TextUtils.isEmpty(youtubeDataModel.getPublishedAt())) {
            textViewDate.setText(Localization.localizeDate(this, youtubeDataModel.getPublishedAt()));
        }
        mList_videos = (RecyclerView) findViewById(R.id.mList_videos);
        mList_videos.setLayoutManager(new LinearLayoutManager(this));
        mList_videos.hasFixedSize();

        videoTitleRoot.setOnClickListener(this);
        img_bookmark.setOnClickListener(this);
        checkBookmarkTag();
        ViewCompat.setNestedScrollingEnabled(mList_videos, false);
        getChannelListFromServer("https://www.googleapis.com/youtube/v3/search?part=snippet&type=video" +
                "&part=contentDetails&relatedToVideoId=" + youtubeDataModel.getVideo_id() + "&maxResults=10&key=" + GOOGLE_YOUTUBE_API_KEY);

        if (!checkPermissionForReadExtertalStorage()) {
            try {
                requestPermissionForReadExtertalStorage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void getVideoStatistics(String url) {
        System.out.println("Get Video statistics*************= " + url);
        //Retrieving response from the server
        JsonObjectRequest js = new JsonObjectRequest(Request.Method.GET, url, jsonObjUserDetail,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        System.out.println("response Channel Api = " + response);
                        mListData = parseTrendingVideoStatistics(response);
                        initList(mListData);

                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplication(), "Server is not reachable!!! " + error, Toast.LENGTH_SHORT).show();
            }
        }) {

            //This is for Headers If You Needed
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "text/plain");
                return params;
            }
        };
        AppController.getInstance().addToRequestQueue(js, null);
    }

    private ArrayList<YoutubeDataModel> parseTrendingVideoStatistics(JSONObject response) {
        return null;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.detail_title_root_layout:
                toggleTitleAndDescription();
                break; case R.id.img_bookmark:
                // Add sample post to the database
                System.out.println("youtubeDataModel.getVideo_id()%%%%% = " + youtubeDataModel.getVideo_id());
                if (!bookmarkedId) {
                    if (PostsDatabaseHelper.getInstance(v.getContext()).addPost(youtubeDataModel, VIDEOS_TYPE)) {
//                    Toast.makeText(getApplicationContext(),"Channel is Bookmarked successfully",Toast.LENGTH_SHORT).show();
                        checkBookmarkTag();
                    } else {
//                        Toast.makeText(getApplicationContext(), "Channel is already Bookmarked", Toast.LENGTH_SHORT).show();
                    }
                } else {

                    if (PostsDatabaseHelper.getInstance(v.getContext()).deleteId(youtubeDataModel.getVideo_id())) {
//                        Toast.makeText(getApplicationContext(), "Channel bookmarked is removed successfully", Toast.LENGTH_SHORT).show();
                        checkBookmarkTag();
                    }
                }
                break;
        }
    }
    private void toggleTitleAndDescription() {
        if (videoTitleToggleArrow != null) {    //it is null for tablets
            if (videoDescriptionRootLayout.getVisibility() == View.VISIBLE) {
                videoTitleTextView.setMaxLines(1);
                videoDescriptionRootLayout.setVisibility(View.GONE);
                videoTitleToggleArrow.setImageResource(R.drawable.arrow_down);
            } else {
                videoTitleTextView.setMaxLines(10);
                videoDescriptionRootLayout.setVisibility(View.VISIBLE);
                videoTitleToggleArrow.setImageResource(R.drawable.arrow_up);
            }
        }
    }
    private void getChannelListFromServer(String url) {
        System.out.println("CHANNLE_GET_URL*************= " + url);
        //Retrieving response from the server
        JsonObjectRequest js = new JsonObjectRequest(Request.Method.GET, url, jsonObjUserDetail,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        System.out.println("response Channel Api = " + response);
                        mListData = parseTrendingVideoListFromResponse(response);
                        initList(mListData);

                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplication(), "Server is not reachable!!! " + error, Toast.LENGTH_SHORT).show();
            }
        }) {

            //This is for Headers If You Needed
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "text/plain");
                return params;
            }
        };
        AppController.getInstance().addToRequestQueue(js, null);

    }

    private void initList(ArrayList<YoutubeDataModel> mListData) {
        adapter = new VideoPostAdapter(getApplicationContext(), mListData, mList_videos, new OnItemClickListener() {
            private Intent intent;

            @Override
            public void onItemClick(YoutubeDataModel item) {
                YoutubeDataModel youtubeDataModel = item;
                System.out.println("youtubeDataModel = " + youtubeDataModel);
                if (youtubeDataModel.getKind().equalsIgnoreCase("youtube#channel")) {
//                    &&requestTypeCallbackStr.equalsIgnoreCase(getString(R.string.channelKey))
                    intent = new Intent(getApplicationContext(), ChannelPlaylistActivity.class);
                    intent.putExtra(YoutubeDataModel.class.toString(), youtubeDataModel);
                    intent.putExtra("activity","SearchActivity");
                    startActivity(intent);
//                    requestTypeCallbackStr.equalsIgnoreCase(getString(R.string.videoKey))
                } else if (youtubeDataModel.getKind().equalsIgnoreCase("youtube#video")) {
                    intent = new Intent(getApplicationContext(), VideoPlayerActivity.class);
                    intent.putExtra(YoutubeDataModel.class.toString(), youtubeDataModel);
                    intent.putExtra("activity","SearchActivity");
                    startActivity(intent);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
                    overridePendingTransition(R.animator.right_in, R.animator.left_out);
                }
            }
        });
        mList_videos.setAdapter(adapter);
//        mList_videos.smoothScrollToPosition(previousListPosition);
    }

    private void checkBookmarkTag() {
        if (PostsDatabaseHelper.getInstance(getApplicationContext()).checkTypeIdExistsOrNot(youtubeDataModel.getVideo_id()) == -1) {
            img_bookmark.setImageResource(R.drawable.ic_bookmarks_outline);
            bookmarkedId = false;
        } else {
            img_bookmark.setImageResource(R.drawable.ic_bookmarks_color);
            bookmarkedId = true;
        }
    }

    public ArrayList<YoutubeDataModel> parseTrendingVideoListFromResponse(JSONObject jsonObject) {


        if (jsonObject.has("items")) {
            try {
                pageToken = jsonObject.getString("nextPageToken");
                JSONArray jsonArray = jsonObject.getJSONArray("items");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    if (json.has("id")) {
                        String video_id = "";
                        JSONObject jsonObj = json.getJSONObject("id");
                        String kind = "";
                        if (jsonObj.has("videoId")) {
                            video_id = jsonObj.getString("videoId");
                            kind = jsonObj.getString("kind");
                        }
                        if (json.has("kind")) {
                            if (json.getString("kind").equals("youtube#video") || json.getString("kind").equals("youtube#searchResult")) {
                                YoutubeDataModel youtubeObject = new YoutubeDataModel();
                                JSONObject jsonSnippet = json.getJSONObject("snippet");
                                String title = jsonSnippet.getString("title");
                                if (json.has("videoId")) {
                                    video_id = json.getString("videoId");
                                }
                                String description = jsonSnippet.getString("description");
                                String publishedAt = jsonSnippet.getString("publishedAt");

                                String thumbnailHigh = jsonSnippet.getJSONObject("thumbnails").getJSONObject("high").getString("url");
                                String thumbnailMedium = jsonSnippet.getJSONObject("thumbnails").getJSONObject("medium").getString("url");
                                String thumbnailDefault = jsonSnippet.getJSONObject("thumbnails").getJSONObject("default").getString("url");
                                youtubeObject.setChannelTitle(jsonSnippet.getString("channelTitle"));
                                youtubeObject.setKind(kind);
                                youtubeObject.setTitle(title);
                                youtubeObject.setVideo_id(video_id);
                                youtubeObject.setDescription(description);
                                youtubeObject.setPublishedAt(publishedAt);
                                youtubeObject.setThumbnailHigh(thumbnailHigh);
                                youtubeObject.setThumbnailMedium(thumbnailMedium);
                                youtubeObject.setThumbnailDefault(thumbnailDefault);
                                mList.add(youtubeObject);

                            }
                        }
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return mList;

    }

    private void nextPageToken(String pageToken) {
        CHANNEL_GET_URL = "https://www.googleapis.com/youtube/v3/search?pageToken=" + pageToken + "&part=snippet&chart=mostPopular&regionCode=IN&" +
                "maxResults=10&key=" + ConstURL.GOOGLE_YOUTUBE_API_KEY + "&part=contentDetails";
        getChannelListFromServer(CHANNEL_GET_URL);

    }

    public void back_btn_pressed(View view) {
        finish();
    }

//    public void playVideo(View view) {
//        if (mYoutubePlayer != null) {
//            if (mYoutubePlayer.isPlaying())
//                mYoutubePlayer.pause();
//            else
//                mYoutubePlayer.play();
//        }
//    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
        youTubePlayer.setPlayerStateChangeListener(playerStateChangeListener);
        youTubePlayer.setPlaybackEventListener(playbackEventListener);
        if (!wasRestored) {
            youTubePlayer.cueVideo(youtubeDataModel.getVideo_id());
        }
        mYoutubePlayer = youTubePlayer;
    }

    private YouTubePlayer.PlaybackEventListener playbackEventListener = new YouTubePlayer.PlaybackEventListener() {
        @Override
        public void onPlaying() {

        }

        @Override
        public void onPaused() {

        }

        @Override
        public void onStopped() {

        }

        @Override
        public void onBuffering(boolean b) {

        }

        @Override
        public void onSeekTo(int i) {

        }
    };

    private YouTubePlayer.PlayerStateChangeListener playerStateChangeListener = new YouTubePlayer.PlayerStateChangeListener() {
        @Override
        public void onLoading() {

        }

        @Override
        public void onLoaded(String s) {

        }

        @Override
        public void onAdStarted() {

        }

        @Override
        public void onVideoStarted() {

        }

        @Override
        public void onVideoEnded() {

        }

        @Override
        public void onError(YouTubePlayer.ErrorReason errorReason) {

        }
    };

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        System.out.println("YouTubePlayer.Provider *****= " + provider);
        System.out.println("youTubeInitializationResult = " + youTubeInitializationResult);
    }

    public void share_btn_pressed(View view) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String link = ("https://www.youtube.com/watch?v=" + youtubeDataModel.getVideo_id());
        // this is the text that will be shared
        sendIntent.putExtra(Intent.EXTRA_TEXT, link);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, youtubeDataModel.getTitle()
                + "Share");

        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "share"));
    }

    public void downloadVideo(View view) {
        //get the download URL
        String youtubeLink = ("https://www.youtube.com/watch?v=" + youtubeDataModel.getVideo_id());
        YouTubeUriExtractor ytEx = new YouTubeUriExtractor(this) {
            @Override
            public void onUrisAvailable(String videoID, String videoTitle, SparseArray<YtFile> ytFiles) {
                if (ytFiles != null) {
                    int itag = 22;
                    //This is the download URL
                    String downloadURL = ytFiles.get(itag).getUrl();
                    Log.e("download URL :", downloadURL);

                    //now download it like a file
                    new RequestDownloadVideoStream().execute(downloadURL, videoTitle);


                }

            }
        };

        ytEx.execute(youtubeLink);
    }

    private ProgressDialog pDialog;

    @Override
    public boolean onLongClick(View view) {
        return false;
    }


    private class RequestDownloadVideoStream extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(VideoPlayerActivity.this);
            pDialog.setMessage("Downloading file. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setMax(100);
            pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            InputStream is = null;
            URL u = null;
            int len1 = 0;
            int temp_progress = 0;
            int progress = 0;
            try {
                u = new URL(params[0]);
                is = u.openStream();
                URLConnection huc = (URLConnection) u.openConnection();
                huc.connect();
                int size = huc.getContentLength();

                if (huc != null) {
                    String file_name = params[1] + ".mp4";
                    String storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/YoutubeVideos";
                    File f = new File(storagePath);
                    if (!f.exists()) {
                        f.mkdir();
                    }

                    FileOutputStream fos = new FileOutputStream(f + "/" + file_name);
                    byte[] buffer = new byte[1024];
                    int total = 0;
                    if (is != null) {
                        while ((len1 = is.read(buffer)) != -1) {
                            total += len1;
                            // publishing the progress....
                            // After this onProgressUpdate will be called
                            progress = (int) ((total * 100) / size);
                            if (progress >= 0) {
                                temp_progress = progress;
                                publishProgress("" + progress);
                            } else
                                publishProgress("" + temp_progress + 1);

                            fos.write(buffer, 0, len1);
                        }
                    }

                    if (fos != null) {
                        publishProgress("" + 100);
                        fos.close();
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            pDialog.setProgress(Integer.parseInt(values[0]));
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (pDialog.isShowing())
                pDialog.dismiss();
        }
    }


    public void initVideoList(ArrayList<YoutubeCommentModel> mListData) {
        mList_videos.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CommentAdapter(this, mListData);
        mList_videos.setAdapter(mAdapter);
    }

    public ArrayList<YoutubeCommentModel> parseJson(JSONObject jsonObject) {
        ArrayList<YoutubeCommentModel> mList = new ArrayList<>();

        if (jsonObject.has("items")) {
            try {
                JSONArray jsonArray = jsonObject.getJSONArray("items");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);

                    YoutubeCommentModel youtubeObject = new YoutubeCommentModel();
                    JSONObject jsonTopLevelComment = json.getJSONObject("snippet").getJSONObject("topLevelComment");
                    JSONObject jsonSnippet = jsonTopLevelComment.getJSONObject("snippet");

                    String title = jsonSnippet.getString("authorDisplayName");
                    String thumbnail = jsonSnippet.getString("authorProfileImageUrl");
                    String comment = jsonSnippet.getString("textDisplay");

                    youtubeObject.setTitle(title);
                    youtubeObject.setComment(comment);
                    youtubeObject.setThumbnailHigh(thumbnail);
                    mList.add(youtubeObject);


                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return mList;

    }

    public void requestPermissionForReadExtertalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    READ_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public boolean checkPermissionForReadExtertalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            int result2 = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            return (result == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED);
        }
        return false;
    }

    private void requestYoutubeCommentAPI() {
        String VIDEO_COMMENT_URL = "https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&maxResults=100&videoId=" + youtubeDataModel.getVideo_id() + "&key=" + GOOGLE_YOUTUBE_API_KEY;

        JSONObject jsonObjUserDetail = new JSONObject();
        System.out.println("VIDEO_COMMENT_URL*************= " + VIDEO_COMMENT_URL);
        //Retrieving response from the server
        JsonObjectRequest js = new JsonObjectRequest(Request.Method.GET, VIDEO_COMMENT_URL, jsonObjUserDetail,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
//                        mListData = parseJson(response);
//                        initVideoList(mListData);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(getApplicationContext(), "Server is not reachable!!! " + error, Toast.LENGTH_SHORT).show();
            }
        }) {

            //This is for Headers If You Needed
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "text/plain");
                return params;
            }
        };
        AppController.getInstance().addToRequestQueue(js, null);

    }
    @Override
    protected void onResume() {
        super.onResume();

    }
    @Override
    public void onBackPressed() {
       finish();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            this.overridePendingTransition(R.animator.left_to_right, R.animator.right_to_left);
        }
        super.onBackPressed();
    }
}
