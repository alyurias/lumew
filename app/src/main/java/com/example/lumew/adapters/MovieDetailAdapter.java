package com.example.lumew.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.lumew.R;
import com.example.lumew.models.Movie;

import java.util.Arrays;
import java.util.List;

public class MovieDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SYNOPSIS = 1;
    private static final int TYPE_DETAILS = 2;
    private static final int TYPE_CAST = 3;

    private final Context context;
    private Movie movie;

    public MovieDetailAdapter(Context context, Movie movie) {
        this.context = context;
        this.movie = movie;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER;
        if (position == 1) return TYPE_SYNOPSIS;
        if (position == 2) return TYPE_DETAILS;
        if (position == 3) return TYPE_CAST;
        throw new IllegalArgumentException("Invalid position");
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_HEADER:
                return new HeaderViewHolder(inflater.inflate(
                        R.layout.item_movie_detail_header, parent, false));
            case TYPE_SYNOPSIS:
                return new SynopsisViewHolder(inflater.inflate(
                        R.layout.item_movie_detail_synopsis, parent, false));
            case TYPE_DETAILS:
                return new DetailsViewHolder(inflater.inflate(
                        R.layout.item_movie_detail_info, parent, false));
            case TYPE_CAST:
                return new CastViewHolder(inflater.inflate(
                        R.layout.item_movie_detail_cast, parent, false));
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_HEADER:
                bindHeader((HeaderViewHolder) holder);
                break;
            case TYPE_SYNOPSIS:
                bindSynopsis((SynopsisViewHolder) holder);
                break;
            case TYPE_DETAILS:
                bindDetails((DetailsViewHolder) holder);
                break;
            case TYPE_CAST:
                bindCast((CastViewHolder) holder);
                break;
        }
    }

    private void bindHeader(HeaderViewHolder holder) {
        holder.titleTextView.setText(movie.getTitle());
        holder.yearTextView.setText(movie.getYear());
        holder.genreTextView.setText(movie.getGenre());
        holder.ratingTextView.setText(formatRating(movie.getImdbRating()));

        // Load backdrop with crossfade
        Glide.with(context)
                .load(movie.getBackdropUrl())
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.logo)
                .error(R.drawable.logo2)
                .into(holder.backdropImageView);

        // Load poster with crossfade
        Glide.with(context)
                .load(movie.getPoster())
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.logo)
                .error(R.drawable.logo2)
                .into(holder.posterImageView);
    }

    private String formatRating(String rating) {
        try {
            double value = Double.parseDouble(rating);
            return String.format("⭐ %.1f/10", value);
        } catch (NumberFormatException e) {
            return "Rating: " + rating;
        }
    }

    private void bindSynopsis(SynopsisViewHolder holder) {
        holder.plotTextView.setText(movie.getPlot().isEmpty() ?
                "No plot information available" : movie.getPlot());
    }

    private void bindDetails(DetailsViewHolder holder) {
        holder.directorTextView.setText(formatField("Director", movie.getDirector()));
        holder.runtimeTextView.setText(formatField("Runtime", movie.getRuntime()));
        holder.releasedTextView.setText(formatField("Released", movie.getReleased()));
    }

    private String formatField(String label, String value) {
        return String.format("%s: %s", label, value.isEmpty() ? "N/A" : value);
    }

    private void bindCast(CastViewHolder holder) {
        if (movie.getActors().isEmpty()) {
            holder.castTextView.setText("No cast information available");
            return;
        }

        List<String> castList = Arrays.asList(movie.getActors().split("\\s*,\\s*"));
        StringBuilder castBuilder = new StringBuilder();
        for (String actor : castList) {
            castBuilder.append("• ").append(actor).append("\n");
        }
        holder.castTextView.setText(castBuilder.toString().trim());
    }

    @Override
    public int getItemCount() {
        return movie != null ? 4 : 0;
    }

    public void updateMovie(Movie newMovie) {
        this.movie = newMovie;
        notifyDataSetChanged();
    }

    // ViewHolder classes
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        ImageView backdropImageView, posterImageView;
        TextView titleTextView, yearTextView, genreTextView, ratingTextView;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            backdropImageView = itemView.findViewById(R.id.backdrop_image);
            posterImageView = itemView.findViewById(R.id.detail_poster_image);
            titleTextView = itemView.findViewById(R.id.detail_title_text);
            yearTextView = itemView.findViewById(R.id.detail_year_text);
            genreTextView = itemView.findViewById(R.id.detail_genre_text);
            ratingTextView = itemView.findViewById(R.id.detail_rating_text);
        }
    }

    static class SynopsisViewHolder extends RecyclerView.ViewHolder {
        TextView plotTextView;

        SynopsisViewHolder(@NonNull View itemView) {
            super(itemView);
            plotTextView = itemView.findViewById(R.id.plot_text);
        }
    }

    static class DetailsViewHolder extends RecyclerView.ViewHolder {
        TextView directorTextView, runtimeTextView, releasedTextView;

        DetailsViewHolder(@NonNull View itemView) {
            super(itemView);
            directorTextView = itemView.findViewById(R.id.director_text);
            runtimeTextView = itemView.findViewById(R.id.runtime_text);
            releasedTextView = itemView.findViewById(R.id.released_text);
        }
    }

    static class CastViewHolder extends RecyclerView.ViewHolder {
        TextView castTextView;

        CastViewHolder(@NonNull View itemView) {
            super(itemView);
            castTextView = itemView.findViewById(R.id.cast_text);
        }
    }
}