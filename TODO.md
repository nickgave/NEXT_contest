# TODO

## 2026-05-14 지도 마커 아이콘 렌더링 / 위치 추적 마커 구분 기록

- 완료:
  - [x] 장소 설정 지도에서 선택 마커가 보이지 않던 문제 수정
  - [x] VectorDrawable을 Bitmap으로 변환해 Kakao Label에 넣는 공통 마커 스타일 생성
  - [x] 위치 추적 지도에서 `나`, `어르신/상대` 글자 라벨 대신 아이콘 마커로 표시
  - [x] 내 위치는 원형 현재 위치 아이콘, 상대 위치는 핀 아이콘, 장소 선택은 별도 선택 핀 아이콘으로 구분
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - `app/src/main/java/com/example/next_contest/util/MapMarkerStyleFactory.kt`
    - drawable 리소스를 Bitmap으로 렌더링한 뒤 Kakao `LabelStyles`로 변환하는 공통 유틸 추가
  - `app/src/main/res/drawable/ic_map_marker_selected.xml`
    - 장소 설정에서 선택 지점을 표시하는 별도 핀 아이콘 추가

- 수정:
  - `app/src/main/java/com/example/next_contest/controller/KakaoPlacePickerMapController.kt`
    - 선택 지점 표시를 `ic_map_marker_selected` Bitmap 마커로 변경
    - 텍스트 라벨 없이 아이콘만 표시하도록 변경
  - `app/src/main/java/com/example/next_contest/controller/KakaoLocationMapController.kt`
    - 위치 추적의 `나`/`상대` 텍스트 라벨 제거
    - 내 위치와 상대 위치를 서로 다른 아이콘 마커로 표시
  - `app/src/main/res/drawable/ic_map_marker_me.xml`
    - 내 위치가 핀과 헷갈리지 않도록 원형 현재 위치 아이콘으로 변경

- 삭제:
  - 없음

## 2026-05-14 장소 설정 검색 버튼 / 지도 핀 표시 수정 기록

- 완료:
  - [x] 주소 검색 버튼의 `검색` 글자가 `검`처럼 잘려 보이던 문제 수정
  - [x] 장소 설정 지도에서 선택 위치를 글자 라벨이 아닌 핀 아이콘만 표시하도록 변경
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - 없음

- 수정:
  - `app/src/main/res/layout/activity_place_setting.xml`
    - `btnSearchAddress` 너비를 `74dp -> 88dp`로 늘림
    - 검색 버튼에 `minWidth=0dp`, 좌우 padding `0dp`, 중앙 정렬 적용
    - 검색 버튼 글자 크기 `15sp -> 14sp`로 조정해 `검색` 전체가 보이도록 수정
  - `app/src/main/java/com/example/next_contest/controller/KakaoPlacePickerMapController.kt`
    - 선택 위치 라벨의 `선택 위치` 텍스트 표시 제거
    - `ic_map_marker_other` 아이콘만 지도 핀으로 표시하도록 변경

- 삭제:
  - 없음

## 2026-05-14 모바일 화면 상하 폭 축소 / 시스템 바 인셋 보정 기록

- 완료:
  - [x] 전체 모바일 화면의 상단/하단 기본 여백 축소
  - [x] 뒤로가기/설정/로그아웃 칩 버튼 높이 축소
  - [x] 메인 화면 카드 높이와 카드 사이 간격 축소
  - [x] 로그인/회원가입 입력창, 버튼, 제목 상단 간격 축소
  - [x] 지도/위치추적/장소설정 화면의 지도 상하 여백과 하단 패널 높이 축소
  - [x] Android 상태바/네비게이션바 인셋을 코드에서 공통 적용하도록 변경
  - [x] 하단 정보 패널이 네비게이션 바에 깔리지 않도록 보정
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - `app/src/main/java/com/example/next_contest/util/SystemBarInsets.kt`
    - `setContentView()` 이후 현재 콘텐츠 루트에 status/navigation bar inset padding을 더하는 공통 유틸 추가

