#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/zulu21.46.19-ca-jdk21.0.9-macosx_aarch64/zulu-21.jdk/Contents/Home}"
JAVA_BIN_DIR="${JAVA_HOME}/bin"
JAVAC_BIN="${JAVA_BIN_DIR}/javac"
JAR_BIN="${JAVA_BIN_DIR}/jar"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DB:-bit_iot}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_BIN="${MYSQL_BIN:-/usr/local/mysql/bin/mysql}"
RULE_SERVICE_BASE_URL="${RULE_SERVICE_BASE_URL:-http://127.0.0.1:9004/iot}"
FLINK_REST_URL="${FLINK_REST_URL:-http://127.0.0.1:8081}"
START_RULE_SERVICE="${START_RULE_SERVICE:-0}"
RULE_SERVICE_PROFILE="${RULE_SERVICE_PROFILE:-smoke}"
RULE_SERVICE_PID=""

RULE_ID="smoke-rule-001"
ALGORITHM_ID="smoke-algorithm-001"
RULE_NAME="Smoke 温度阈值规则"
ALGORITHM_NAME="Smoke 阈值算法"
ALGORITHM_CLASS="smoke.MockThresholdAlgorithm"

log() {
  printf '[smoke] %s\n' "$*"
}

log_err() {
  printf '[smoke] %s\n' "$*" >&2
}

extract_json_string_field() {
  local json="$1"
  local field="$2"
  printf '%s' "${json}" | sed -n "s/.*\"${field}\":\"\\([^\"]*\\)\".*/\\1/p"
}

extract_json_number_field() {
  local json="$1"
  local field="$2"
  printf '%s' "${json}" | sed -n "s/.*\"${field}\":\\([0-9][0-9]*\\).*/\\1/p"
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令: $1" >&2
    exit 1
  fi
}

cleanup() {
  if [[ -n "${RULE_SERVICE_PID}" ]]; then
    log "停止脚本启动的 rule-service (pid=${RULE_SERVICE_PID})"
    kill "${RULE_SERVICE_PID}" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

wait_http_ok() {
  local url="$1"
  local retry="${2:-60}"
  local sleep_seconds="${3:-2}"
  local i
  for ((i = 0; i < retry; i++)); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      return 0
    fi
    sleep "${sleep_seconds}"
  done
  return 1
}

mysql_exec() {
  if [[ -n "${MYSQL_PASSWORD}" ]]; then
    MYSQL_PWD="${MYSQL_PASSWORD}" "${MYSQL_BIN}" -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" "${MYSQL_DB}" "$@"
  else
    "${MYSQL_BIN}" -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" "${MYSQL_DB}" "$@"
  fi
}

wait_sql_rows() {
  local sql="$1"
  local retry="${2:-30}"
  local sleep_seconds="${3:-2}"
  local i
  for ((i = 0; i < retry; i++)); do
    local count
    count="$(mysql_exec -Nse "${sql}")"
    if [[ "${count}" =~ ^[0-9]+$ ]] && (( count > 0 )); then
      return 0
    fi
    sleep "${sleep_seconds}"
  done
  return 1
}

wait_sql_value() {
  local sql="$1"
  local retry="${2:-30}"
  local sleep_seconds="${3:-2}"
  local i
  for ((i = 0; i < retry; i++)); do
    local value
    value="$(mysql_exec -Nse "${sql}")"
    if [[ -n "${value}" ]]; then
      printf '%s' "${value}"
      return 0
    fi
    sleep "${sleep_seconds}"
  done
  return 1
}

ensure_column_exists() {
  local table_name="$1"
  local column_name="$2"
  local alter_sql="$3"
  local exists
  exists="$(mysql_exec -Nse "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${MYSQL_DB}' AND TABLE_NAME='${table_name}' AND COLUMN_NAME='${column_name}'")"
  if [[ "${exists}" == "0" ]]; then
    mysql_exec -e "${alter_sql}"
  fi
}

get_flink_job_state() {
  local job_id="$1"
  local response
  response="$(curl -fsS "${FLINK_REST_URL}/jobs/${job_id}" || true)"
  extract_json_string_field "${response}" "state"
}

wait_flink_terminal_status() {
  local job_id="$1"
  local retry="${2:-45}"
  local sleep_seconds="${3:-2}"
  local i
  for ((i = 0; i < retry; i++)); do
    local state
    state="$(get_flink_job_state "${job_id}")"
    if [[ -n "${state}" ]]; then
      log_err "Flink job ${job_id} 当前状态: ${state}"
    fi
    case "${state}" in
      FINISHED|FAILED|CANCELED|SUSPENDED)
        printf '%s' "${state}"
        return 0
        ;;
    esac
    sleep "${sleep_seconds}"
  done
  return 1
}

