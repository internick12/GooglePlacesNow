package com.galosoft.googleplacesnow;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPhotoResponse;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    PlacesClient placesClient;
    List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,Place.Field.NAME, Place.Field.ADDRESS);
    AutocompleteSupportFragment places_fragment;
    Button btn_current_palce, btn_get_photo;
    EditText edt_address, edt_place_likelihoods;
    ImageView image_view;
    TextView txt_detail;
    private String placeId ="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermission();

        //init view
        btn_current_palce = findViewById(R.id.btn_get_current_palce);
        edt_address = findViewById(R.id.edt_address);
        edt_place_likelihoods = findViewById(R.id.edt_likehood);
        btn_get_photo = findViewById(R.id.get_photo);
        image_view = findViewById(R.id.image_view);
        txt_detail = findViewById(R.id.txt_detail);

        btn_get_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TextUtils.isEmpty(placeId)) {
                    Toast.makeText(MainActivity.this, "Place id must no be null", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    getPhotoAndDetail(placeId);
                }
            }
        });

        btn_current_palce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentPlace();
            }
        });

        initPlaces();
        setupPlaceAutoComplete();
    }

    private void getPhotoAndDetail(String placeId) {
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, Arrays.asList(Place.Field.PHOTO_METADATAS)).build();
        placesClient.fetchPlace(request).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
            @Override
            public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {

                Place place = fetchPlaceResponse.getPlace();

                //get photo metadata
                PhotoMetadata photoMetadata = place.getPhotoMetadatas().get(0);
                //create fetch photo request
                FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata).build();
                placesClient.fetchPhoto(photoRequest)
                        .addOnSuccessListener(new OnSuccessListener<FetchPhotoResponse>() {
                            @Override
                            public void onSuccess(FetchPhotoResponse fetchPhotoResponse) {
                                Bitmap bitmap = fetchPhotoResponse.getBitmap();
                                image_view.setImageBitmap(bitmap);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, ""+ e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //Get details
        FetchPlaceRequest detailRequest =  FetchPlaceRequest.builder(placeId, Arrays.asList(Place.Field.LAT_LNG)).build();

        placesClient.fetchPlace(detailRequest)
                .addOnCompleteListener(new OnCompleteListener<FetchPlaceResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<FetchPlaceResponse> task) {
                        if(task.isSuccessful()){
                            Place place = task.getResult().getPlace();
                            txt_detail.setText(new StringBuilder(String.valueOf(place.getLatLng().latitude))
                                    .append("/").append(place.getLatLng().longitude));
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestPermission() {
        Dexter.withActivity(this)
                .withPermissions(Arrays.asList(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION))
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        Toast.makeText(MainActivity.this, "You must enable this permission", Toast.LENGTH_SHORT).show();
                    }
                }).check();
    }

    private void getCurrentPlace() {
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeFields).build();

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }

        Task<FindCurrentPlaceResponse> placeResponseTask = placesClient.findCurrentPlace(request);

        placeResponseTask.addOnCompleteListener(new OnCompleteListener<FindCurrentPlaceResponse>() {
            @Override
            public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                if (task.isSuccessful()) {
                    FindCurrentPlaceResponse response = task.getResult();
                    Collections.sort(response.getPlaceLikelihoods(), new Comparator<PlaceLikelihood>() {
                        @Override
                        public int compare(PlaceLikelihood placeLikelihood, PlaceLikelihood t1) {
                            return new Double(placeLikelihood.getLikelihood()).compareTo(t1.getLikelihood());
                        }
                    });

                    //After sort order by ascending
                    Collections.reverse(response.getPlaceLikelihoods());
                    //Set id
                    placeId = response.getPlaceLikelihoods().get(0).getPlace().getId();
                    //Set address for edt_address
                    edt_address.setText(new StringBuilder(response.getPlaceLikelihoods().get(0).getPlace().getAddress()));
                    //loop
                    StringBuilder stringBuilder = new StringBuilder();

                    for(PlaceLikelihood place : response.getPlaceLikelihoods()) {
                        stringBuilder.append(place.getPlace().getName()).append(" -Likelihoods value: ")
                                .append(place.getLikelihood()).append("\n");
                    }
                    edt_place_likelihoods.setText(stringBuilder.toString());
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, ""+ e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupPlaceAutoComplete() {
        places_fragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.places_autocomplet_fragment);
        places_fragment.setPlaceFields(placeFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Toast.makeText(MainActivity.this, "Place get name: " + place.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(MainActivity.this, "" + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initPlaces() {
        Places.initialize(this, getString(R.string.places_api_key));
        placesClient = Places.createClient(this);
    }
}