- 수정:
  - `app/src/main/java/com/example/next_contest/MainActivity.kt`
    - edge-to-edge 모드에서 모든 화면 전환마다 `applySystemBarInsetsToContent()` 적용
  - `app/src/main/java/com/example/next_contest/TracingActivity.kt`
    - 추적 전용 Activity에도 동일한 시스템 바 인셋 적용
  - `app/src/main/res/layout/activity_map.xml`
    - 상단 헤더/지도 여백/하단 위치 정보 패널 높이 축소
  - `app/src/main/res/layout/activity_tracing.xml`
    - 상단 헤더/지도 여백/SOS 배너/하단 추적 정보 패널 높이 축소
  - `app/src/main/res/layout/activity_place_setting.xml`
    - 장소 설정 지도와 하단 검색/저장 패널 세로 폭 축소
  - `app/src/main/res/layout/activity_safe_zone.xml`
    - 안전구역 지도 화면 상단 버튼, 지도 margin, 반경 패널 폭 축소
  - `app/src/main/res/layout/activity_home_navigation.xml`
    - 길안내 안내문/화살표/하단 버튼의 세로 폭 축소
  - `app/src/main/res/layout/activity_patient_main.xml`
    - 어르신 메인 화면 상단 액션, 제목 간격, 카드 높이 축소
  - `app/src/main/res/layout/activity_guardian_main.xml`
    - 보호자 메인 화면 상단 액션, 제목 간격, 카드 높이 축소
  - `app/src/main/res/layout/activity_user_auth.xml`
    - 로그인 화면 상단 빈 공간, 입력창, 버튼 높이 축소
  - `app/src/main/res/layout/activity_login.xml`
    - 역할 선택 화면 제목 간격과 선택 카드 높이 축소
  - `app/src/main/res/layout/activity_signup.xml`
    - 회원가입 화면 제목/입력창/버튼 세로 폭 축소
  - `app/src/main/res/layout/activity_settings.xml`
    - 설정 화면 헤더, 카드 padding, 입력/버튼 높이 축소
  - `app/src/main/res/layout/activity_daily_info.xml`
    - 오늘 정보 화면 헤더와 카드 내부 padding 축소
  - `app/src/main/res/layout/activity_seek_help.xml`
    - 도움 요청 화면 상단 빈 공간과 큰 버튼 높이 축소

- 삭제:
  - 없음

## 2026-05-14 로그인 뒤로가기 / 입력창 포커스 화면 밀림 수정 기록

- 완료:
  - [x] 로그인 화면에서도 시스템 뒤로가기를 두 번 누르면 앱이 종료되도록 변경
  - [x] 로그인/회원가입/역할 선택/메인/설정 화면의 좌우 여백을 `ScrollView`가 아니라 내부 컨테이너에 적용
  - [x] 입력창 포커스 또는 키보드 표시 시 콘텐츠가 왼쪽으로 밀리거나 화면 폭이 꼬여 보이는 문제 완화
  - [x] `MainActivity`에 `windowSoftInputMode="adjustResize"` 적용
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - 없음
  - 빌드 산출물 갱신: `app/build/outputs/apk/debug/app-debug.apk`

- 수정:
  - `app/src/main/java/com/example/next_contest/MainActivity.kt`
    - `AppScreen.LOGIN` 뒤로가기 동작을 `handleMainBackPressed()`로 변경
  - `app/src/main/AndroidManifest.xml`
    - `MainActivity`에 `android:windowSoftInputMode="adjustResize"` 추가
  - `app/src/main/res/layout/activity_user_auth.xml`
    - 루트 `ScrollView` 좌우 패딩 제거
    - 내부 컨테이너 좌우 패딩 적용
  - `app/src/main/res/layout/activity_signup.xml`
  - `app/src/main/res/layout/activity_login.xml`
  - `app/src/main/res/layout/activity_patient_main.xml`
  - `app/src/main/res/layout/activity_guardian_main.xml`
  - `app/src/main/res/layout/activity_settings.xml`
    - 같은 방식으로 좌우 여백을 내부 컨테이너로 이동
  - `TODO.md`
    - 이번 수정/빌드 기록 추가

- 삭제:
  - 없음

## 2026-05-13 화면 폭/카드 그림자/집 주소 표시 정리 기록

- 완료:
  - [x] 어르신 메인 화면 좌우 여백 확대
  - [x] 보호자 메인 화면 좌우 여백 확대
  - [x] 로그인 화면 좌우 여백 확대
  - [x] 회원가입/역할 선택 화면 좌우 여백 확대
  - [x] 설정 화면 좌우 여백 확대
  - [x] 앱 전체 CardView 그림자를 `0dp`로 정리해 애매한 음영 제거
  - [x] 설정 화면의 집 위치 설정 카드에 현재 저장된 집 주소 표시 추가
  - [x] 어르신 계정은 본인 `homeLocation`, 보호자 계정은 연결된 어르신 `homeLocation`을 읽어 표시
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - 없음
  - 빌드 산출물 갱신: `app/build/outputs/apk/debug/app-debug.apk`

