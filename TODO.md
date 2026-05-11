# TODO

## 바로 해야 할 작업

- [ ] Kakao Maps 네이티브 앱 키 적용하기
  - Kakao Developers에서 앱을 만들고 Android 플랫폼을 등록한다.
  - 패키지명: `com.example.next_contest`
  - 디버그 키 해시: `MPTx98iWYEk4pMLyQXTPor4JK9I=`
  - `local.properties`에 아래 값을 추가한다.
    ```properties
    KAKAO_MAP_NATIVE_KEY=발급받은_네이티브_앱_키
    ```
  - 관련 파일:
    - `app/build.gradle.kts`
    - `app/src/main/java/com/example/next_contest/NextContestApplication.kt`

- [ ] 실제 기기에서 Kakao Maps 표시 확인하기
  - 앱 키 입력 후 `실시간 위치 확인`, `실시간 위치 추적` 화면에서 지도 타일이 뜨는지 확인한다.
  - 내 위치/상대 위치 라벨이 동시에 표시되는지 확인한다.
  - 두 위치가 모두 수신될 때 카메라가 두 위치를 포함하도록 이동하는지 확인한다.
  - 관련 파일:
    - `app/src/main/java/com/example/next_contest/controller/KakaoLocationMapController.kt`
    - `app/src/main/java/com/example/next_contest/controller/PairedLocationMapController.kt`
    - `app/src/main/java/com/example/next_contest/controller/TracingUiController.kt`

- [ ] Firebase Realtime Database Rules 최종 적용하기
  - `users`, `pairingRequests`, `locations` 경로 권한을 Firebase Console에 적용한다.
  - 본인 정보와 페어링된 상대 위치만 읽고 쓸 수 있게 제한한다.
  - 확인할 데이터 경로:
    - `users/{uid}`
    - `phoneIndex/{normalizedPhone}`
    - `pairingRequests/{requestId}`
    - `locations/{uid}`

- [ ] 도움 요청 화면 버튼 동작 구현하기
  - `btnCallCaregiver`: 연결된 보호자 전화 걸기 또는 전화 앱 열기
  - `btnCallPolice`: 112 전화 앱 열기
  - `btnSendLocation`: 현재 위치/SOS 상태 전송
  - 관련 파일:
    - `app/src/main/res/layout/activity_seek_help.xml`
    - `app/src/main/java/com/example/next_contest/controller/SimpleScreenController.kt`
    - `app/src/main/java/com/example/next_contest/data/tracking/LocationSharingRepository.kt`

## 다음 작업

- [ ] 안전구역 기능 실제 구현하기
  - 현재 화면은 디자인 정리와 반경 UI만 되어 있다.
  - Kakao Maps 표시, 중심 위치 선택, 반경 저장, 이탈 감지 로직이 필요하다.
  - 관련 파일:
    - `app/src/main/res/layout/activity_safe_zone.xml`
    - `app/src/main/java/com/example/next_contest/controller/SimpleScreenController.kt`

- [ ] 집 목적지 좌표를 사용자별로 설정 가능하게 만들기
  - 현재 집 목적지는 코드에 고정되어 있다.
  - 사용자별 집 좌표를 저장하고, 설정 화면에서 수정할 수 있게 만든다.
  - 관련 파일:
    - `app/src/main/java/com/example/next_contest/MainActivity.kt`
    - `app/src/main/java/com/example/next_contest/controller/HomeNavigationController.kt`

- [ ] 위치 권한 UX 개선하기
  - 권한 거부 시 재요청 안내, 설정 화면 이동, 기능 제한 안내를 추가한다.
  - 날씨/집 안내/추적/위치 공유 권한 요청 흐름을 공통화할 수 있는지 검토한다.
  - 관련 파일:
    - `app/src/main/java/com/example/next_contest/MainActivity.kt`
    - `app/src/main/java/com/example/next_contest/TracingActivity.kt`

- [ ] API 키 관리 방식 정리하기
  - Tmap, Kakao Maps, 날씨 API 키가 모두 로컬 설정 또는 안전한 빌드 설정에서 관리되도록 정리한다.
  - 관련 파일:
    - `local.properties`
    - `app/build.gradle.kts`
    - `app/src/main/java/com/example/next_contest/data/weather/WeatherRepository.kt`

- [ ] 문자열 리소스 분리하기
  - Kotlin/XML에 직접 들어간 한국어 문구를 `strings.xml`로 이동한다.
  - 관련 파일:
    - `app/src/main/res/values/strings.xml`
    - `app/src/main/java/com/example/next_contest/**/*.kt`
    - `app/src/main/res/layout/*.xml`

- [ ] 테스트 보강하기
  - 현재 기본 테스트를 실제 로직 테스트로 교체한다.
  - 우선순위:
    - Firebase snapshot을 모델로 변환하는 로직
    - 페어링 요청/수락/취소 상태 계산
    - 위치 거리 계산 및 방향 문구
    - 날씨 API 응답 파싱
  - 관련 파일:
    - `app/src/test/java/com/example/next_contest/ExampleUnitTest.kt`
    - `app/src/androidTest/java/com/example/next_contest/ExampleInstrumentedTest.kt`

- [ ] 백업/데이터 추출 정책 정리하기
  - 토큰, 위치 캐시, 사용자 민감 정보가 로컬에 저장될 경우 백업 제외 규칙을 추가한다.
  - 관련 파일:
    - `app/src/main/AndroidManifest.xml`
    - `app/src/main/res/xml/backup_rules.xml`
    - `app/src/main/res/xml/data_extraction_rules.xml`

