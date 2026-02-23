package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

        private Button addCityButton;
        private ListView cityListView;

        private ArrayList<City> cityArrayList;
        private ArrayAdapter<City> cityArrayAdapter;

        //creating a firestore instance
        private FirebaseFirestore db;

        private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        Button deleteCityButton = findViewById(R.id.buttonDeleteCity);
        cityListView = findViewById(R.id.listviewCities);

        // Create city array — MUST be before snapshot listener
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // NOW set up the snapshot listener
        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }
            if (value != null) {
                cityArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");
                    cityArrayList.add(new City(name, province));
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });

        // Add city button listener
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(), "Add City");
        });

        // Delete city button listener
        deleteCityButton.setOnClickListener(view -> {
            int selectedPosition = cityListView.getCheckedItemPosition();
            if (selectedPosition != ListView.INVALID_POSITION) {
                City city = cityArrayAdapter.getItem(selectedPosition);
                if (city != null) {
                    // Delete from Firestore
                    citiesRef.document(city.getName()).delete()
                            .addOnSuccessListener(aVoid -> Log.d("Firestore", "City deleted"))
                            .addOnFailureListener(e -> Log.e("Firestore", "Error deleting", e));
                    // Local list will auto-update via snapshot listener
                }
            }
        });

        // Item click listener for editing
        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(), "City Details");
        });
    }

    @Override
    public void updateCity(City city, String title, String province) {
        // Only update if something actually changed
        if (city.getName().equals(title) && city.getProvince().equals(province)) {
            return; // Nothing changed, do nothing
        }

        citiesRef.document(city.getName()).delete();
        city.setName(title);
        city.setProvince(province);
        citiesRef.document(title).set(city);
    }

    @Override
    public void addCity(City city) {
        Log.d("Firestore", "Attempting to add city: " + city.getName());
        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(city)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "City successfully added!"))
                .addOnFailureListener(e -> Log.e("Firestore", "FAILED to add city: " + e.getMessage()));
    }
}