- 수정:
  - `app/src/main/res/layout/activity_patient_main.xml`
    - 좌우 패딩 `20dp -> 34dp`
    - 카드 그림자 제거
  - `app/src/main/res/layout/activity_guardian_main.xml`
    - 좌우 패딩 `20dp -> 34dp`
    - 카드 그림자 제거
  - `app/src/main/res/layout/activity_user_auth.xml`
    - 로그인 화면 좌우 패딩 `24dp -> 36dp`
    - 로그인 버튼 카드 그림자 제거
  - `app/src/main/res/layout/activity_login.xml`
    - 역할 선택 화면 좌우 패딩 `22dp -> 36dp`
    - 카드 그림자 제거
  - `app/src/main/res/layout/activity_signup.xml`
    - 회원가입 화면 좌우 패딩 `24dp -> 36dp`
    - 카드 그림자 제거
  - `app/src/main/res/layout/activity_settings.xml`
    - 좌우 패딩 `20dp -> 30dp`
    - `tvHomeSettingCurrentAddress` 추가
    - 카드 그림자 제거
  - `app/src/main/java/com/example/next_contest/controller/SimpleScreenController.kt`
    - 설정 화면 진입 시 현재 집 주소를 로드해 표시
  - `app/src/main/res/layout/activity_daily_info.xml`
  - `app/src/main/res/layout/activity_tracing.xml`
  - `app/src/main/res/layout/activity_safe_zone.xml`
  - `app/src/main/res/layout/activity_main.xml`
    - 남아 있던 CardView 그림자 제거
  - `TODO.md`
    - 이번 수정/빌드 기록 추가

- 삭제:
  - 없음

## 2026-05-13 휴대폰 앱 UI/UX 전면 개편 기록

- 완료:
  - [x] 어르신 메인 화면을 스크롤 가능한 카드형 홈으로 개편
  - [x] 보호자 메인 화면을 같은 카드/아이콘 규칙으로 개편
  - [x] 로그인, 회원가입, 역할 선택 화면의 입력/버튼/카드 스타일 통일
  - [x] 설정 화면을 집 위치 섹션과 연결 섹션 카드로 분리
  - [x] 도움 요청 화면 버튼을 큰 액션 버튼으로 재배치
  - [x] 오늘 정보 화면을 날짜/날씨/추천 활동 카드 구조로 정리
  - [x] 집 길안내 화면의 상단 안내, 중앙 화살표, 하단 액션 버튼 재배치
  - [x] 실시간 위치 확인/추적 지도 화면의 상단 바와 하단 정보 패널 정리
  - [x] 장소 설정 화면의 검색/저장 패널을 지도 하단에 안정적으로 배치
  - [x] 주요 화면 루트에 `fitsSystemWindows`와 하단 여백을 적용해 시스템 네비게이션 바 겹침 완화
  - [x] `MainActivity`, `TracingActivity`에서 `WindowCompat.setDecorFitsSystemWindows(window, true)` 적용
  - [x] 공통 카드/칩/버튼/아이콘 리소스 추가
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - `app/src/main/res/drawable/bg_toolbar.xml`
  - `app/src/main/res/drawable/bg_chip.xml`
  - `app/src/main/res/drawable/bg_card_primary.xml`
  - `app/src/main/res/drawable/bg_card_success.xml`
  - `app/src/main/res/drawable/bg_card_danger.xml`
  - `app/src/main/res/drawable/bg_button_secondary.xml`
  - `app/src/main/res/drawable/bg_button_danger.xml`
  - `app/src/main/res/drawable/bg_icon_soft.xml`
  - `app/src/main/res/drawable/ic_home.xml`
  - `app/src/main/res/drawable/ic_calendar.xml`
  - `app/src/main/res/drawable/ic_location_pin.xml`
  - `app/src/main/res/drawable/ic_help_circle.xml`
  - `app/src/main/res/drawable/ic_shield.xml`
  - `app/src/main/res/drawable/ic_settings.xml`
  - `app/src/main/res/drawable/ic_logout.xml`
  - `app/src/main/res/drawable/ic_phone.xml`
  - `app/src/main/res/drawable/ic_person.xml`
  - `app/src/main/res/drawable/ic_search.xml`
  - `app/src/main/res/drawable/ic_arrow_forward.xml`
  - 빌드 산출물 갱신: `app/build/outputs/apk/debug/app-debug.apk`

