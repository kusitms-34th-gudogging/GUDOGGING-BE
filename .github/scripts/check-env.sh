#!/usr/bin/env bash
# 배포 전 필수 환경변수 누락 검사.
# 사용법: check-env.sh <env 파일> <설정 파일...>
#   설정 파일(application yaml, docker compose)에서 기본값 없는 ${VAR} 를 찾아
#   env 파일에 값이 있는지 확인한다. 값은 출력하지 않고 누락된 변수 이름만 출력.
set -euo pipefail

env_file="$1"; shift
# 한 번만 읽음 (파이프·프로세스 치환으로 넘겨도 동작하도록)
env_content=$(cat "$env_file")

required=$(grep -hoE '\$\{[A-Z_][A-Z0-9_]*\}' "$@" | sed -E 's/^\$\{(.*)\}$/\1/' | sort -u)

missing=()
for var in $required; do
  value=$(printf '%s\n' "$env_content" | grep -E "^${var}=" | tail -n 1 | cut -d= -f2- || true)
  [ -z "$value" ] && missing+=("$var")
done

if [ ${#missing[@]} -gt 0 ]; then
  echo "::error::환경변수 누락: ${missing[*]} (GitHub Secret 의 .env 내용에 추가 필요)"
  exit 1
fi

echo "필수 환경변수 확인 완료: $(echo $required | wc -w | tr -d ' ')개"
