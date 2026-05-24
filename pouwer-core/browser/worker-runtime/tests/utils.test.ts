/// <reference types="vitest" />
import {bytesToHex, compareHex, concatUint8, hexToBytes, leadingZeroBits, nowMs, sleep, utf8ToBytes} from "../src/utils.js";
import {describe, expect, test, vi} from "vitest";

describe("utils", () => {
    test("testHexToBytesHandlesOddLength", () => {
        const bytes = hexToBytes("abc");
        expect(Array.from(bytes)).toEqual([0x0a, 0xbc]);
    });

    test("testHexToBytesHandlesEvenLength", () => {
        const bytes = hexToBytes("00ff");
        expect(Array.from(bytes)).toEqual([0x00, 0xff]);
    });

    test("testBytesToHexEncodesBytes", () => {
        const hex = bytesToHex(new Uint8Array([0x00, 0x0f, 0xff]));
        expect(hex).toBe("000fff");
    });

    test("testConcatUint8Concatenates", () => {
        const result = concatUint8(new Uint8Array([1, 2]), new Uint8Array([3, 4]));
        expect(Array.from(result)).toEqual([1, 2, 3, 4]);
    });

    test("testLeadingZeroBitsCountsLeadingZeros", () => {
        const count = leadingZeroBits(new Uint8Array([0x00, 0x00, 0x0f]));
        expect(count).toBe(20);
    });

    test("testLeadingZeroBitsCountsNonZeroFirstByte", () => {
        const count = leadingZeroBits(new Uint8Array([0x80, 0x00]));
        expect(count).toBe(0);
    });

    test("testCompareHexOrdersValues", () => {
        expect(compareHex("0a", "0b")).toBe(-1);
        expect(compareHex("0b", "0a")).toBe(1);
        expect(compareHex("0a", "0a")).toBe(0);
    });

    test("testUtf8ToBytesEncodesString", () => {
        const bytes = utf8ToBytes("A");
        expect(Array.from(bytes)).toEqual([65]);
    });

    test("testNowMsReturnsDateNow", () => {
        const spy = vi.spyOn(Date, "now").mockReturnValue(123456);
        expect(nowMs()).toBe(123456);
        spy.mockRestore();
    });

    test("testSleepResolvesAfterTimeout", async () => {
        vi.useFakeTimers();
        let resolved = false;
        const promise = sleep(50).then(() => {
            resolved = true;
        });

        await vi.advanceTimersByTimeAsync(49);
        expect(resolved).toBe(false);

        await vi.advanceTimersByTimeAsync(1);
        await promise;
        expect(resolved).toBe(true);

        vi.useRealTimers();
    });
});