- 수정:
  - `app/src/main/res/values/colors.xml`
    - 표면/강조/역상 텍스트 색상 추가
  - `app/src/main/res/values/themes.xml`
    - light navigation bar 설정 추가
  - `app/src/main/java/com/example/next_contest/MainActivity.kt`
    - 시스템 바 영역과 앱 콘텐츠 겹침 방지 설정 추가
  - `app/src/main/java/com/example/next_contest/TracingActivity.kt`
    - 시스템 바 영역과 앱 콘텐츠 겹침 방지 설정 추가
  - `app/src/main/res/layout/activity_patient_main.xml`
  - `app/src/main/res/layout/activity_guardian_main.xml`
  - `app/src/main/res/layout/activity_user_auth.xml`
  - `app/src/main/res/layout/activity_signup.xml`
  - `app/src/main/res/layout/activity_login.xml`
  - `app/src/main/res/layout/activity_settings.xml`
  - `app/src/main/res/layout/activity_home_navigation.xml`
  - `app/src/main/res/layout/activity_seek_help.xml`
  - `app/src/main/res/layout/activity_daily_info.xml`
  - `app/src/main/res/layout/activity_map.xml`
  - `app/src/main/res/layout/activity_place_setting.xml`
  - `app/src/main/res/layout/activity_tracing.xml`
  - `app/src/main/res/layout/activity_safe_zone.xml`
  - `TODO.md`

- 삭제:
  - 없음

## 2026-05-13 보호자 어르신 집 위치 변경 / 연결 해제 구현 기록

- 완료:
  - [x] 보호자 설정 화면의 집 위치 설정 문구를 `어르신 집 위치 설정`으로 변경
  - [x] 보호자가 집 위치 설정 화면을 열면 연결된 어르신의 `users/{elderlyUid}/homeLocation`을 불러오도록 구현
  - [x] 보호자가 주소 검색 또는 지도 선택 후 연결된 어르신의 집 위치를 저장하도록 구현
  - [x] 보호자 계정, 연결된 어르신 계정, 양방향 pairedUid가 맞는지 검증 후 저장
  - [x] 보호자/어르신 양쪽 설정 화면에서 `연결 해제하기` 기능 추가
  - [x] 연결 해제 시 `users/{currentUid}/pairedUid`, `users/{pairedUid}/pairedUid`와 pairedAt을 함께 제거
  - [x] 설정 화면을 ScrollView로 변경해 작은 화면에서도 버튼이 잘리지 않게 조정
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - 없음
  - 빌드 산출물 갱신: `app/build/outputs/apk/debug/app-debug.apk`

- 수정:
  - `app/src/main/java/com/example/next_contest/data/place/UserPlaceRepository.kt`
    - 특정 UID의 `homeLocation`을 읽고 저장하는 `loadHomeForUser`, `saveHomeForUser` 추가
  - `app/src/main/java/com/example/next_contest/service/PlaceService.kt`
    - 보호자 -> 연결된 어르신 UID 검증 후 `loadPairedElderlyHome`, `savePairedElderlyHome` 처리 추가
  - `app/src/main/java/com/example/next_contest/controller/PlaceSettingController.kt`
    - `HomeSettingTarget` 추가
    - 내 집 설정과 연결된 어르신 집 설정을 같은 화면에서 분기 처리
  - `app/src/main/java/com/example/next_contest/MainActivity.kt`
    - 보호자 계정은 집 설정 화면에서 `PAIRED_ELDERLY` 대상으로 진입하도록 변경
  - `app/src/main/java/com/example/next_contest/data/pairing/PairingRepository.kt`
    - 양방향 pairedUid/pairedAt 제거용 `disconnectPairing` 추가
  - `app/src/main/java/com/example/next_contest/service/PairingService.kt`
    - 현재 로그인 사용자의 연결 상태를 확인한 뒤 연결 해제를 수행하는 서비스 추가
  - `app/src/main/java/com/example/next_contest/controller/SimpleScreenController.kt`
    - 보호자 설정 문구 변경
    - 연결 해제 확인 다이얼로그 및 버튼 상태 갱신 추가
  - `app/src/main/res/layout/activity_settings.xml`
    - `ScrollView` 적용
    - `btnDisconnectPairing` 추가
  - `TODO.md`
    - 이번 수정/빌드 기록 및 Firebase Rules TODO 갱신

- 삭제:
  - 없음

