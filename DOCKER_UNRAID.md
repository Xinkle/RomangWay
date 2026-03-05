# Docker / Unraid 배포 가이드

## 1) 이미지 빌드 (로컬/CI)

이 프로젝트는 Gradle 빌드 중 FFLogs GraphQL introspection이 실행되므로, **이미지 빌드 시점**에 FFLogs Client ID/Secret이 필요합니다.

```bash
docker build \
  --build-arg FFLOGS_CLIENT_ID=YOUR_ID \
  --build-arg FFLOGS_CLIENT_SECRET=YOUR_SECRET \
  -t yourrepo/romangway:latest .
```

빌드 후 레지스트리에 푸시:

```bash
docker push yourrepo/romangway:latest
```

## 2) 런타임 설정 방식 (권장 2가지)

### 방식 A. `secret_profile.properties` 파일 마운트 (기존 방식 유지)

컨테이너 기본 설정 경로:

- 컨테이너 내부: `/config/secret_profile.properties`

Unraid에서는 예를 들어 아래 경로에 파일을 두고 마운트합니다.

- 호스트: `/mnt/user/appdata/romangway/secret_profile.properties`
- 컨테이너: `/config/secret_profile.properties`

### 방식 B. 환경변수 사용 (Unraid Docker 탭에 적합)

`Prop.kt`가 환경변수를 우선 지원하도록 확장되어 아래 키로 설정 가능합니다.

- `DISCORD_BOT_TOKEN`
- `FFLOGS_CLIENT_ID` (또는 `FFLOG_CLIENT_ID`)
- `FFLOGS_CLIENT_SECRET` (또는 `FFLOG_CLIENT_SECRET`)
- `CHROMEDRIVER` (tar/아이템검색 Selenium 연결용, 예: `http://100.87.250.109:3001/wd/hub`)
- `DATABASE`
- `DB_ID` (또는 `DATABASE_ID`)
- `DB_PW` (또는 `DATABASE_PW`)
- `OPEN_AI_KEY` (또는 `OPENAI_API_KEY`)

## 3) Unraid Docker 탭 설정 예시

### 필수 값

- `Repository`: `yourrepo/romangway:latest`
- `Network Type`: 브리지(기본) 또는 사용자 네트워크
- `Restart`: `unless-stopped`

### Path (파일 방식 사용 시)

- `Host Path 1`: `/mnt/user/appdata/romangway`
- `Container Path 1`: `/config`

### Variables (환경변수 방식 사용 시)

- `DISCORD_BOT_TOKEN`
- `FFLOGS_CLIENT_ID`
- `FFLOGS_CLIENT_SECRET`
- `CHROMEDRIVER` (tar/아이템검색 Selenium용, 예: `http://100.87.250.109:3001/wd/hub`)
- `DATABASE` (예: `mariadb:3306/romangway`)
- `DB_ID`
- `DB_PW`
- `OPEN_AI_KEY`
- `JAVA_TOOL_OPTIONS` (선택, 예: `-Xms128m -Xmx256m -XX:MaxDirectMemorySize=96m`)
- `ROMANGWAY_JAVA_MEMORY_OPTS` (선택, `JAVA_TOOL_OPTIONS` 미지정 시에만 적용)
- `LOG_DIR` (선택, 기본: `/app/logs`)

## 4) 주의사항

- 이 앱은 **MariaDB**에 연결하므로 DB 컨테이너/외부 DB가 먼저 준비되어 있어야 합니다.
- `chromedriver`는 TAR/아이템검색 클라이언트가 사용하는 Selenium 원격 WebDriver 엔드포인트입니다.
- 기본적으로 컨테이너는 JVM 메모리 제한을 강제하지 않습니다.
- 필요 시 `JAVA_TOOL_OPTIONS` 또는 `ROMANGWAY_JAVA_MEMORY_OPTS`로 JVM 메모리 옵션을 명시적으로 설정할 수 있습니다.
- 로그는 콘솔과 파일에 동시에 기록되며, 파일은 `${LOG_DIR}` 경로에 일 단위(`romangway.YYYY-MM-DD.log`)로 롤링됩니다.
- Unraid Docker 탭은 보통 “이미지 실행” 중심이므로, **이미지는 미리 빌드/푸시**해서 사용하는 흐름이 가장 안정적입니다.
