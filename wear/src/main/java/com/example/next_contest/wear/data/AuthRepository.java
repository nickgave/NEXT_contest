package com.example.next_contest.wear.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AuthRepository {
    private static final String ROLE_ELDERLY = "elderly";

    private final FirebaseAuth auth;
    private final DatabaseReference db;

    public AuthRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
    }

    public boolean hasCurrentUser() {
        return auth.getCurrentUser() != null;
    }

    public void signOut() {
        auth.signOut();
    }

    public void requireCurrentElderly(RoleCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onFailure("로그인이 필요합니다.");
            return;
        }

        loadCurrentUserRole(callback);
    }

    public void signInElderlyOnly(String email, String password, RoleCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) {
                        callback.onFailure("사용자 정보를 확인하지 못했습니다.");
                        return;
                    }

                    loadCurrentUserRole(callback);
                })
                .addOnFailureListener(error ->
                        callback.onFailure(error.getMessage() == null ? "로그인에 실패했습니다." : error.getMessage())
                );
    }

    private void loadCurrentUserRole(RoleCallback callback) {
        String uid = auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();

        if (uid == null) {
            callback.onFailure("로그인이 필요합니다.");
            return;
        }

        db.child("users")
                .child(uid)
                .child("role")
                .get()
                .addOnSuccessListener(snapshot -> {
                    String role = snapshot.getValue(String.class);

                    if (role == null) {
                        auth.signOut();
                        callback.onFailure("회원 역할 정보를 찾지 못했습니다.");
                        return;
                    }

                    if (!ROLE_ELDERLY.equals(role.trim().toLowerCase())) {
                        auth.signOut();
                        callback.onFailure("워치에서는 어르신 계정만 로그인할 수 있습니다.");
                        return;
                    }

                    callback.onSuccess(role);
                })
                .addOnFailureListener(error -> {
                    callback.onFailure(error.getMessage() == null ? "회원 정보를 불러오지 못했습니다." : error.getMessage());
                });
    }

    public interface RoleCallback {
        void onSuccess(String role);

        void onFailure(String message);
    }
}
