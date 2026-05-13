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

- [ ] 도움 요청 화면 버튼 동작 구현하기
  - `btnCallCaregiver`: 연결된 보호자 전화 걸기 또는 전화 앱 열기
  - `btnCallPolice`: 112 전화 앱 열기
  - `btnSendLocation`: 현재 위치/SOS 상태 전송

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