- Firebase Rules 반영 필요:
  - 보호자가 연결된 어르신의 `users/{elderlyUid}/homeLocation`을 읽고 쓸 수 있어야 한다.
  - 현재 Rules가 본인 UID만 허용하면 저장 시 `Permission denied`가 난다.

## 2026-05-13 워치 원형 길안내 UI 재배치 기록

- 완료:
  - [x] 워치 메인 화면에서 큰 거리 텍스트를 제거하고 화살표를 중앙 핵심 요소로 재배치
  - [x] 거리(`km`, `m`) 표시는 화살표 아래의 작은 텍스트로 이동
  - [x] `다시 듣기` 버튼은 화살표 왼쪽, `도움 요청` 버튼은 화살표 오른쪽에 배치
  - [x] 원형 화면에서 로그아웃을 제외한 안내/버튼 요소가 한 화면 안에 들어가도록 여백과 글자 크기 축소
  - [x] 로그아웃은 기존처럼 아래로 내려야 보이는 위치 유지
  - [x] 일반 워치 레이아웃도 같은 구조로 정리
  - [x] 매우 먼 거리는 `9116.2 km` 대신 `9116 km`처럼 짧게 표시
  - [x] `.\gradlew.bat :wear:assembleDebug` 통과
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - 없음
  - 빌드 산출물 갱신: `wear/build/outputs/apk/debug/wear-debug.apk`

- 수정:
  - `wear/src/main/res/layout-round/activity_watch_main.xml`
    - 원형 화면 전용 메인 안내 UI를 `상태/주소 -> 다시 듣기 + 화살표 + 도움 요청 -> 거리/방향/안내` 순서로 재배치
    - 버튼을 화살표 양옆에 고정하고 거리 텍스트를 14sp로 축소
  - `wear/src/main/res/layout/activity_watch_main.xml`
    - 일반 워치 화면도 같은 배치 구조로 맞춤
  - `wear/src/main/java/com/example/next_contest/wear/WatchMainActivity.java`
    - 100km 이상 거리는 소수점 없이 km로 표시하도록 `formatDistance()` 조정
    - 사용하지 않는 `android.view.View` import 제거
  - `TODO.md`
    - 이번 수정/빌드 기록 추가

- 삭제:
  - 없음

## 바로 해야 할 작업

- [x] Kakao API 키 적용하기
  - Kakao Developers에서 Android 플랫폼을 등록한다.
  - 패키지명: `com.example.next_contest`
  - 디버그 키 해시: `MPTx98iWYEk4pMLyQXTPor4JK9I=`
  - `local.properties`에 아래 값을 추가한다.
    ```properties
    KAKAO_MAP_NATIVE_KEY=발급받은_네이티브_앱_키
    KAKAO_REST_API_KEY=발급받은_REST_API_키
    ```
  - `KAKAO_MAP_NATIVE_KEY`: 지도 표시용
  - `KAKAO_REST_API_KEY`: 주소 검색, 좌표->주소 변환용

- [ ] 실제 기기에서 장소 설정 확인하기
  - 설정 > 집 위치 설정에서 주소 검색이 되는지 확인한다.
  - 집 위치 설정에서 지도를 눌렀을 때 핀이 이동하고 주소가 표시되는지 확인한다.
  - 보호자 홈 > 안전구역 설정에서 주소 검색/지도 선택/반경 조절이 되는지 확인한다.
  - 저장 후 Firebase `users/{uid}/homeLocation`, `users/{uid}/safeZone`에 값이 들어가는지 확인한다.

- [ ] Firebase Realtime Database Rules 최종 적용하기
  - `users/{uid}/homeLocation`은 본인 또는 양방향으로 연결된 보호자가 읽고 쓸 수 있게 제한한다.
  - `users/{uid}/safeZone`은 본인만 읽고 쓸 수 있게 제한한다.
  - `locations/{uid}`는 본인과 페어링된 상대만 접근할 수 있게 제한한다.

## 다음 작업

- [ ] 안전구역 이탈 감지 구현하기
  - 현재는 안전구역 중심/반경 저장까지만 구현되어 있다.
  - 어르신 위치와 `safeZone` 중심점의 거리를 비교해 반경 밖이면 보호자에게 알리는 흐름이 필요하다.

- [ ] 집 목적지 설정 UX 다듬기
  - 저장된 집 주소를 설정 화면에 요약 표시한다.
  - 집 안내 시작 전에 목적지 주소를 한 번 보여주는 확인 화면을 검토한다.

