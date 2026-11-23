{{- define "your-repo-name.backupsEnabled" -}}
{{- if or (not (kindIs "bool" .Values.backups.enabled)) (not (kindIs "bool" .Values.backups.creationEnabled)) -}}
{{- fail "backup enabled and creationEnabled settings must be boolean values" -}}
{{- end -}}
{{- if .Values.backups.enabled -}}
{{- if not (semverCompare ">=1.27-0" .Capabilities.KubeVersion.Version) -}}
{{- fail "backups require Kubernetes 1.27 or newer for CronJob timeZone" -}}
{{- end -}}
{{- if ne .Release.Name "laa-info-and-advice-datastore" -}}
{{- fail "backups may only be enabled for the canonical release" -}}
{{- end -}}
{{- if not (has .Values.backups.environment (list "uat" "staging" "prod")) -}}
{{- fail "backups require a supported environment" -}}
{{- end -}}
{{- if ne .Release.Namespace (printf "laa-info-and-advice-datastore-%s" .Values.backups.environment) -}}
{{- fail "backups require the canonical namespace for their environment" -}}
{{- end -}}
{{- if and (eq .Values.backups.environment "prod") (not .Values.backups.creationEnabled) -}}
{{- fail "backup prune-only is not allowed in prod" -}}
{{- end -}}
{{- if or (empty .Values.backups.serviceAccountName) (eq .Values.backups.serviceAccountName .Values.serviceAccount.name) -}}
{{- fail "backups require a dedicated service account, not the application account" -}}
{{- end -}}
true
{{- end -}}
{{- end -}}
