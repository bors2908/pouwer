{{- define "pouwer.hostOrRule" -}}
{{- $parts := list -}}
{{- range . -}}
{{- $parts = append $parts (printf "Host(`%s`)" .) -}}
{{- end -}}
{{- join " || " $parts -}}
{{- end -}}
