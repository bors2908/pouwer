FROM alpine:3.20

RUN apk add --no-cache ca-certificates curl

WORKDIR /work

COPY pouwer-core/browser/widget-traefik/pages/ ./pages/
COPY examples/crowdsec-traefik-integration/plugin-sync-container/plugin-sync.sh /usr/local/bin/plugin-sync.sh
RUN sed -i 's/\r$//' /usr/local/bin/plugin-sync.sh && chmod +x /usr/local/bin/plugin-sync.sh

ENTRYPOINT ["/bin/sh", "/usr/local/bin/plugin-sync.sh"]
