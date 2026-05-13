package com.example.next_contest.wear.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class LocationSharingRepository {
    private final FirebaseAuth auth;
    private final DatabaseReference db;

    public LocationSharingRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
    }

    public void ensureCurrentUserLocationNode(FailureCallback onFailure) {
        String uid = getCurrentUid();
        if (uid == null) {
            onFailure.onFailure("로그인이 필요합니다.");
            return;
        }

        db.child("locations")
                .child(uid)
                .child("sos")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        db.child("locations")
                                .child(uid)
                                .child("sos")
                                .setValue(false)
                                .addOnFailureListener(error ->
                                        onFailure.onFailure(error.getMessage() == null ? "SOS 초기화에 실패했습니다." : error.getMessage())
                                );
                    }
                })
                .addOnFailureListener(error ->
                        onFailure.onFailure(error.getMessage() == null ? "위치 노드를 확인하지 못했습니다." : error.getMessage())
                );
    }

    public void updateCurrentUserLocation(double latitude, double longitude, FailureCallback onFailure) {
        String uid = getCurrentUid();
        if (uid == null) {
            onFailure.onFailure("로그인이 필요합니다.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("latitude", latitude);
        updates.put("longitude", longitude);
        updates.put("timestamp", ServerValue.TIMESTAMP);
        updates.put("isOnline", true);

        db.child("locations")
                .child(uid)
                .updateChildren(updates)
                .addOnFailureListener(error ->
                        onFailure.onFailure(error.getMessage() == null ? "위치 공유에 실패했습니다." : error.getMessage())
                );
    }

    public void markCurrentUserOffline() {
        String uid = getCurrentUid();
        if (uid == null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("timestamp", ServerValue.TIMESTAMP);
        updates.put("isOnline", false);

        db.child("locations")
                .child(uid)
                .updateChildren(updates);
    }

    private String getCurrentUid() {
        return auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
    }

    public interface FailureCallback {
        void onFailure(String message);
    }
}
