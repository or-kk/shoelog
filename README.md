# ShoeLog

갤럭시 워치와 Samsung Health가 Health Connect에 기록한 달리기를 읽어, 러닝화별 누적 주행거리를 관리하는 로컬 우선 Android 앱입니다.

ShoeLog는 운동 세션과 거리만 읽습니다. 경로, 심박수, 걸음 수처럼 러닝화 마일리지에 필요하지 않은 데이터는 요청하지 않으며 네트워크 권한도 사용하지 않습니다.

## 주요 기능

- Health Connect의 야외 달리기·트레드밀 운동과 거리 동기화
- 운동별 러닝화 배정 및 기본 러닝화 자동 배정
- 러닝화별 초기 거리, 누적 거리, 목표 거리, 남은 거리 표시
- 러닝화 등록·수정·은퇴 처리, 원화 구매가격·정가 관리, Android Photo Picker 기반 사진 선택
- 데일리·슈퍼 트레이너·레이싱 계열의 러닝화 분류와 다중 사용 목적 칩
- 하단 러닝화 관리 메뉴에서 활성·은퇴 목록 조회, 수정, 안전 삭제
- 달린 거리, 구매 날짜, 구매가격, 정가, 브랜드·모델, 최근 수정일 기준 정렬
- 미배정 운동 필터와 비공개 알림
- 30일 기본 동기화 및 기기 지원 시 전체 기록 권한 사용
- 가상의 러닝화와 운동만 사용하는 샘플 모드
- 라이트·다크 테마와 Material 3 UI

## 기술 구성

- Kotlin, Jetpack Compose, Material 3
- Room: 러닝화·운동·배정 데이터
- Preferences DataStore: 앱 설정
- Health Connect 1.1
- Navigation Compose
- JDK 17, Gradle 9.4.1, Android Gradle Plugin 9.2.1
- minSdk 28, targetSdk 36, compileSdk 36.1

앱은 별도 서버나 계정 없이 동작합니다. Health Connect 원본 식별자와 갱신 시각을 사용해 동기화를 멱등하게 처리하고, 원본에서 사라진 기록은 즉시 물리 삭제하지 않고 삭제 표시합니다.

Room 데이터베이스는 명시적인 마이그레이션을 사용합니다. 이전 버전에서 업데이트해도 기존 러닝화, 가격, 운동 기록, 러닝화 배정과 개인 설정을 초기화하지 않습니다.

## 시작하기

### 준비물

- Android Studio와 JDK 17
- Android SDK Platform 36.1
- Android 9(API 28) 이상 기기 또는 에뮬레이터
- 실제 Samsung Health 연동 검증 시 갤럭시 워치, Samsung Health, Health Connect

저장소를 복제한 뒤 Android SDK 경로를 로컬에만 설정합니다.

```bash
git clone git@github.com:or-kk/shoelog.git
cd shoelog
printf 'sdk.dir=/Users/you/Library/Android/sdk\n' > local.properties
./gradlew assembleDebug
```

`local.properties`는 Git에서 제외됩니다. Android Studio가 자동으로 생성하도록 두어도 됩니다.

생성된 디버그 APK는 `app/build/outputs/apk/debug/app-debug.apk`에 있습니다.

### 테스트

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew compileDebugAndroidTestKotlin
```

연결된 Android 기기가 있으면 계측 테스트도 실행할 수 있습니다.

```bash
./gradlew connectedDebugAndroidTest
```

## Samsung Health 연동

1. 갤럭시 워치에서 달리기 운동을 기록하고 Samsung Health 동기화를 완료합니다.
2. Samsung Health의 Health Connect 연결에서 운동과 거리 쓰기를 허용합니다.
3. ShoeLog의 `설정`에서 Health Connect 읽기 권한을 허용합니다.
4. `지금 동기화`를 누른 뒤 운동 목록에서 러닝화를 배정합니다.

Health Connect 또는 Samsung Health의 동기화 지연 때문에 새 운동이 바로 나타나지 않을 수 있습니다. ShoeLog는 포그라운드에서 사용자가 요청할 때 동기화하며, 백그라운드에서 건강 데이터를 주기적으로 읽지 않습니다.

## 요청 권한

| 권한 | 용도 |
| --- | --- |
| `READ_EXERCISE` | 달리기·트레드밀 세션의 시간과 출처 읽기 |
| `READ_DISTANCE` | 동일 출처·시간 범위의 주행거리 합계 읽기 |
| `READ_HEALTH_DATA_HISTORY` | 지원 기기에서 30일보다 오래된 기록 읽기 |
| `POST_NOTIFICATIONS` | 새 미배정 운동이 있을 때 내용이 드러나지 않는 알림 표시 |

인터넷, 위치, 연락처, 파일 전체 접근 권한은 요청하지 않습니다.

## 개인정보 보호

- 건강 기록, 러닝화, 사진 URI, 개인 설정은 기기 내부에만 저장합니다.
- Android 백업을 비활성화해 데이터베이스와 설정이 클라우드 백업에 포함되지 않게 했습니다.
- 앱에는 광고, 분석 SDK, 오류 수집 SDK, 자체 네트워크 통신이 없습니다.
- 알림은 잠금 화면에서 비공개이며 시간·거리 등 건강 내용을 표시하지 않습니다.
- 저장소의 샘플 데이터는 모두 명시적으로 만든 가상 값입니다.

상세 내용은 [개인정보 처리 원칙](docs/PRIVACY.md)을 참고하세요.

## 저장소 안전 기준

`.gitignore`는 다음 항목을 커밋 대상에서 제외합니다.

- `local.properties`
- `*.jks`, `*.keystore`, `*.p12`, 서명 설정 파일
- `health-data/`, `private-fixtures/`, 로컬 데이터베이스와 DataStore 파일
- APK, AAB, 빌드 결과와 IDE 개인 설정

공개 저장소에 실제 건강 기록이나 개인 설정을 테스트 픽스처로 추가하지 마세요.

## 실기기 검증

Samsung Health와 Health Connect의 실제 데이터 흐름은 제조사 앱과 사용자 권한에 의존하므로 갤럭시 실기기에서 최종 확인해야 합니다. 절차는 [실기기 테스트 체크리스트](docs/DEVICE_TEST_CHECKLIST.md)에 정리했습니다.

## 현재 범위

- 포그라운드 수동 동기화만 지원합니다.
- 달리기와 트레드밀 운동만 집계합니다.
- 거리 없는 세션은 목록에는 남지만 마일리지에는 합산하지 않습니다.
- 달리기 기록이 배정된 러닝화는 삭제할 수 없습니다. 먼저 은퇴 처리하거나 운동에서 러닝화 배정을 해제해야 하며, 기록이 없는 러닝화만 확인 후 영구 삭제됩니다.
- 사진은 선택된 로컬 URI만 저장하며 저장소에 복사하지 않습니다.

## 라이선스

Apache License 2.0. 자세한 내용은 [LICENSE](LICENSE)를 확인하세요.
