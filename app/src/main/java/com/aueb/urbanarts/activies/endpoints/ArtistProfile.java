package com.aueb.urbanarts.activies.endpoints;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aueb.urbanarts.R;
import com.aueb.urbanarts.activies.search.Feed;
import com.aueb.urbanarts.activies.HomePage;
import com.aueb.urbanarts.activies.accountmanagement.EditAccount;
import com.aueb.urbanarts.activies.report.ReportUser;
import com.blogspot.atifsoftwares.animatoolib.Animatoo;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.synnapps.carouselview.CarouselView;
import com.synnapps.carouselview.ImageListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ArtistProfile extends AppCompatActivity {
    final String TAG = "123";
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CarouselView carouselView;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseUser user = mAuth.getCurrentUser();
    String artistName;
    String artistType;
    String artistGenre;
    String artistYear;
    String followersNum;
    String artistDescription;
    String artistImage;
    String artist_id;
    String text;
    List<String> artistGallery = new ArrayList<>();
    Map<String, Boolean> followedUsersMap = new HashMap<>();
    TextView followersNumDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_profile);

        ProgressBar imageProg = findViewById(R.id.progress_bar);
        imageProg.setVisibility(View.VISIBLE);

        Intent intent = getIntent();
        artist_id = intent.getStringExtra("ARTIST_DOCUMENT_ID");
        whoIsIt(artist_id);
        getArtistInformation(artist_id);

        carouselView = findViewById(R.id.gallery);
        carouselView.setPageCount(artistGallery.size());
        carouselView.setImageListener(imageListener);

        findViewById(R.id.action).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (artist_id.equals(user.getUid())) {
                    goEditAccount();
                } else {
                    goReportUser(artist_id);
                }
            }
        });

        findViewById(R.id.home_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goHomePage();
            }
        });

        findViewById(R.id.go_feed).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goFeed(artist_id, artistName, artistImage);
            }
        });

        TextView appName = findViewById(R.id.appName);
        appName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ArtistProfile.this, HomePage.class);
                startActivity(intent);
                Animatoo.animateZoom(ArtistProfile.this);
                finish();
            }
        });

        final Button follow = findViewById(R.id.follow_button);

        if (user != null) {
            DocumentReference docRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        final DocumentSnapshot document = task.getResult();
                        followedUsersMap = (Map<String, Boolean>) document.get("followedUsers");
                        try {
                            if (followedUsersMap.get(artist_id)) {
                                text = "Unfollow";
                                follow.setText(text);
                            } else {
                                text = "Follow";
                                follow.setText(text);
                            }
                        } catch (Exception ignore) {
                        }
                    }
                }
            });
        } else {
            follow.setClickable(false);
        }

        follow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (user == null) {
                    Toast.makeText(ArtistProfile.this, "You have to create an account in order to follow an artist!", Toast.LENGTH_SHORT).show();
                } else {
                    final DocumentReference docRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
                    docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                final DocumentSnapshot document = task.getResult();
                                followedUsersMap = (Map<String, Boolean>) document.get("followedUsers");
                                final DocumentReference docRef2 = db.collection("artists").document(artist_id);
                                docRef2.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            final DocumentSnapshot documentSnapshot = task.getResult();
                                            if (followedUsersMap.containsKey(artist_id)) {
                                                if (followedUsersMap.get(artist_id)) {
                                                    showFollowers(followersNumDisplay, String.valueOf(Integer.parseInt(documentSnapshot.getString("followers")) - 1));
                                                    docRef2.update("followers", String.valueOf(Integer.parseInt(documentSnapshot.getString("followers")) - 1));
                                                    followedUsersMap.put(artist_id, false);
                                                    docRef.update("followedUsers", followedUsersMap);
                                                    text = "Follow";
                                                    follow.setText(text);
                                                } else {
                                                    showFollowers(followersNumDisplay, String.valueOf(Integer.parseInt(documentSnapshot.getString("followers")) + 1));
                                                    docRef2.update("followers", String.valueOf(Integer.parseInt(documentSnapshot.getString("followers")) + 1));
                                                    followedUsersMap.put(artist_id, true);
                                                    docRef.update("followedUsers", followedUsersMap);
                                                    text = "Unfollow";
                                                    follow.setText(text);
                                                }
                                            } else {
                                                followedUsersMap.put(artist_id, true);
                                                showFollowers(followersNumDisplay, String.valueOf(Integer.parseInt(documentSnapshot.getString("followers")) + 1));
                                                docRef2.update("followers", String.valueOf(Integer.parseInt(documentSnapshot.getString("followers")) + 1));
                                                followedUsersMap.put(artist_id, true);
                                                docRef.update("followedUsers", followedUsersMap);
                                                text = "Unfollow";
                                                follow.setText(text);
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void whoIsIt(String artist_id) {
        if (user != null) {
            ImageButton action = findViewById(R.id.action);
            if (artist_id.equals(user.getUid())) {
                action.setImageResource(R.drawable.edit);
            } else {
                action.setImageResource(R.drawable.report);
            }
        }
    }

    private void getArtistInformation(final String artist_id) {
        final CircleImageView profileImage = findViewById(R.id.artist_profile_photo);
        final TextView artistNameDisplay = findViewById(R.id.artist_name);
        final TextView genreDisplay = findViewById(R.id.artist_genre);
        final TextView yearDisplay = findViewById(R.id.artist_age);
        final TextView descriptionDisplay = findViewById(R.id.artist_description);
        followersNumDisplay = findViewById(R.id.num_follows);
        final ProgressBar loadGallery = findViewById(R.id.load_carousel);
        final CarouselView gallery = findViewById(R.id.gallery);

        DocumentReference docArtist = db.collection("artists").document(artist_id);
        docArtist.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        getArtistEvents(artist_id);
                        artistName = document.getString("display_name");
                        artistType = document.getString("artist_type");
                        artistGenre = document.getString("genre");
                        artistYear = document.getString("year");
                        artistDescription = document.getString("description");
                        artistName = document.getString("display_name");
                        artistImage = document.getString("profile_image_url");
                        followersNum = document.getString("followers");
                        if (!document.get("gallery").equals(""))
                            artistGallery = (List<String>) document.get("gallery");

                        showProfileImage(profileImage, artistImage);
                        showArtistName(artistNameDisplay, artistName);
                        showGenre(genreDisplay, artistGenre);
                        showYear(yearDisplay, artistYear);
                        showDescription(descriptionDisplay, artistDescription);
                        showFollowers(followersNumDisplay, followersNum);

                        TextView noGallery = findViewById(R.id.error_gallery);
                        if (!artistGallery.isEmpty()) {
                            noGallery.setText("");
                            showGallery(artistGallery);

                            final float scale = getResources().getDisplayMetrics().density;
                            int pixels = (int) (300 * scale);
                            gallery.requestLayout();
                            gallery.getLayoutParams().height = pixels;

                        } else {
                            noGallery.setText("No Images :(");
                            loadGallery.setVisibility(View.INVISIBLE);
                            loadGallery.setVisibility(View.GONE);
                        }
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void getArtistEvents(final String artist_id) {
        final ImageView goFeedDisplay = findViewById(R.id.go_feed);
        final TextView eventsNumDisplay = findViewById(R.id.events_num);
        final int[] counter = {0};
        db.collection("events")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.getString("ArtistID").equals(artist_id)) {
                                    counter[0]++;
                                }
                            }

                            if (counter[0] == 0) {
                                eventsNumDisplay.setText("No events.");
                            } else if (counter[0] == 1) {
                                goFeedDisplay.setVisibility(View.VISIBLE);
                                eventsNumDisplay.setText("1 event.");
                            } else {
                                goFeedDisplay.setVisibility(View.VISIBLE);
                                eventsNumDisplay.setText(counter[0] + " events.");
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private void showFollowers(final TextView followersNumDisplay, final String followers) {
        followersNumDisplay.setText(followers);
    }

    private void showDescription(TextView descriptionDisplay, final String description) {
        descriptionDisplay.setText(description);
    }

    private void showYear(final TextView yearDisplay, final String year) {
        String whatYear = String.valueOf(year);
        String type = String.valueOf(artistType);
        String whatToSay = "";

        if (type.equals("individual")) {
            int findAge = Calendar.getInstance().get(Calendar.YEAR) - Integer.parseInt(whatYear);
            whatToSay = findAge + " years old.";
        } else {
            int findAge = Calendar.getInstance().get(Calendar.YEAR) - Integer.parseInt(whatYear);
            if (findAge == 0) {
                whatToSay = "almost 1 year together.";
            } else if (findAge > 1) {
                whatToSay = findAge + " years together.";
            } else if (findAge == 1) {
                whatToSay = "1 year together.";
            }
        }
        yearDisplay.setText(whatToSay);
    }

    private void showGenre(TextView genreDisplay, final String genre) {
        genreDisplay.setText(genre);
    }

    private void showArtistName(TextView nameDisplay, final String name) {
        nameDisplay.setText(name);
    }

    private void showProfileImage(final CircleImageView profileImage, final String imageURL) {
        final ProgressBar imageProg = findViewById(R.id.progress_bar);
        if (imageURL.equals("none")) {
            profileImage.setImageResource(R.drawable.profile);
            imageProg.setVisibility(View.INVISIBLE);
        } else {
            Glide.with(getApplicationContext())
                    .load(imageURL)
                    .listener(new RequestListener() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
                            imageProg.setVisibility(View.INVISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Object resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
                            imageProg.setVisibility(View.INVISIBLE);
                            return false;
                        }
                    })
                    .into(profileImage);
        }
    }

    private void showGallery(List<String> artistGallery) {
        carouselView.setPageCount(artistGallery.size());
        carouselView.setImageListener(imageListener);
    }

    ImageListener imageListener = new ImageListener() {
        @Override
        public void setImageForPosition(int position, ImageView imageView) {
            final ProgressBar loadGallery = findViewById(R.id.load_carousel);
            Glide.with(getApplicationContext())
                    .load(artistGallery.get(position))
                    .listener(new RequestListener() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
                            loadGallery.setVisibility(View.INVISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Object resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
                            loadGallery.setVisibility(View.INVISIBLE);
                            return false;
                        }
                    })
                    .into(imageView);
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Animatoo.animateFade(this);
        finish();
    }

    public void goHomePage() {
        startActivity(new Intent(this, HomePage.class));
        Animatoo.animateZoom(this);
        finish();
    }

    private void goEditAccount() {
        startActivity(new Intent(this, EditAccount.class));
        Animatoo.animateFade(this);
        finish();
    }

    private void goFeed(String artist_id, String artist_name, String artist_image) {
        Intent intent = new Intent(ArtistProfile.this, Feed.class);
        intent.putExtra("artist_id", artist_id);
        intent.putExtra("artist_name", artist_name);
        intent.putExtra("artist_image", artist_image);
        intent.putExtra("FROM_ARTIST", "FROM_ARTIST");
        startActivity(intent);
        Animatoo.animateFade(this);
    }

    private void goReportUser(String artist_id) {
        Intent intent = new Intent(ArtistProfile.this, ReportUser.class);
        intent.putExtra("artist_id", artist_id);
        startActivity(intent);
        Animatoo.animateFade(this);
    }
}