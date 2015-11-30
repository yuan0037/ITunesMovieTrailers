package com.algonquincollege.mad9132.itunesmovietrailers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.ContentHandler;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.algonquincollege.mad9132.itunesmovietrailers.domain.MovieItem;

import org.w3c.dom.Text;

import static com.algonquincollege.mad9132.itunesmovietrailers.Constants.LOG_TAG;

/**
 * Created by boyuan on 15-11-29.
 */
public class MovieListArrayAdapter extends ArrayAdapter<MovieItem> {

    private static final SimpleDateFormat DATE_FORMAT;
    private ImageView posterView;

    static {
        DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy");
    }

    
    // use viewHolder to load list faster, refer to
    //https://dl.google.com/io/2009/pres/Th_0230_TurboChargeYourUI-HowtomakeyourAndroidUIfastandefficient.pdf
    static class ViewHolder {
        TextView title;
        TextView pubDate;
        ImageView poster;
        ProgressBar progressBarIndicator;
    }

    //used a caching mechanism from google to cache downloaded images
    //refer to http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html
    //if the list is very long (can easily fill up a memory cache), use disk cache
    //refer to http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html Disk Cache section;
    private LruCache<String, Bitmap> mMemoryCache;

    public MovieListArrayAdapter(Context context, ArrayList<MovieItem> movieList) {
        super(context, 0, movieList);

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        final MovieItem movie = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.movie_item_view, parent, false);

            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.pubDate = (TextView) convertView.findViewById(R.id.pub_date);
            holder.poster = (ImageView) convertView.findViewById(R.id.poster);
            holder.progressBarIndicator = (ProgressBar) convertView.findViewById(R.id.loadingProgressBar);
            convertView.setTag(holder);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent playTrailerIntent = new Intent(getContext(), LinkActivity.class);
                    playTrailerIntent.putExtra(LinkFragment.ARG_LINK, movie.getTheRealLink());
                    getContext().startActivity(playTrailerIntent);
                }
            });
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.title.setText(movie.getTitle());
        holder.pubDate.setText(DATE_FORMAT.format(movie.getPubDate()));
        if (getBitmapFromMemCache(movie.getPosterLink()) != null) {
            //Log.e(LOG_TAG, "found in cache");
            holder.progressBarIndicator.setVisibility(View.GONE);
            holder.poster.setImageBitmap(getBitmapFromMemCache(movie.getPosterLink()));
        } else {
            //Log.e(LOG_TAG, "not found in cache");
            holder.progressBarIndicator.setVisibility(View.VISIBLE);
            new GetPosterTask().execute(movie.getPosterLink());
        }

        return convertView;
    }

    private class GetPosterTask extends AsyncTask<String, Void, Bitmap> {
        private Drawable posterDrawable;

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bmIcon;
            try {
                URL url = new URL(params[0]);
                Log.e(LOG_TAG, "url: " + url);
                bmIcon = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                addBitmapToMemoryCache(params[0], bmIcon);
                //posterDrawable = Drawable.createFromStream(url.openStream(), "src");
            } catch (Exception e) {
                return null;
            }
            return bmIcon;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            //Log.e(LOG_TAG, "post execute: ");
            notifyDataSetChanged();
        }
    }
}
