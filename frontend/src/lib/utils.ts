export function hexToBytes(hex: string): Uint8Array {
  if (hex.length % 2 !== 0) hex = "0" + hex;
  const bytes = new Uint8Array(hex.length / 2);
  for (let i = 0; i < hex.length; i += 2) {
    bytes[i / 2] = parseInt(hex.substring(i, i + 2), 16);
  }
  return bytes;
}

export function bytesToHex(bytes: Uint8Array): string {
  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

export function concatUint8(arr1: Uint8Array, arr2: Uint8Array): Uint8Array {
  const res = new Uint8Array(arr1.length + arr2.length);
  res.set(arr1);
  res.set(arr2, arr1.length);
  return res;
}

export function leadingZeroBits(hash: Uint8Array): number {
  let count = 0;
  for (let i = 0; i < hash.length; i++) {
    const byte = hash[i];
    if (byte === 0) {
      count += 8;
    } else {
      // Use Math.clz32 on 32-bit integer. byte is 8-bit.
      // clz32 for 1 (00...0001) is 31.
      // For byte 1, we want leading zeros in 8 bits: 7.
      // byte as 32-bit: 00000000 00000000 00000000 BBBBBBBB
      // clz32(1) = 31. 31 - 24 = 7. Correct.
      count += Math.clz32(byte) - 24;
      break;
    }
  }
  return count;
}

export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export function compareHex(hex1: string, hex2: string): number {
  const h1 = hex1.padStart(64, "0");
  const h2 = hex2.padStart(64, "0");
  if (h1 < h2) return -1;
  if (h1 > h2) return 1;
  return 0;
}

export function nowMs(): number {
  return Date.now();
}

const encoder = new TextEncoder();
export function utf8ToBytes(str: string): Uint8Array {
  return encoder.encode(str);
}