print_flink_job_diagnostics() {
  local job_id="$1"
  log_err "Flink job 诊断: ${job_id}"
  curl -fsS "${FLINK_REST_URL}/jobs/${job_id}" || true
  printf '\n' >&2
  curl -fsS "${FLINK_REST_URL}/jobs/${job_id}/exceptions" || true
  printf '\n' >&2
}

require_cmd curl
require_cmd mvn
require_cmd "${MYSQL_BIN}"
if [[ ! -x "${JAVAC_BIN}" ]]; then
  echo "找不到 JDK javac: ${JAVAC_BIN}" >&2
  exit 1
fi
if [[ ! -x "${JAR_BIN}" ]]; then
  echo "找不到 JDK jar: ${JAR_BIN}" >&2
  exit 1
fi

log "检查 Flink REST"
curl -fsS "${FLINK_REST_URL}/overview" >/dev/null

log "检查 MySQL"
mysql_exec -e "SELECT 1;" >/dev/null

if [[ "${START_RULE_SERVICE}" == "1" ]]; then
  log "启动 iot-rule-service (profile=${RULE_SERVICE_PROFILE})"
  (
    cd "${ROOT_DIR}"
    PATH="/Library/apache-maven-3.6.3/bin:${JAVA_BIN_DIR}:${PATH}" \
    mvn -q -pl iot-rule-service -am spring-boot:run -Dspring-boot.run.profiles="${RULE_SERVICE_PROFILE}"
  ) >"${ROOT_DIR}/scripts/smoke/rule-service-smoke.log" 2>&1 &
  RULE_SERVICE_PID=$!
fi

log "等待 rule-service 就绪"
if ! wait_http_ok "${RULE_SERVICE_BASE_URL}/rule-config/list?current=1&size=1" 90 2; then
  echo "rule-service 未就绪，请确认它已使用 smoke profile 启动" >&2
  exit 1
fi

log "编译 smoke 算法 JAR"
mkdir -p "${ROOT_DIR}/algorithms/upload" "${ROOT_DIR}/algorithms/shared" "${ROOT_DIR}/scripts/smoke/build/classes"
PATH="/Library/apache-maven-3.6.3/bin:${JAVA_BIN_DIR}:${PATH}" \
mvn -q -pl iot-common/common-flink -am clean compile -DskipTests
"${JAVAC_BIN}" \
  -cp "${ROOT_DIR}/iot-common/common-flink/target/classes" \
  -d "${ROOT_DIR}/scripts/smoke/build/classes" \
  "${ROOT_DIR}/scripts/smoke/MockThresholdAlgorithm.java"
"${JAR_BIN}" cf "${ROOT_DIR}/algorithms/shared/smoke-threshold-algorithm.jar" \
  -C "${ROOT_DIR}/scripts/smoke/build/classes" .
cp "${ROOT_DIR}/algorithms/shared/smoke-threshold-algorithm.jar" \
   "${ROOT_DIR}/algorithms/upload/smoke-threshold-algorithm.jar"

log "构建 Flink Job Fat JAR"
rm -f "${ROOT_DIR}/iot-flink-job/dependency-reduced-pom.xml"
PATH="/Library/apache-maven-3.6.3/bin:${JAVA_BIN_DIR}:${PATH}" \
mvn -q -pl iot-flink-job -am clean package -DskipTests

