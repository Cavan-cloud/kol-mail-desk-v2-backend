#!/usr/bin/env bash
# Verify K8s Secret maildesk-secrets has required keys before/after deploy (P6-T13).
set -euo pipefail

NAMESPACE="${NAMESPACE:-maildesk}"
SECRET_NAME="${SECRET_NAME:-maildesk-secrets}"

REQUIRED_KEYS=(
  token-encryption-key
  db-password
  google-oauth-client-id
  google-oauth-client-secret
)

OPTIONAL_KEYS=(
  redis-password
  feishu-app-id
  feishu-app-secret
  feishu-kol-app-token
  feishu-kol-table-id
  moonshot-api-key
  deepseek-api-key
)

if ! command -v kubectl >/dev/null 2>&1; then
  echo "kubectl not found" >&2
  exit 1
fi

if ! kubectl get secret "$SECRET_NAME" -n "$NAMESPACE" >/dev/null 2>&1; then
  echo "Secret $NAMESPACE/$SECRET_NAME not found" >&2
  echo "If using External Secrets, check: kubectl get externalsecret -n $NAMESPACE" >&2
  exit 1
fi

FAILED=0
check_key() {
  local key="$1"
  local required="$2"
  if kubectl get secret "$SECRET_NAME" -n "$NAMESPACE" -o "jsonpath={.data.${key}}" 2>/dev/null | grep -q .; then
    echo "[OK]   $key"
  elif [[ "$required" == "required" ]]; then
    echo "[FAIL] missing required key: $key" >&2
    FAILED=1
  else
    echo "[WARN] optional key absent: $key"
  fi
}

echo "Checking $NAMESPACE/$SECRET_NAME ..."
for k in "${REQUIRED_KEYS[@]}"; do
  check_key "$k" required
done
for k in "${OPTIONAL_KEYS[@]}"; do
  check_key "$k" optional
done

# At least one AI provider key
HAS_AI=0
for k in moonshot-api-key deepseek-api-key; do
  if kubectl get secret "$SECRET_NAME" -n "$NAMESPACE" -o "jsonpath={.data.${k}}" 2>/dev/null | grep -q .; then
    HAS_AI=1
  fi
done
if [[ "$HAS_AI" -eq 0 ]]; then
  echo "[WARN] no AI API key (moonshot or deepseek) — AI features will fallback only" >&2
fi

if [[ "$FAILED" -ne 0 ]]; then
  exit 1
fi
echo "Secret keys OK."
