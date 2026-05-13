# TODO

## 바로 해야 할 작업

- [ ] Kakao API 키 적용하기
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
  - `users/{uid}/homeLocation`, `users/{uid}/safeZone`은 본인만 읽고 쓸 수 있게 제한한다.
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
