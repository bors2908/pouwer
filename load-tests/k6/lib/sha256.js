/**
 * SHA-256 PoW solver for k6.
 *
 * The SHA-256 plugin challenge payload contains:
 *   - dataHex:     hex-encoded block template (nonce bytes are at nonceOffset)
 *   - nonceOffset: byte offset where the 4-byte nonce is written
 *   - nonceIsLE:   true → little-endian nonce, false → big-endian
 *   - targetHex:   hex-encoded 32-byte target (hash must be ≤ target)
 *   - nonceRange:  { min, max } inclusive nonce search range
 *
 * Returns { nonce, hashHex } or throws if no solution found in range.
 */
export async function solveSha256(payload) {
  const data = hexToBytes(payload.dataHex);
  const target = hexToBytes(payload.targetHex);
  const { min, max } = payload.nonceRange;
  const offset = payload.nonceOffset;
  const le = payload.nonceIsLE;

  for (let nonce = min; nonce <= max; nonce++) {
    writeNonce(data, offset, nonce, le);
    const hashBuf = await crypto.subtle.digest('SHA-256', data.buffer);
    const hash = new Uint8Array(hashBuf);
    if (compareLE(hash, target) <= 0) {
      return { nonce, hashHex: bytesToHex(hash) };
    }
  }
  throw new Error(`SHA-256: no solution found in range [${min}, ${max}]`);
}

function hexToBytes(hex) {
  const bytes = new Uint8Array(hex.length / 2);
  for (let i = 0; i < bytes.length; i++) {
    bytes[i] = parseInt(hex.slice(i * 2, i * 2 + 2), 16);
  }
  return bytes;
}

function bytesToHex(bytes) {
  return Array.from(bytes).map(b => b.toString(16).padStart(2, '0')).join('');
}

function writeNonce(data, offset, nonce, le) {
  if (le) {
    data[offset]     =  nonce        & 0xff;
    data[offset + 1] = (nonce >>  8) & 0xff;
    data[offset + 2] = (nonce >> 16) & 0xff;
    data[offset + 3] = (nonce >> 24) & 0xff;
  } else {
    data[offset]     = (nonce >> 24) & 0xff;
    data[offset + 1] = (nonce >> 16) & 0xff;
    data[offset + 2] = (nonce >>  8) & 0xff;
    data[offset + 3] =  nonce        & 0xff;
  }
}

// Returns negative if a < b, 0 if equal, positive if a > b (byte-by-byte, big-endian)
function compareLE(a, b) {
  for (let i = 0; i < a.length; i++) {
    if (a[i] !== b[i]) return a[i] - b[i];
  }
  return 0;
}
