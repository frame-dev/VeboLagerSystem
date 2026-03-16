package ch.framedev.lagersystem.classes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class SeperateArticle {

    private int index;

    private String articleNumber;
    private String otherDetails;

    public SeperateArticle(int index, String articleNumber, String otherDetails) {
        this.index = index;
        this.articleNumber = articleNumber;
        this.otherDetails = otherDetails;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public String getArticleNumber() {
        return articleNumber;
    }   

    public String getOtherDetails() {
        return otherDetails;
    }

    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

    public static SeperateArticle fromJson(String json) {
        return new Gson().fromJson(json, SeperateArticle.class);
    }

    public static SeperateArticle fromJsonObject(JsonObject jsonObject) {
        return new Gson().fromJson(jsonObject, SeperateArticle.class);
    }
}
