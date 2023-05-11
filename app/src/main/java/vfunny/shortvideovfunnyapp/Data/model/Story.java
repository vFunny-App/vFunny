package vfunny.shortvideovfunnyapp.Data.model;

import android.util.Log;

import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.FirebaseDatabase;

import vfunny.shortvideovfunnyapp.Login.model.User;


/**
 * Created on 28/04/2017.
 * Copyright by 01eg.me
 */

public class Story {

    private String user;
    private String image;
    private String video;
    private Long created;
    private String message;
    private Integer likes = 0;
    private Integer comments = 0;
    private Long timestamp;

    public Story() {
    }

    public static void uploadVideoStory(String video, String thumbnail) {
        Story newStory = new Story();
        newStory.setUser(User.current().getKey());
        newStory.setImage(thumbnail);
        newStory.setVideo(video);
        newStory.setTimestamp(-1 * System.currentTimeMillis());
        Log.e("TAG", "UPLOADING TO DB");
        Log.e("TAG", "thumbnail: " + thumbnail);
        Log.e("TAG", "video: " + video);
        uploadStory(newStory);
    }

    private static void uploadStory(final Story story) {
        FirebaseDatabase.getInstance().getReference(Const.kDataPostKey).push().setValue(story);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getComments() {
        return comments;
    }

    public void setComments(Integer comments) {
        this.comments = comments;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    public static class StoryReference {
        public String id;

        public static SnapshotParser<StoryReference> parser() {
            return snapshot -> {
                StoryReference reference = new StoryReference();
                reference.id = snapshot.getKey();
                return reference;
            };
        }
    }
}