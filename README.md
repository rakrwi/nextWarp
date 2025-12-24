# NextWarp

마인크래프트 멀티 서버를 위한 크로스 서버 텔레포트 플러그인입니다.

## 기능

### 워프 시스템
- `/warp <이름>` - 저장된 워프 위치로 텔레포트
- `/setwarp <이름>` - 현재 위치를 워프로 저장
- `/delwarp <이름>` - 워프 삭제
- `/warps` - 모든 워프 목록 확인

### 스폰 시스템
- `/spawn` - 글로벌 스폰으로 텔레포트
- `/setspawn` - 현재 위치를 스폰으로 설정

### TPA 시스템
- `/tpa <플레이어>` - 다른 플레이어에게 텔레포트 요청
- `/tpahere <플레이어>` - 다른 플레이어를 자신에게 텔레포트 요청
- `/tpaccept` - 텔레포트 요청 수락
- `/tpdeny` - 텔레포트 요청 거절

### 랜덤 텔레포트
- `/randomtp` - 무작위 안전한 위치로 텔레포트

## 핵심 특징

- **크로스 서버 지원**: Velocity 프록시와 Redis를 통해 서로 다른 서버 간 텔레포트 가능
- **쿨다운 시스템**: 모든 명령어에 개별 쿨다운 설정 가능
- **안전한 랜덤 TP**: 설정된 안전 블록 위에서만 스폰되도록 보장
- **개발자 API**: 다른 플러그인에서 NextWarp 기능 활용 가능

## 요구 사항

- **Paper 서버**: 1.21+ (Java 21)
- **Velocity 프록시**: 3.0+
- **MySQL**: 데이터 저장용
- **Redis**: 서버 간 통신용

## 설치

1. `NextWarp.jar`를 각 Paper 서버의 `plugins` 폴더에 복사
2. Velocity 서버의 `plugins` 폴더에도 복사
3. 서버 시작 후 생성된 `config.yml` 설정
4. MySQL과 Redis 연결 정보 입력
5. 서버 재시작

## 설정

### config.yml (Paper)
```yaml
server-name: "lobby"  # 이 서버의 이름

mysql:
  host: "localhost"
  port: 3306
  database: "nextwarp"
  username: "root"
  password: ""

redis:
  host: "localhost"
  port: 6379
  password: ""

random-tp:
  min-x: -5000
  max-x: 5000
  min-z: -5000
  max-z: 5000
  default-world: "world"
  safe-blocks:
    - GRASS_BLOCK
    - DIRT
    - STONE
    # ...

tpa:
  enabled: true
  expire-seconds: 60

cooldown:
  warp:
    enabled: true
    seconds: 5
  random-tp:
    enabled: true
    seconds: 30
  tpa:
    enabled: true
    seconds: 10
  spawn:
    enabled: true
    seconds: 3
```

## 권한

| 권한 | 설명 |
|------|------|
| `nextwarp.admin` | 모든 권한 |
| `nextwarp.warp.use` | 워프 사용 |
| `nextwarp.warp.set` | 워프 설정 |
| `nextwarp.warp.delete` | 워프 삭제 |
| `nextwarp.spawn.use` | 스폰 사용 |
| `nextwarp.spawn.set` | 스폰 설정 |
| `nextwarp.tpa.use` | TPA 사용 |
| `nextwarp.randomtp.use` | 랜덤 텔레포트 사용 |
| `nextwarp.cooldown.bypass.*` | 쿨다운 우회 |

## 프로젝트 구조

```
nextWarp/
├── api/          # 외부 플러그인용 API
├── common/       # 공통 모듈 (설정, 데이터, DB, Redis)
├── paper/        # Paper 서버 플러그인
└── velocity/     # Velocity 프록시 플러그인
```

## 기술 스택

- **언어**: Kotlin 2.0
- **서버 API**: Paper API, Velocity API
- **데이터베이스**: MySQL + HikariCP
- **메시징**: Redis + Jedis
- **빌드**: Gradle + Shadow Plugin

## 빌드

```bash
./gradlew build
```

빌드된 JAR 파일은 `build/libs/NextWarp.jar`에 생성됩니다.

## 라이선스

이 프로젝트는 개인 사용 목적으로 제작되었습니다.

## 개발자

- **rakrwi** (dev.rakrwi)
