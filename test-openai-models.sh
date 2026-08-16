#!/usr/bin/env bash

set -u

SERVER="${1:-llms.am5.hergeth.cloud}"
PORT="${2:-443}"
DEFAULT_PROMPT="Reply with one short sentence: what is the capital of Switzerland?"
PROMPT="${3:-${DEFAULT_PROMPT}}"

BASE_URL="https://${SERVER}:${PORT}"
MODELS_URL="${BASE_URL}/v1/models"
CHAT_URL="${BASE_URL}/v1/chat/completions"

WARMUPS=3
MEASUREMENTS=10
MAX_TOKENS=1000

if [[ -z "${BIFROST_VKEY:-}" ]]; then
  echo "Error: BIFROST_VKEY is not set." >&2
  exit 1
fi

for command in curl jq date awk sort; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    echo "Error: ${command} is required." >&2
    exit 1
  fi
done

stats() {
  local label="$1"
  shift

  printf '%s' "${label}: "

  printf '%s\n' "$@" | awk '
    {
      values[NR] = $1
      sum += $1
    }
    END {
      n = NR

      if (n == 0) {
        print "no successful requests"
        exit
      }

      for (i = 1; i <= n; i++) {
        for (j = i + 1; j <= n; j++) {
          if (values[i] > values[j]) {
            tmp = values[i]
            values[i] = values[j]
            values[j] = tmp
          }
        }
      }

      mean = sum / n
      max = values[n]

      if (n % 2 == 1) {
        median = values[(n + 1) / 2]
      } else {
        median = (values[n / 2] + values[n / 2 + 1]) / 2
      }

      p90_index = int(0.90 * n)
      if (p90_index < 1) {
        p90_index = 1
      }
      if (p90_index < 0.90 * n) {
        p90_index++
      }

      p90 = values[p90_index]

      printf "mean=%.0f ms, max=%.0f ms", mean, max

      if (n > 1) {
        printf ", median=%.0f ms, P90=%.0f ms", median, p90
      }

      printf " (n=%d)\n", n
    }
  '
}

request() {
  local model="$1"
  local prompt="$2"

  REQUEST_BODY="$(
    jq -n \
      --arg model "${model}" \
      --arg prompt "${prompt}" \
      --argjson max_tokens "${MAX_TOKENS}" \
      '{
        model: $model,
        messages: [
          {
            role: "user",
            content: $prompt
          }
        ],
        temperature: 0.2,
        max_tokens: $max_tokens,
        stream: false
      }'
  )"

  REQUEST_START="$(date +%s%N)"

  if REQUEST_RESPONSE="$(
    curl \
      --fail-with-body \
      --silent \
      --show-error \
      --connect-timeout 10 \
      --max-time 300 \
      -H "Content-Type: application/json" \
      -H "x-bf-vk: ${BIFROST_VKEY}" \
      "${CHAT_URL}" \
      --data "${REQUEST_BODY}"
  )"; then
    REQUEST_END="$(date +%s%N)"
    REQUEST_TIME_MS="$(( (REQUEST_END - REQUEST_START) / 1000000 ))"
    return 0
  fi

  REQUEST_END="$(date +%s%N)"
  REQUEST_TIME_MS="$(( (REQUEST_END - REQUEST_START) / 1000000 ))"
  return 1
}

echo "Fetching models from ${MODELS_URL}..."

if ! models_json="$(
  curl \
    --fail-with-body \
    --silent \
    --show-error \
    --connect-timeout 10 \
    --max-time 30 \
    -H "x-bf-vk: ${BIFROST_VKEY}" \
    "${MODELS_URL}"
)"; then
  echo "Error: could not fetch models." >&2
  exit 1
fi

if ! jq empty <<<"${models_json}" >/dev/null 2>&1; then
  echo "Error: models endpoint did not return valid JSON." >&2
  exit 1
fi

mapfile -t models < <(
  jq -r '.data[]?.id // empty' <<<"${models_json}"
)

if ((${#models[@]} == 0)); then
  echo "No models found."
  exit 1
fi

echo
echo "Prompt: ${PROMPT}"

for model in "${models[@]}"; do
  warmup_times=()
  measurement_times=()
  completion_tokens=()
  total_tokens=()
  successful_measurements=0

  echo
  echo "========================================"
  echo "Model: ${model}"
  echo "========================================"
  echo "Running ${WARMUPS} warmup requests..."

  for ((i = 1; i <= WARMUPS; i++)); do
    if request "${model}" "${PROMPT}"; then
      warmup_times+=("${REQUEST_TIME_MS}")
    else
      echo "Warmup ${i} failed."
    fi
  done

  echo "Running ${MEASUREMENTS} measurements..."

  for ((i = 1; i <= MEASUREMENTS; i++)); do
    if ! request "${model}" "${PROMPT}"; then
      echo "Measurement ${i} failed."
      continue
    fi

    if ! jq empty <<<"${REQUEST_RESPONSE}" >/dev/null 2>&1; then
      echo "Measurement ${i}: invalid JSON response."
      continue
    fi

    if jq -e '(.error // null) != null' <<<"${REQUEST_RESPONSE}" \
      >/dev/null 2>&1; then
      echo "Measurement ${i}: model returned an error."
      jq -r '.error.message // .error' <<<"${REQUEST_RESPONSE}"
      continue
    fi

    measurement_times+=("${REQUEST_TIME_MS}")
    successful_measurements=$((successful_measurements + 1))

    tokens="$(
      jq -r '
        .usage.completion_tokens //
        .usage.output_tokens //
        "unknown"
      ' <<<"${REQUEST_RESPONSE}"
    )"

    total="$(
      jq -r '
        .usage.total_tokens //
        "unknown"
      ' <<<"${REQUEST_RESPONSE}"
    )"

    completion_tokens+=("${tokens}")
    total_tokens+=("${total}")
  done

  echo
  echo "Timing statistics:"
  stats "Warmup" "${warmup_times[@]}"
  stats "Measurements" "${measurement_times[@]}"

  echo
  echo "Successful measurements: ${successful_measurements}/${MEASUREMENTS}"

  if ((${#completion_tokens[@]} > 0)); then
    echo
    echo "Token usage per measurement:"
    printf '%-12s | %-16s | %-12s\n' \
      "Measurement" "Completion tokens" "Total tokens"
    printf '%s\n' \
      "-------------+------------------+-------------"

    for ((i = 0; i < ${#completion_tokens[@]}; i++)); do
      printf '%-12s | %-16s | %-12s\n' \
        "$((i + 1))" \
        "${completion_tokens[$i]}" \
        "${total_tokens[$i]}"
    done
  fi
done