log "初始化数据库表"
mysql_exec < "${ROOT_DIR}/iot-rule-service/src/main/resources/sql/rule_engine.sql"
mysql_exec < "${ROOT_DIR}/iot-rule-service/src/main/resources/sql/alarm.sql"
ensure_column_exists "rule_execution_log" "window_key" \
  "ALTER TABLE rule_execution_log ADD COLUMN window_key VARCHAR(100) NULL AFTER rule_id"
mysql_exec <<SQL
DELETE FROM rule_param WHERE rule_id='${RULE_ID}';
DELETE FROM rule_data_source WHERE rule_id='${RULE_ID}';
DELETE FROM rule_config WHERE id='${RULE_ID}';
DELETE FROM rule_algorithm WHERE id='${ALGORITHM_ID}';
DELETE FROM rule_execution_log WHERE rule_id='${RULE_ID}';
DELETE FROM alarm_record WHERE rule_id='${RULE_ID}';
INSERT INTO rule_algorithm
(id, algorithm_name, algorithm_desc, algorithm_type, algorithm_path, algorithm_class, algorithm_version, algorithm_status, file_size, create_time, update_time)
VALUES
('${ALGORITHM_ID}', '${ALGORITHM_NAME}', 'Smoke 阈值算法', 'jar',
 '${ROOT_DIR}/algorithms/upload/smoke-threshold-algorithm.jar',
 '${ALGORITHM_CLASS}', '1.0.0', 1,
 $(wc -c < "${ROOT_DIR}/algorithms/upload/smoke-threshold-algorithm.jar"),
 NOW(), NOW());
