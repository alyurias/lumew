package com.example.lumew.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Movie implements Parcelable {
    private String imdbID;
    private String title;
    private String year;
    private String poster;
    private String backdropUrl;
    private String plot;
    private String director;
    private String genre;
    private String imdbRating;
    private String runtime;
    private String actors;
    private String released;
    private String type;

    // Constructors
    public Movie() {
        this.imdbID = "";
        this.title = "Unknown Title";
        this.year = "N/A";
        this.poster = "";
        this.backdropUrl = "";
        this.plot = "No plot available";
        this.director = "Unknown";
        this.genre = "";
        this.imdbRating = "N/A";
        this.runtime = "N/A";
        this.actors = "";
        this.released = "N/A";
        this.type = "movie";
    }

    public Movie(String imdbID, String title, String year, String poster) {
        this();
        this.imdbID = imdbID;
        this.title = title;
        this.year = year;
        this.poster = poster;
    }

    // Getters and Setters
    public String getImdbID() { return imdbID; }
    public void setImdbID(String imdbID) { this.imdbID = imdbID != null ? imdbID : ""; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title != null ? title : "Unknown Title"; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year != null ? year : "N/A"; }

    public String getPoster() { return poster; }
    public void setPoster(String poster) { this.poster = poster != null ? poster : ""; }

    public String getBackdropUrl() { return backdropUrl; }
    public void setBackdropUrl(String backdropUrl) { this.backdropUrl = backdropUrl != null ? backdropUrl : ""; }

    public String getPlot() { return plot; }
    public void setPlot(String plot) { this.plot = plot != null ? plot : "No plot available"; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director != null ? director : "Unknown"; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre != null ? genre : ""; }

    public String getImdbRating() { return imdbRating; }
    public void setImdbRating(String imdbRating) { this.imdbRating = imdbRating != null ? imdbRating : "N/A"; }

    public String getRuntime() { return runtime; }
    public void setRuntime(String runtime) { this.runtime = runtime != null ? runtime : "N/A"; }

    public String getActors() { return actors; }
    public void setActors(String actors) { this.actors = actors != null ? actors : ""; }

    public String getReleased() { return released; }
    public void setReleased(String released) { this.released = released != null ? released : "N/A"; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type != null ? type : "movie"; }

    // Parcelable implementation
    protected Movie(Parcel in) {
        imdbID = in.readString();
        title = in.readString();
        year = in.readString();
        poster = in.readString();
        backdropUrl = in.readString();
        plot = in.readString();
        director = in.readString();
        genre = in.readString();
        imdbRating = in.readString();
        runtime = in.readString();
        actors = in.readString();
        released = in.readString();
        type = in.readString();
    }

    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(imdbID);
        dest.writeString(title);
        dest.writeString(year);
        dest.writeString(poster);
        dest.writeString(backdropUrl);
        dest.writeString(plot);
        dest.writeString(director);
        dest.writeString(genre);
        dest.writeString(imdbRating);
        dest.writeString(runtime);
        dest.writeString(actors);
        dest.writeString(released);
        dest.writeString(type);
    }

    @Override
    public String toString() {
        return "Movie{" +
                "imdbID='" + imdbID + '\'' +
                ", title='" + title + '\'' +
                ", year='" + year + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}