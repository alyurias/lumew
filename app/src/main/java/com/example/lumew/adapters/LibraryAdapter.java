package com.example.lumew.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.lumew.R;
import com.example.lumew.activities.MovieDetailActivity;
import com.example.lumew.models.Movie;
import com.example.lumew.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder> {

    private final Context context;
    private List<Movie> libraryMovies;
    private List<Movie> filteredMovies;
    private OnLibraryItemListener libraryItemListener;
    private boolean isFiltering = false;
    private String currentUserId;

    public interface OnLibraryItemListener {
        void onMovieRemoved(Movie movie, int position);
        void onMovieClicked(Movie movie);
        void onRemoveError(Exception e);
    }

    public LibraryAdapter(Context context, List<Movie> movies, String userId) {
        this.context = context;
        this.libraryMovies = new ArrayList<>(movies);
        this.filteredMovies = new ArrayList<>(movies);
        this.currentUserId = userId;
    }

    public void setOnLibraryItemListener(OnLibraryItemListener listener) {
        this.libraryItemListener = listener;
    }

    @NonNull
    @Override
    public LibraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_library_movie, parent, false);
        return new LibraryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryViewHolder holder, int position) {
        Movie movie = getMovieAt(position);

        holder.titleTextView.setText(movie.getTitle());
        holder.yearTextView.setText(movie.getYear());

        // Load movie poster with rounded corners
        Glide.with(context)
                .load(movie.getPoster())
                .apply(new RequestOptions()
                        .placeholder(R.drawable.logo2)
                        .error(R.drawable.logo2)
                        .transform(new RoundedCorners(16)))
                .into(holder.posterImageView);

        // Set click listeners
        holder.cardView.setOnClickListener(v -> {
            if (libraryItemListener != null) {
                libraryItemListener.onMovieClicked(movie);
            } else {
                openMovieDetails(movie);
            }
        });

        holder.removeButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                handleMovieRemove(movie, adapterPosition);
            }
        });
    }

    private Movie getMovieAt(int position) {
        return isFiltering ? filteredMovies.get(position) : libraryMovies.get(position);
    }

    private void handleMovieRemove(Movie movie, int position) {
        if (currentUserId == null) {
            Toast.makeText(context, "Authentication required", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUtil.getInstance().removeMovieFromLibrary(currentUserId, movie.getImdbID(),
                new FirebaseUtil.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        if (libraryItemListener != null) {
                            int actualPosition = isFiltering ? libraryMovies.indexOf(movie) : position;
                            libraryItemListener.onMovieRemoved(movie, actualPosition);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (libraryItemListener != null) {
                            libraryItemListener.onRemoveError(e);
                        } else {
                            Toast.makeText(context, "Failed to remove movie", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void openMovieDetails(Movie movie) {
        Intent intent = new Intent(context, MovieDetailActivity.class);
        intent.putExtra("movie_id", movie.getImdbID());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return isFiltering ? filteredMovies.size() : libraryMovies.size();
    }

    public void updateMovies(List<Movie> newMovies) {
        this.libraryMovies = new ArrayList<>(newMovies);
        this.filteredMovies = new ArrayList<>(newMovies);
        notifyDataSetChanged();
    }

    public void addMovie(Movie movie) {
        if (containsMovie(movie.getImdbID())) {
            return;
        }

        libraryMovies.add(0, movie);
        filteredMovies.add(0, movie);
        notifyItemInserted(0);
    }

    public void removeMovie(int position) {
        if (position >= 0 && position < libraryMovies.size()) {
            Movie removed = libraryMovies.remove(position);
            filteredMovies.remove(removed);
            notifyItemRemoved(position);
        }
    }

    public boolean containsMovie(String movieId) {
        for (Movie movie : libraryMovies) {
            if (movie.getImdbID().equals(movieId)) {
                return true;
            }
        }
        return false;
    }

    public void filterMovies(String query) {
        filteredMovies.clear();

        if (query == null || query.isEmpty()) {
            isFiltering = false;
            filteredMovies.addAll(libraryMovies);
        } else {
            isFiltering = true;
            String lowerCaseQuery = query.toLowerCase();
            for (Movie movie : libraryMovies) {
                // Safe null checks for all fields
                boolean matchesTitle = movie.getTitle() != null &&
                        movie.getTitle().toLowerCase().contains(lowerCaseQuery);

                boolean matchesYear = movie.getYear() != null &&
                        movie.getYear().toLowerCase().contains(lowerCaseQuery);

                boolean matchesGenre = movie.getGenre() != null &&
                        movie.getGenre().toLowerCase().contains(lowerCaseQuery);

                if (matchesTitle || matchesYear || matchesGenre) {
                    filteredMovies.add(movie);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void clearFilter() {
        isFiltering = false;
        notifyDataSetChanged();
    }

    public List<Movie> getCurrentDisplayedMovies() {
        return isFiltering ? new ArrayList<>(filteredMovies) : new ArrayList<>(libraryMovies);
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    static class LibraryViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView posterImageView;
        TextView titleTextView;
        TextView yearTextView;
        ImageView removeButton;

        public LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.library_card_view);
            posterImageView = itemView.findViewById(R.id.library_poster_image);
            titleTextView = itemView.findViewById(R.id.library_title_text);
            yearTextView = itemView.findViewById(R.id.library_year_text);
            removeButton = itemView.findViewById(R.id.library_remove_button);
        }
    }
}