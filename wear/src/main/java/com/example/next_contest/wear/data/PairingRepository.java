package com.example.next_contest.wear.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PairingRepository {
    private final FirebaseAuth auth;
    private final DatabaseReference db;

    public PairingRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
    }

    public void loadConnectedCaregiver(ConnectedUserCallback callback) {
        String uid = auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();

        if (uid == null) {
            callback.onFailure("로그인이 필요합니다.");
            return;
        }

        db.child("users")
                .child(uid)
                .child("pairedUid")
                .get()
                .addOnSuccessListener(snapshot -> {
                    String pairedUid = snapshot.getValue(String.class);

                    if (pairedUid == null || pairedUid.trim().isEmpty()) {
                        callback.onFailure("연결된 보호자가 없습니다.");
                        return;
                    }

                    loadUser(pairedUid, callback);
                })
                .addOnFailureListener(error ->
                        callback.onFailure(error.getMessage() == null ? "연결 정보를 불러오지 못했습니다." : error.getMessage())
                );
    }

    private void loadUser(String uid, ConnectedUserCallback callback) {
        db.child("users")
                .child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    ConnectedUser user = toConnectedUser(snapshot);

                    if (user == null) {
                        callback.onFailure("보호자 정보를 찾지 못했습니다.");
                        return;
                    }

                    callback.onSuccess(user);
                })
                .addOnFailureListener(error ->
                        callback.onFailure(error.getMessage() == null ? "보호자 정보를 불러오지 못했습니다." : error.getMessage())
                );
    }

    private ConnectedUser toConnectedUser(DataSnapshot snapshot) {
        if (!snapshot.exists()) {
            return null;
        }

        String name = snapshot.child("name").getValue(String.class);
        String phoneNumber = snapshot.child("phoneNumber").getValue(String.class);

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }

        return new ConnectedUser(
                name == null || name.trim().isEmpty() ? "보호자" : name,
                phoneNumber
        );
    }

    public static class ConnectedUser {
        public final String name;
        public final String phoneNumber;

        public ConnectedUser(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
        }
    }

    public interface ConnectedUserCallback {
        void onSuccess(ConnectedUser user);

        void onFailure(String message);
    }
}
