{{/*
Expand the name of the chart.
*/}}
{{- define "book-network.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "book-network.fullname" -}}
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

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "book-network.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "book-network.labels" -}}
helm.sh/chart: {{ include "book-network.chart" . }}
{{ include "book-network.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: book-network-platform
{{- end }}

{{/*
Selector labels
*/}}
{{- define "book-network.selectorLabels" -}}
app.kubernetes.io/name: {{ include "book-network.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "book-network.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "book-network.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Database connection string
*/}}
{{- define "book-network.databaseUrl" -}}
{{- if .Values.postgresql.enabled }}
{{- printf "jdbc:postgresql://%s-postgresql:5432/%s" (include "book-network.fullname" .) .Values.postgresql.auth.database }}
{{- else }}
{{- .Values.externalDatabase.url }}
{{- end }}
{{- end }}

{{/*
Redis connection details
*/}}
{{- define "book-network.redisHost" -}}
{{- if .Values.redis.enabled }}
{{- printf "%s-redis-master" (include "book-network.fullname" .) }}
{{- else }}
{{- .Values.externalRedis.host }}
{{- end }}
{{- end }}

{{/*
Redis port
*/}}
{{- define "book-network.redisPort" -}}
{{- if .Values.redis.enabled }}
{{- "6379" }}
{{- else }}
{{- .Values.externalRedis.port | toString }}
{{- end }}
{{- end }}

{{/*
Monitoring labels
*/}}
{{- define "book-network.monitoringLabels" -}}
monitoring: "true"
prometheus.io/scrape: "true"
prometheus.io/port: "8080"
prometheus.io/path: "/api/v1/actuator/prometheus"
{{- end }}

{{/*
Security labels
*/}}
{{- define "book-network.securityLabels" -}}
security.kubernetes.io/pod-security-policy: restricted
app.kubernetes.io/security-context: non-root
{{- end }}

{{/*
Environment specific labels
*/}}
{{- define "book-network.environmentLabels" -}}
app.kubernetes.io/environment: {{ .Values.global.environment | default "production" }}
app.kubernetes.io/tier: backend
app.kubernetes.io/component: application
{{- end }}

{{/*
Resource annotations
*/}}
{{- define "book-network.resourceAnnotations" -}}
deployment.kubernetes.io/revision: "1"
kubectl.kubernetes.io/last-applied-configuration: |
  {{ toJson . | nindent 2 }}
{{- end }}

{{/*
Common pod annotations
*/}}
{{- define "book-network.podAnnotations" -}}
checksum/config: {{ include (print $.Template.BasePath "/configmap.yaml") . | sha256sum }}
checksum/secret: {{ include (print $.Template.BasePath "/secret.yaml") . | sha256sum }}
prometheus.io/scrape: "true"
prometheus.io/port: "8080"
prometheus.io/path: "/api/v1/actuator/prometheus"
{{- if .Values.monitoring.jaeger.enabled }}
sidecar.jaegertracing.io/inject: "true"
{{- end }}
{{- end }}

{{/*
Validate required values
*/}}
{{- define "book-network.validateValues" -}}
{{- if not .Values.image.repository }}
{{- fail "image.repository is required" }}
{{- end }}
{{- if not .Values.image.tag }}
{{- fail "image.tag is required" }}
{{- end }}
{{- if and .Values.postgresql.enabled (not .Values.postgresql.auth.password) }}
{{- fail "postgresql.auth.password is required when postgresql.enabled is true" }}
{{- end }}
{{- if and .Values.redis.enabled (not .Values.redis.auth.password) }}
{{- fail "redis.auth.password is required when redis.enabled is true" }}
{{- end }}
{{- end }}

{{/*
Image pull policy validation
*/}}
{{- define "book-network.imagePullPolicy" -}}
{{- if eq .Values.image.tag "latest" }}
{{- "Always" }}
{{- else }}
{{- .Values.image.pullPolicy | default "IfNotPresent" }}
{{- end }}
{{- end }}

{{/*
Generate certificate name
*/}}
{{- define "book-network.certificateName" -}}
{{- if .Values.ingress.tls }}
{{- range .Values.ingress.tls }}
{{- .secretName }}
{{- end }}
{{- else }}
{{- printf "%s-tls" (include "book-network.fullname" .) }}
{{- end }}
{{- end }}

{{/*
Generate ingress class name
*/}}
{{- define "book-network.ingressClassName" -}}
{{- if .Values.ingress.className }}
{{- .Values.ingress.className }}
{{- else }}
{{- "nginx" }}
{{- end }}
{{- end }}

{{/*
Generate storage class name
*/}}
{{- define "book-network.storageClassName" -}}
{{- if .Values.global.storageClass }}
{{- .Values.global.storageClass }}
{{- else if .Values.persistence.storageClass }}
{{- .Values.persistence.storageClass }}
{{- else }}
{{- "default" }}
{{- end }}
{{- end }}

{{/*
Generate backup storage configuration
*/}}
{{- define "book-network.backupStorageConfig" -}}
{{- if .Values.backup.enabled }}
type: {{ .Values.backup.storage.type }}
{{- if eq .Values.backup.storage.type "s3" }}
bucket: {{ .Values.backup.storage.bucket }}
region: {{ .Values.backup.storage.region }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Network policy configuration
*/}}
{{- define "book-network.networkPolicyConfig" -}}
{{- if .Values.networkPolicy.enabled }}
policyTypes:
{{- range .Values.networkPolicy.policyTypes }}
  - {{ . }}
{{- end }}
{{- end }}
{{- end }}