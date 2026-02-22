FROM ruimarinho/bitcoin-core:23

COPY bitcoin-regtest-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
