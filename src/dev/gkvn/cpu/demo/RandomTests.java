package dev.gkvn.cpu.demo;

import dev.gkvn.cpu.fl32r.Utils;

public class RandomTests {

	public static void main(String[] args) {

        boolean ok = true;

        for (int i = -8191; i <= 8191; i++) {

            int encoded = Utils.convertIntToU14(i);
            int decoded = Utils.convertU14ToInt(encoded);

            if (decoded != i) {
                ok = false;
                System.out.printf(
                    "FAIL: %6d -> encoded=%04x -> decoded=%6d%n",
                    i, encoded, decoded
                );
            }
        }

        if (ok) {
            System.out.println("ALL TESTS PASSED");
        }
    }

}
