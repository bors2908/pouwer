#!/bin/sh
set -e

# Start bitcoind in background
bitcoind \
  -regtest=1 \
  -server=1 \
  -rpcbind=0.0.0.0 \
  -rpcallowip=0.0.0.0/0 \
  -rpcuser=rpcuser \
  -rpcpassword=rpcpassword \
  -fallbackfee=0.0002 \
  -printtoconsole &

BITCOIND_PID=$!

# Wait for RPC
echo "Waiting for bitcoind..."
until bitcoin-cli -regtest -rpcuser=rpcuser -rpcpassword=rpcpassword getblockchaininfo >/dev/null 2>&1
do
  sleep 1
done

echo "Bitcoin RPC ready"

# Create wallet if not exists
bitcoin-cli -regtest -rpcuser=rpcuser -rpcpassword=rpcpassword listwallets | grep -q poc || \
  bitcoin-cli -regtest -rpcuser=rpcuser -rpcpassword=rpcpassword createwallet "poc"

# Check block height
HEIGHT=$(bitcoin-cli -regtest -rpcuser=rpcuser -rpcpassword=rpcpassword getblockcount)

if [ "$HEIGHT" -lt 101 ]; then
  echo "Mining initial 101 blocks..."
  ADDR=$(bitcoin-cli -regtest -rpcuser=rpcuser -rpcpassword=rpcpassword -rpcwallet=poc getnewaddress)
  bitcoin-cli -regtest -rpcuser=rpcuser -rpcpassword=rpcpassword -rpcwallet=poc generatetoaddress 101 $ADDR
fi

echo "Regtest bootstrap complete"

wait $BITCOIND_PID