- [ ] CompassHelper 구현 방식 개선하기
  - deprecated 된 `Sensor.TYPE_ORIENTATION` 사용 여부를 확인한다.
  - 가속도계/자이로 기반 회전 벡터 센서로 교체 가능한지 검토한다.
  - 관련 파일:
    - `app/src/main/java/com/example/next_contest/util/CompassHelper.kt`

## 완료된 작업

- [x] Firebase Auth 로그인/회원가입 연결
  - 이메일/비밀번호 회원가입 및 로그인 구현
  - 가입 완료 후 역할에 따라 어르신/보호자 메인 화면으로 이동

- [x] 회원가입 사용자 정보 저장
  - `users/{uid}`에 이름, 전화번호, 역할, 생성일 저장
  - 전화번호 검색용 인덱스 저장

- [x] 백엔드 구조를 Repository/Service로 분리
  - Auth, Pairing, Tracking 관련 데이터 접근과 비즈니스 로직 분리

- [x] 보호자-어르신 연결 요청 흐름 구현
  - 전화번호로 상대 사용자 찾기
  - 바로 연결하지 않고 요청/수락 방식으로 연결
  - 보호자 -> 어르신, 어르신 -> 보호자 방향 모두 허용
  - 요청 중 상태와 취소/거절/수락 처리 구현

- [x] 양방향 페어링 저장
  - 연결 수락 시 양쪽 사용자에 `pairedUid` 저장

- [x] 어르신 위치 공유 쓰기 구현
  - `/locations/{uid}`에 latitude, longitude, timestamp, isOnline, sos 저장
  - 어르신 화면 진입 시 위치 공유 시작

- [x] 보호자 실시간 위치 추적 읽기 구현
  - 연결된 어르신의 `/locations/{uid}`를 실시간 감시
  - 거리, 방향, 온라인 상태, 마지막 수신 시각 표시

- [x] 실시간 위치 확인 화면을 Kakao Maps와 연결
  - Google Maps 의존성 제거
  - Kakao Maps SDK v2 적용
  - 내 위치와 상대 위치를 지도 라벨로 표시
  - 두 위치 사이 polyline 표시

- [x] 전체 화면 디자인 정리
  - 로그인/회원가입/메인/설정/지도/추적/오늘 정보/도움 요청/안전구역 화면 톤 정리
  - 공통 색상, 배경, 카드, 입력창 drawable 추가
  - 깨진 한글 문구 정리

- [x] 빌드 확인
  - `.\gradlew.bat assembleDebug` 통과

## 최근 변경 파일 기록

- 생성:
  - `app/src/main/java/com/example/next_contest/NextContestApplication.kt`
  - `app/src/main/java/com/example/next_contest/controller/KakaoLocationMapController.kt`
  - `app/src/main/java/com/example/next_contest/controller/PairedLocationMapController.kt`
  - `app/src/main/java/com/example/next_contest/controller/PatientLocationShareController.kt`
  - `app/src/main/java/com/example/next_contest/data/auth/AuthRepository.kt`
  - `app/src/main/java/com/example/next_contest/data/pairing/PairingRepository.kt`
  - `app/src/main/java/com/example/next_contest/data/tracking/LocationSharingRepository.kt`
  - `app/src/main/java/com/example/next_contest/data/tracking/PairedLocationRepository.kt`
  - `app/src/main/java/com/example/next_contest/service/AuthService.kt`
  - `app/src/main/java/com/example/next_contest/service/PairingService.kt`
  - `app/src/main/res/drawable/bg_input.xml`
  - `app/src/main/res/drawable/bg_panel.xml`
  - `app/src/main/res/drawable/bg_primary_button.xml`
  - `app/src/main/res/drawable/bg_screen.xml`
  - `app/src/main/res/drawable/bg_status.xml`
  - `app/src/main/res/drawable/ic_map_marker_me.xml`
  - `app/src/main/res/drawable/ic_map_marker_other.xml`

- 수정:
  - `settings.gradle.kts`
  - `app/build.gradle.kts`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/com/example/next_contest/MainActivity.kt`
  - `app/src/main/java/com/example/next_contest/TracingActivity.kt`
  - `app/src/main/java/com/example/next_contest/controller/AuthController.kt`
  - `app/src/main/java/com/example/next_contest/controller/SimpleScreenController.kt`
  - `app/src/main/java/com/example/next_contest/controller/TracingUiController.kt`
  - `app/src/main/java/com/example/next_contest/controller/PairedLocationMapController.kt`
  - `app/src/main/res/layout/activity_user_auth.xml`
  - `app/src/main/res/layout/activity_signup.xml`
  - `app/src/main/res/layout/activity_login.xml`
  - `app/src/main/res/layout/activity_patient_main.xml`
  - `app/src/main/res/layout/activity_guardian_main.xml`
  - `app/src/main/res/layout/activity_settings.xml`
  - `app/src/main/res/layout/activity_map.xml`
  - `app/src/main/res/layout/activity_tracing.xml`
  - `app/src/main/res/layout/activity_daily_info.xml`
  - `app/src/main/res/layout/activity_home_navigation.xml`
  - `app/src/main/res/layout/activity_safe_zone.xml`
  - `app/src/main/res/layout/activity_seek_help.xml`
  - `app/src/main/res/values/colors.xml`
  - `app/src/main/res/values/themes.xml`

- 삭제:
  - `app/src/main/java/com/example/next_contest/controller/GoogleLocationMapController.kt`
  - `app/src/main/java/com/example/next_contest/view/LiveLocationMapView.kt`
