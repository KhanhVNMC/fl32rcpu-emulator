package dev.gkvn.cpu.utils;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

// ===
// This code is automatically generated.
// Please do not modify.
//===
public class PS2Mappings {
	public static final Map<Integer, int[]> VK_TO_PS2 = new HashMap<>();
	static {
		// Alphabet
		VK_TO_PS2.put(KeyEvent.VK_A, new int[] { 0x1C });
		VK_TO_PS2.put(KeyEvent.VK_B, new int[] { 0x32 });
		VK_TO_PS2.put(KeyEvent.VK_C, new int[] { 0x21 });
		VK_TO_PS2.put(KeyEvent.VK_D, new int[] { 0x23 });
		VK_TO_PS2.put(KeyEvent.VK_E, new int[] { 0x24 });
		VK_TO_PS2.put(KeyEvent.VK_F, new int[] { 0x2B });
		VK_TO_PS2.put(KeyEvent.VK_G, new int[] { 0x34 });
		VK_TO_PS2.put(KeyEvent.VK_H, new int[] { 0x33 });
		VK_TO_PS2.put(KeyEvent.VK_I, new int[] { 0x43 });
		VK_TO_PS2.put(KeyEvent.VK_J, new int[] { 0x3B });
		VK_TO_PS2.put(KeyEvent.VK_K, new int[] { 0x42 });
		VK_TO_PS2.put(KeyEvent.VK_L, new int[] { 0x4B });
		VK_TO_PS2.put(KeyEvent.VK_M, new int[] { 0x3A });
		VK_TO_PS2.put(KeyEvent.VK_N, new int[] { 0x31 });
		VK_TO_PS2.put(KeyEvent.VK_O, new int[] { 0x44 });
		VK_TO_PS2.put(KeyEvent.VK_P, new int[] { 0x4D });
		VK_TO_PS2.put(KeyEvent.VK_Q, new int[] { 0x15 });
		VK_TO_PS2.put(KeyEvent.VK_R, new int[] { 0x2D });
		VK_TO_PS2.put(KeyEvent.VK_S, new int[] { 0x1B });
		VK_TO_PS2.put(KeyEvent.VK_T, new int[] { 0x2C });
		VK_TO_PS2.put(KeyEvent.VK_U, new int[] { 0x3C });
		VK_TO_PS2.put(KeyEvent.VK_V, new int[] { 0x2A });
		VK_TO_PS2.put(KeyEvent.VK_W, new int[] { 0x1D });
		VK_TO_PS2.put(KeyEvent.VK_X, new int[] { 0x22 });
		VK_TO_PS2.put(KeyEvent.VK_Y, new int[] { 0x35 });
		VK_TO_PS2.put(KeyEvent.VK_Z, new int[] { 0x1A });

		// Digits
		VK_TO_PS2.put(KeyEvent.VK_0, new int[] { 0x45 });
		VK_TO_PS2.put(KeyEvent.VK_1, new int[] { 0x16 });
		VK_TO_PS2.put(KeyEvent.VK_2, new int[] { 0x1E });
		VK_TO_PS2.put(KeyEvent.VK_3, new int[] { 0x26 });
		VK_TO_PS2.put(KeyEvent.VK_4, new int[] { 0x25 });
		VK_TO_PS2.put(KeyEvent.VK_5, new int[] { 0x2E });
		VK_TO_PS2.put(KeyEvent.VK_6, new int[] { 0x36 });
		VK_TO_PS2.put(KeyEvent.VK_7, new int[] { 0x3D });
		VK_TO_PS2.put(KeyEvent.VK_8, new int[] { 0x3E });
		VK_TO_PS2.put(KeyEvent.VK_9, new int[] { 0x46 });

		// Special Keys
		VK_TO_PS2.put(KeyEvent.VK_ENTER, new int[] { 0x5A });
		VK_TO_PS2.put(KeyEvent.VK_SPACE, new int[] { 0x29 });
		VK_TO_PS2.put(KeyEvent.VK_TAB, new int[] { 0x0D });
		VK_TO_PS2.put(KeyEvent.VK_BACK_SPACE, new int[] { 0x66 });
		VK_TO_PS2.put(KeyEvent.VK_ESCAPE, new int[] { 0x76 });

		// Navigation (Extended codes)
		VK_TO_PS2.put(KeyEvent.VK_UP, new int[] { 0xE0, 0x75 });
		VK_TO_PS2.put(KeyEvent.VK_DOWN, new int[] { 0xE0, 0x72 });
		VK_TO_PS2.put(KeyEvent.VK_LEFT, new int[] { 0xE0, 0x6B });
		VK_TO_PS2.put(KeyEvent.VK_RIGHT, new int[] { 0xE0, 0x74 });
		VK_TO_PS2.put(KeyEvent.VK_INSERT, new int[] { 0xE0, 0x70 });
		VK_TO_PS2.put(KeyEvent.VK_DELETE, new int[] { 0xE0, 0x71 });
		VK_TO_PS2.put(KeyEvent.VK_HOME, new int[] { 0xE0, 0x6C });
		VK_TO_PS2.put(KeyEvent.VK_END, new int[] { 0xE0, 0x69 });
		VK_TO_PS2.put(KeyEvent.VK_PAGE_UP, new int[] { 0xE0, 0x7D });
		VK_TO_PS2.put(KeyEvent.VK_PAGE_DOWN, new int[] { 0xE0, 0x7A });

		// Function Keys
		VK_TO_PS2.put(KeyEvent.VK_F1, new int[] { 0x05 });
		VK_TO_PS2.put(KeyEvent.VK_F2, new int[] { 0x06 });
		VK_TO_PS2.put(KeyEvent.VK_F3, new int[] { 0x04 });
		VK_TO_PS2.put(KeyEvent.VK_F4, new int[] { 0x0C });
		VK_TO_PS2.put(KeyEvent.VK_F5, new int[] { 0x03 });
		VK_TO_PS2.put(KeyEvent.VK_F6, new int[] { 0x0B });
		VK_TO_PS2.put(KeyEvent.VK_F7, new int[] { 0x83 });
		VK_TO_PS2.put(KeyEvent.VK_F8, new int[] { 0x0A });
		VK_TO_PS2.put(KeyEvent.VK_F9, new int[] { 0x01 });
		VK_TO_PS2.put(KeyEvent.VK_F10, new int[] { 0x09 });
		VK_TO_PS2.put(KeyEvent.VK_F11, new int[] { 0x78 });
		VK_TO_PS2.put(KeyEvent.VK_F12, new int[] { 0x07 });

		// Modifiers
		VK_TO_PS2.put(KeyEvent.VK_SHIFT, new int[] { 0x12 }); // Left Shift
		VK_TO_PS2.put(KeyEvent.VK_CONTROL, new int[] { 0x14 }); // Left Ctrl
		VK_TO_PS2.put(KeyEvent.VK_ALT, new int[] { 0x11 }); // Left Alt
	}
}