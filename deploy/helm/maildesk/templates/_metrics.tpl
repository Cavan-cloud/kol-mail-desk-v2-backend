{{- define "maildesk.prometheusPodAnnotations" -}}
{{- if .Values.metrics.prometheusScrape }}
prometheus.io/scrape: "true"
prometheus.io/path: "/actuator/prometheus"
{{- end }}
{{- end }}

{{- define "maildesk.api.prometheusPodAnnotations" -}}
{{- if .Values.metrics.prometheusScrape }}
prometheus.io/scrape: "true"
prometheus.io/port: {{ .Values.api.service.port | quote }}
prometheus.io/path: "/actuator/prometheus"
{{- end }}
{{- end }}

{{- define "maildesk.worker.prometheusPodAnnotations" -}}
{{- if .Values.metrics.prometheusScrape }}
prometheus.io/scrape: "true"
prometheus.io/port: {{ .Values.worker.managementPort | quote }}
prometheus.io/path: "/actuator/prometheus"
{{- end }}
{{- end }}