- [ ] 위치 권한 UX 개선하기
  - 권한 거부 시 재요청 안내, 설정 화면 이동, 기능 제한 안내를 추가한다.
  - 날씨/집 안내/추적/위치 공유 권한 요청 흐름을 공통화할 수 있는지 검토한다.

- [ ] API 키 관리 방식 정리하기
  - Tmap, Kakao Maps, 날씨 API 키가 모두 로컬 설정 또는 안전한 빌드 설정에서 관리되도록 정리한다.

- [ ] 문자열 리소스 분리하기
  - Kotlin/XML에 직접 들어간 한국어 문구를 `strings.xml`로 이동한다.

- [ ] 테스트 보강하기
  - Firebase snapshot을 모델로 변환하는 로직
  - 페어링 요청/수락/취소 상태 계산
  - 위치 거리 계산 및 방향 문구
  - Kakao Local API 응답 파싱
  - 날씨 API 응답 파싱

- [ ] 백업/데이터 추출 정책 정리하기
  - 토큰, 위치 캐시, 사용자 민감 정보가 로컬에 저장될 경우 백업 제외 규칙을 추가한다.

- [ ] CompassHelper 구현 방식 개선하기
  - deprecated 된 `Sensor.TYPE_ORIENTATION` 사용 여부를 확인한다.
  - 가속도계/자이로 기반 회전 벡터 센서로 교체 가능한지 검토한다.

## 완료된 작업

- [x] Firebase Auth 로그인/회원가입 연결
- [x] 회원가입 사용자 정보 저장
- [x] 백엔드 구조를 Repository/Service로 분리
- [x] 보호자-어르신 연결 요청/수락/취소 흐름 구현
- [x] 양방향 페어링 저장
- [x] 어르신 위치 공유 쓰기 구현
- [x] 보호자 실시간 위치 추적 읽기 구현
- [x] 실시간 위치 확인 화면을 Kakao Maps와 연결
- [x] 전체 화면 디자인 정리
- [x] 시스템 뒤로가기 제어
- [x] 집 위치 설정 구현
  - 주소/장소 검색으로 집 좌표 저장
  - 지도 탭으로 집 좌표 저장
  - 저장된 집 위치를 집 안내 목적지로 사용
- [x] 안전구역 설정 구현
  - 주소/장소 검색으로 안전구역 중심 저장
  - 지도 탭으로 안전구역 중심 저장
  - SeekBar로 안전 반경 저장
  - 지도에 안전구역 반경 폴리곤 표시
- [x] `.\gradlew.bat assembleDebug` 통과

## 최근 변경 파일 기록

- 생성:
  - `app/src/main/java/com/example/next_contest/model/SavedPlace.kt`
  - `app/src/main/java/com/example/next_contest/data/place/KakaoLocalRepository.kt`
  - `app/src/main/java/com/example/next_contest/data/place/UserPlaceRepository.kt`
  - `app/src/main/java/com/example/next_contest/service/PlaceService.kt`
  - `app/src/main/java/com/example/next_contest/controller/KakaoPlacePickerMapController.kt`
  - `app/src/main/java/com/example/next_contest/controller/PlaceSettingController.kt`
  - `app/src/main/res/layout/activity_place_setting.xml`

- 수정:
  - `app/build.gradle.kts`
  - `app/src/main/java/com/example/next_contest/MainActivity.kt`
  - `app/src/main/java/com/example/next_contest/controller/HomeNavigationController.kt`
  - `app/src/main/java/com/example/next_contest/controller/SimpleScreenController.kt`
  - `app/src/main/java/com/example/next_contest/data/route/RouteRepository.kt`
  - `app/src/main/res/layout/activity_settings.xml`

## 2026-05-13 워치 원형 안내 화면 재구성 기록

- 완료:
  - [x] 워치 메인 화면을 시작/종료 버튼 중심에서 집 방향 안내 중심으로 재구성
  - [x] 로그인 후 집 위치가 로드되면 자동으로 집 안내 시작
  - [x] 원형 화면 첫 화면을 거리/화살표/방향/안내 문구가 크게 차지하도록 변경
  - [x] `집 안내 시작`, `안내 종료` 버튼 제거
  - [x] 일반 버튼은 `다시 듣기`, `도움 요청`만 유지
  - [x] `로그아웃`은 아래로 스크롤해야 보이는 하단 영역으로 이동
  - [x] 도움 요청 화면에 들어가도 길 안내 위치 업데이트를 중지하지 않도록 변경
  - [x] 시스템 뒤로가기에서도 길 안내를 중지하지 않도록 변경
  - [x] 도착해도 위치 업데이트 콜백은 유지되도록 변경
  - [x] `.\gradlew.bat :wear:assembleDebug` 통과
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - 없음

