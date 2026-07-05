#!/usr/bin/env bash
# CI guard: no plaintext production secrets committed under deploy/ (P6-T13).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
FAILED=0

echo "==> values.yaml must keep secrets.create=false"
if awk '/^secrets:/{p=1} p&&/create:/{print; exit}' "$ROOT/deploy/helm/maildesk/values.yaml" | grep -q 'create: true'; then
  echo "[FAIL] deploy/helm/maildesk/values.yaml has secrets.create=true" >&2
  FAILED=1
else
  echo "[OK]   secrets.create=false in values.yaml"
fi

echo "==> scan deploy/ for leaked API key patterns (exclude *.example.*)"
while IFS= read -r -d '' file; do
  if grep -qE 'sk-[a-zA-Z0-9]{10,}|ya29\.[a-zA-Z0-9_-]{20,}' "$file" 2>/dev/null; then
    echo "[FAIL] possible secret in $file" >&2
    FAILED=1
  fi
done < <(find "$ROOT/deploy" -type f \( -name '*.yaml' -o -name '*.yml' -o -name '*.json' \) \
  ! -name '*.example.*' ! -name 'values-local.example.yaml' ! -name 'values-prod.example.yaml' -print0 2>/dev/null)

if [[ "$FAILED" -eq 0 ]]; then
  echo "[OK]   no obvious secret patterns under deploy/"
fi

exit "$FAILED"