INSERT INTO rule_config
(id, rule_name, rule_desc, algorithm_id, trigger_type, window_type, window_size, window_slide, window_unit, key_strategy, parallelism, rule_status, flink_job_id, create_time, update_time)
VALUES
('${RULE_ID}', '${RULE_NAME}', 'Smoke test rule for Flink alarm pipeline', '${ALGORITHM_ID}', 'periodic', 'tumbling', 1, NULL, 's', 'device_point', 1, 0, NULL, NOW(), NOW());
INSERT INTO rule_data_source
(id, rule_id, device_id, device_name, point_codes, time_range_start, time_range_end, create_time)
VALUES
('smoke-ds-001', '${RULE_ID}', 'smoke-device-001', 'Smoke 温度传感器', '["temp"]', NULL, NULL, NOW());
INSERT INTO rule_param (id, rule_id, param_key, param_value, param_desc) VALUES
('smoke-param-001', '${RULE_ID}', 'threshold', '70', '告警阈值'),
('smoke-param-002', '${RULE_ID}', 'alertLevel', 'error', '告警级别'),
('smoke-param-003', '${RULE_ID}', 'alertMessage', 'Smoke 温度超限', '告警内容'),
('smoke-param-004', '${RULE_ID}', '_mock_source_enabled', 'true', '启用 mock source'),
('smoke-param-005', '${RULE_ID}', '_mock_source_events_json', '$(tr -d '\n' < "${ROOT_DIR}/scripts/smoke/mock-events.json" | sed "s/'/''/g")', 'mock source 数据');
SQL

log "校验规则配置已写入 MySQL"
mysql_exec -e "SELECT id, rule_name, algorithm_id, trigger_type, window_type, window_size, window_unit, key_strategy, parallelism, rule_status, flink_job_id FROM rule_config WHERE id='${RULE_ID}';"
mysql_exec -e "SELECT rule_id, device_id, device_name, point_codes FROM rule_data_source WHERE rule_id='${RULE_ID}';"
mysql_exec -e "SELECT rule_id, param_key, LEFT(param_value, 120) AS param_value FROM rule_param WHERE rule_id='${RULE_ID}' ORDER BY param_key;"

log "启动规则"
curl -fsS -X PUT "${RULE_SERVICE_BASE_URL}/rule-config/${RULE_ID}/stop" >/dev/null 2>&1 || true
START_RESPONSE="$(curl -fsS -X PUT "${RULE_SERVICE_BASE_URL}/rule-config/${RULE_ID}/start")"
if [[ "${START_RESPONSE}" != *'"code":200'* ]]; then
  echo "规则启动失败: ${START_RESPONSE}" >&2
  exit 1
fi
log "启动接口返回: ${START_RESPONSE}"

log "等待 rule_config.flink_job_id 回写"
FLINK_JOB_ID="$(wait_sql_value "SELECT flink_job_id FROM rule_config WHERE id='${RULE_ID}' AND flink_job_id IS NOT NULL AND flink_job_id <> ''" 30 2 || true)"
if [[ -z "${FLINK_JOB_ID}" ]]; then
  echo "未在预期时间内拿到 flink_job_id" >&2
  exit 1
fi
log "规则已提交到 Flink, jobId=${FLINK_JOB_ID}"

log "查询 Flink 作业详情"
curl -fsS "${FLINK_REST_URL}/jobs/${FLINK_JOB_ID}" || true
printf '\n'

log "等待执行日志落库"
if ! wait_sql_rows "SELECT COUNT(1) FROM rule_execution_log WHERE rule_id='${RULE_ID}'" 30 2; then
  print_flink_job_diagnostics "${FLINK_JOB_ID}"
  echo "未在预期时间内查询到 rule_execution_log 记录" >&2
  exit 1
fi

log "等待告警落库"
if ! wait_sql_rows "SELECT COUNT(1) FROM alarm_record WHERE rule_id='${RULE_ID}'" 30 2; then
  print_flink_job_diagnostics "${FLINK_JOB_ID}"
  echo "未在预期时间内查询到 alarm_record 记录" >&2
  exit 1
fi

log "等待 Flink 进入终态"
FINAL_FLINK_STATUS="$(wait_flink_terminal_status "${FLINK_JOB_ID}" 30 2 || true)"
if [[ -z "${FINAL_FLINK_STATUS}" ]]; then
  log "未在等待窗口内观察到终态，继续输出当前结果"
else
  log "Flink job ${FLINK_JOB_ID} 终态: ${FINAL_FLINK_STATUS}"
fi

log "查询规则详情"
curl -fsS "${RULE_SERVICE_BASE_URL}/rule-config/${RULE_ID}" || true
printf '\n'

log "查询 Flink 状态"
curl -fsS "${RULE_SERVICE_BASE_URL}/rule-config/${RULE_ID}/flink-status" || true
printf '\n'

log "查询 rule_config 当前状态"
mysql_exec -e "SELECT id, rule_status, flink_job_id, update_time FROM rule_config WHERE id='${RULE_ID}';"

log "查询执行日志"
mysql_exec -e "SELECT rule_id, window_key, exec_status, start_time, end_time, duration_ms, LEFT(result_data, 200) AS result_data FROM rule_execution_log WHERE rule_id='${RULE_ID}' ORDER BY start_time DESC;"

log "查询告警记录"
mysql_exec -e "SELECT rule_id, device_id, point_code, alarm_level, alarm_status, trigger_count, last_trigger_time, metric_value, alarm_message FROM alarm_record WHERE rule_id='${RULE_ID}' ORDER BY last_trigger_time DESC;"

EXEC_LOG_COUNT="$(mysql_exec -Nse "SELECT COUNT(1) FROM rule_execution_log WHERE rule_id='${RULE_ID}'")"
ALARM_COUNT="$(mysql_exec -Nse "SELECT COUNT(1) FROM alarm_record WHERE rule_id='${RULE_ID}'")"

log "结果摘要"
printf '  ruleId=%s\n' "${RULE_ID}"
printf '  flinkJobId=%s\n' "${FLINK_JOB_ID}"
printf '  execLogCount=%s\n' "${EXEC_LOG_COUNT}"
printf '  alarmCount=%s\n' "${ALARM_COUNT}"
printf '  flinkFinalStatus=%s\n' "${FINAL_FLINK_STATUS:-UNKNOWN}"
printf '  result=%s\n' "PASS"

log "Smoke 流程执行完成"