- 수정:
  - `wear/src/main/java/com/example/next_contest/wear/WatchMainActivity.java`
  - `wear/src/main/res/layout/activity_watch_main.xml`
  - `wear/src/main/res/layout-round/activity_watch_main.xml`
  - `TODO.md`

- 삭제:
  - 파일 삭제 없음
  - 워치 메인 화면의 `btnStartNavigation`, `btnStopNavigation` 제거

## 2026-05-13 워치 원형 화면 UI 기록

- 완료:
  - [x] 원형 Wear OS 화면 전용 `layout-round` 리소스 추가
  - [x] 로그인 화면 원형 안전 여백 적용
  - [x] 집 안내 메인 화면 원형 안전 여백 적용
  - [x] 도움 요청 화면 원형 안전 여백 적용
  - [x] 원형 화면에서 버튼 높이/폰트/화살표 크기 축소
  - [x] 원형 모서리에 텍스트와 버튼이 닿지 않도록 좌우/상하 padding 조정
  - [x] `.\gradlew.bat :wear:assembleDebug` 통과
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - `wear/src/main/res/layout-round/activity_watch_main.xml`
  - `wear/src/main/res/layout-round/activity_watch_login.xml`
  - `wear/src/main/res/layout-round/activity_watch_help.xml`

- 수정:
  - `TODO.md`

- 삭제:
  - 없음

## 2026-05-13 워치 TTS/화살표 길안내 기록

- 완료:
  - [x] 워치 집 안내 화면에 방향 화살표 추가
  - [x] 현재 위치에서 집까지 bearing 계산 후 화살표 회전
  - [x] 워치 나침반 센서 기반 방향 보정 추가
  - [x] `TYPE_ROTATION_VECTOR` 우선 사용, 없으면 `TYPE_ORIENTATION` fallback
  - [x] TTS 초기화 완료 전 안내 문구를 보류했다가 준비되면 재생
  - [x] TTS 한국어, 말 속도, 음높이 설정
  - [x] 경로 탐색 성공/실패 fallback 안내 음성 추가
  - [x] `다시 듣기`가 마지막 TTS 문구를 다시 말하도록 유지
  - [x] `.\gradlew.bat :wear:assembleDebug` 통과
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - `wear/src/main/java/com/example/next_contest/wear/util/WearCompassHelper.java`
  - `wear/src/main/res/drawable/wear_arrow_up.xml`

- 수정:
  - `wear/src/main/java/com/example/next_contest/wear/WatchMainActivity.java`
  - `wear/src/main/res/layout/activity_watch_main.xml`
  - `TODO.md`

- 삭제:
  - 없음

## 2026-05-13 워치 도움 요청 기능 구현 기록

- 완료:
  - [x] 워치 메인 화면에 `도움 요청` 버튼 추가
  - [x] 워치 도움 요청 화면 추가
  - [x] `보호자 전화` 버튼 구현
  - [x] Firebase `users/{uid}/pairedUid`로 연결된 보호자 조회
  - [x] 연결된 보호자의 `phoneNumber`를 읽어 전화 앱 열기
  - [x] `112 전화` 버튼 구현
  - [x] 전화 권한 없이 동작하도록 `ACTION_DIAL` 방식 사용
  - [x] 워치 도움 요청 화면에서 뒤로가기 지원
  - [x] `.\gradlew.bat :wear:assembleDebug` 통과
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - `wear/src/main/res/layout/activity_watch_help.xml`
  - `wear/src/main/res/drawable/wear_bg_button_danger.xml`
  - `wear/src/main/java/com/example/next_contest/wear/data/PairingRepository.java`

- 수정:
  - `wear/src/main/res/layout/activity_watch_main.xml`
  - `wear/src/main/res/values/colors.xml`
  - `wear/src/main/java/com/example/next_contest/wear/WatchMainActivity.java`
  - `TODO.md`

- 삭제:
  - 없음

## 2026-05-13 도움 요청 기능 구현 기록

