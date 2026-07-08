{{/*
Expand the name of the chart.
*/}}
{{- define "maildesk.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "maildesk.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{- define "maildesk.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "maildesk.labels" -}}
helm.sh/chart: {{ include "maildesk.chart" . }}
{{ include "maildesk.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "maildesk.selectorLabels" -}}
app.kubernetes.io/name: {{ include "maildesk.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "maildesk.api.selectorLabels" -}}
{{ include "maildesk.selectorLabels" . }}
app.kubernetes.io/component: api
{{- end }}

{{- define "maildesk.worker.selectorLabels" -}}
{{ include "maildesk.selectorLabels" . }}
app.kubernetes.io/component: worker
{{- end }}

{{- define "maildesk.web.selectorLabels" -}}
{{ include "maildesk.selectorLabels" . }}
app.kubernetes.io/component: web
{{- end }}

{{- define "maildesk.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "maildesk.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{- define "maildesk.secretName" -}}
{{- .Values.secrets.name }}
{{- end }}

{{- define "maildesk.secretStoreName" -}}
{{- default (printf "%s-secret-store" (include "maildesk.fullname" .)) .Values.externalSecrets.secretStore.name }}
{{- end }}

{{/*
Non-secret Feishu sync env vars for API and Worker.
*/}}
{{- define "maildesk.feishuEnv" -}}
- name: FEISHU_SYNC_SOURCE
  value: {{ .Values.config.feishuSyncSource | quote }}
- name: FEISHU_TAB_FILTER
  value: {{ .Values.config.feishuTabFilter | quote }}
- name: FEISHU_REGIONAL_TAB_NAMES
  value: {{ .Values.config.feishuRegionalTabNames | quote }}
- name: FEISHU_SHEET_RECENT_MONTHS
  value: {{ .Values.config.feishuSheetRecentMonths | quote }}
- name: FEISHU_FULL_SYNC
  value: {{ .Values.config.feishuFullSync | quote }}
{{- end }}

{{/*
Shared secret-backed env vars for API and Worker.
*/}}
{{- define "maildesk.secretEnv" -}}
- name: TOKEN_ENCRYPTION_KEY
  valueFrom:
    secretKeyRef:
      name: {{ include "maildesk.secretName" . }}
      key: token-encryption-key
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Values.database.existingSecret }}
      key: {{ .Values.database.passwordKey }}
- name: REDIS_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Values.redis.existingSecret }}
      key: {{ .Values.redis.passwordKey }}
      optional: true
- name: GOOGLE_OAUTH_CLIENT_ID
  valueFrom:
    secretKeyRef:
      name: {{ include "maildesk.secretName" . }}
      key: google-oauth-client-id
- name: GOOGLE_OAUTH_CLIENT_SECRET
  valueFrom:
    secretKeyRef:
      name: {{ include "maildesk.secretName" . }}
      key: google-oauth-client-secret
- name: FEISHU_APP_ID
  valueFrom:
    secretKeyRef:
      name: {{ include "maildesk.secretName" . }}
      key: feishu-app-id
      optional: true
- name: FEISHU_APP_SECRET
  valueFrom:
    secretKeyRef:
      name: {{ include "maildesk.secretName" . }}
      key: feishu-app-secret
      optional: true
- name: FEISHU_KOL_APP_TOKEN
  valueFrom:
    secretKeyRef:
      name: {{ include "maildesk.secretName" . }}
      key: feishu-kol-app-token
      optional: true
- name: FEISHU_KOL_TABLE_ID
  valueFrom:
    secretKeyRef:
      name: {{ include "maildesk.secretName" . }}
      key: feishu-kol-table-id
      optional: true
- name: MOONSHOT_API_KEY
  valueFrom:
    secretKeyRef:
      name: {{ include "maildesk.secretName" . }}
      key: moonshot-api-key
      optional: true
- name: DEEPSEEK_API_KEY
  valueFrom:
    secretKeyRef:
      name: {{ include "maildesk.secretName" . }}
      key: deepseek-api-key
      optional: true
{{- end }}
