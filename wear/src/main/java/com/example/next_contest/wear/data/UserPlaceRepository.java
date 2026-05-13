package com.example.next_contest.wear.data;

import com.example.next_contest.wear.model.SavedPlace;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserPlaceRepository {
    private final FirebaseAuth auth;
    private final DatabaseReference db;

    public UserPlaceRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
    }

    public void loadHome(PlaceCallback callback) {
        String uid = auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();

        if (uid == null) {
            callback.onFailure("로그인이 필요합니다.");
            return;
        }

        db.child("users")
                .child(uid)
                .child("homeLocation")
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(toSavedPlace(snapshot)))
                .addOnFailureListener(error ->
                        callback.onFailure(error.getMessage() == null ? "집 위치를 불러오지 못했습니다." : error.getMessage())
                );
    }

    private SavedPlace toSavedPlace(DataSnapshot snapshot) {
        if (!snapshot.exists()) {
            return null;
        }

        Double latitude = numberToDouble(snapshot.child("latitude").getValue());
        Double longitude = numberToDouble(snapshot.child("longitude").getValue());

        if (latitude == null || longitude == null) {
            return null;
        }

        String address = snapshot.child("address").getValue(String.class);
        if (address == null || address.trim().isEmpty()) {
            address = "저장된 집 위치";
        }

        return new SavedPlace(latitude, longitude, address);
    }

    private Double numberToDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    public interface PlaceCallback {
        void onSuccess(SavedPlace place);

        void onFailure(String message);
    }
}