- 완료:
  - [x] 도움 요청 화면에서 `내 위치 알리기` 버튼 삭제
  - [x] `보호자에게 전화하기` 버튼 구현
  - [x] 연결된 보호자가 없거나 전화번호가 없을 때 안내 Toast 표시
  - [x] `경찰(112)에 도움 요청` 버튼 구현
  - [x] 전화 권한 없이 동작하도록 전화 앱을 여는 `ACTION_DIAL` 방식 사용
  - [x] 집 안내 화면의 `도움 요청` 버튼을 도움 요청 화면으로 연결
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - 없음

- 수정:
  - `app/src/main/res/layout/activity_seek_help.xml`
  - `app/src/main/java/com/example/next_contest/controller/SimpleScreenController.kt`
  - `app/src/main/java/com/example/next_contest/controller/HomeNavigationController.kt`
  - `app/src/main/java/com/example/next_contest/MainActivity.kt`
  - `TODO.md`

- 삭제:
  - 파일 삭제 없음
  - `activity_seek_help.xml`의 `btnSendLocation` 버튼 제거

## 2026-05-13 휴대폰 로그인 유지 기록

- 완료:
  - [x] 휴대폰 앱 시작 시 Firebase Auth 저장 세션 복구
  - [x] 저장된 세션이 있으면 사용자 role을 읽고 어르신/보호자 메인 화면으로 자동 진입
  - [x] 로그인 화면 진입과 실제 로그아웃 동작 분리
  - [x] 로그아웃 버튼을 눌렀을 때만 `FirebaseAuth.signOut()` 실행
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - 없음

- 수정:
  - `app/src/main/java/com/example/next_contest/MainActivity.kt`
  - `app/src/main/java/com/example/next_contest/controller/AuthController.kt`
  - `app/src/main/java/com/example/next_contest/service/AuthService.kt`
  - `app/src/main/java/com/example/next_contest/data/auth/AuthRepository.kt`
  - `TODO.md`

- 삭제:
  - 없음

## 2026-05-13 워치 포팅 기록

- 완료:
  - [x] `wear` 모듈 추가
  - [x] 워치에서는 어르신 계정만 로그인 허용
  - [x] Firebase Auth 로그인 상태를 로그아웃 전까지 유지
  - [x] 워치 로그인 후 Foreground Service로 어르신 위치 공유
  - [x] Firebase `users/{uid}/homeLocation`을 읽어 집 안내 목적지로 사용
  - [x] 워치 화면에서 집 안내 시작/종료/다시 듣기/로그아웃 제공
  - [x] Tmap 키가 없거나 경로 요청이 실패하면 직선 방향 안내로 fallback
  - [x] `.\gradlew.bat :wear:assembleDebug` 통과
  - [x] `.\gradlew.bat assembleDebug` 통과

- 생성:
  - `wear/build.gradle.kts`
  - `wear/proguard-rules.pro`
  - `wear/google-services.json`
  - `wear/src/main/AndroidManifest.xml`
  - `wear/src/main/java/com/example/next_contest/wear/WatchMainActivity.java`
  - `wear/src/main/java/com/example/next_contest/wear/WatchLocationService.java`
  - `wear/src/main/java/com/example/next_contest/wear/data/AuthRepository.java`
  - `wear/src/main/java/com/example/next_contest/wear/data/UserPlaceRepository.java`
  - `wear/src/main/java/com/example/next_contest/wear/data/LocationSharingRepository.java`
  - `wear/src/main/java/com/example/next_contest/wear/data/RouteRepository.java`
  - `wear/src/main/java/com/example/next_contest/wear/model/SavedPlace.java`
  - `wear/src/main/java/com/example/next_contest/wear/model/NavStep.java`
  - `wear/src/main/java/com/example/next_contest/wear/util/GeoUtils.java`
  - `wear/src/main/res/layout/activity_watch_login.xml`
  - `wear/src/main/res/layout/activity_watch_main.xml`
  - `wear/src/main/res/values/strings.xml`
  - `wear/src/main/res/values/colors.xml`
  - `wear/src/main/res/values/themes.xml`
  - `wear/src/main/res/drawable/wear_bg_screen.xml`
  - `wear/src/main/res/drawable/wear_bg_panel.xml`
  - `wear/src/main/res/drawable/wear_bg_input.xml`
  - `wear/src/main/res/drawable/wear_bg_button_primary.xml`
  - `wear/src/main/res/drawable/wear_bg_button_secondary.xml`
  - `wear/src/main/res/drawable/ic_stat_location.xml`

- 수정:
  - `settings.gradle.kts`: `:wear` 모듈 include 추가

- 삭제:
  - 없음
