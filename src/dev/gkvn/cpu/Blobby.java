package dev.gkvn.cpu;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Blobby {
	static final int WIDTH = 160;
	static final int HEIGHT = 120;
	static final int FRAME_BYTES = WIDTH * HEIGHT * 3;

	public static void main(String[] args) throws Exception {
		ProcessBuilder pb = new ProcessBuilder(
			"ffmpeg", "-i", args[0], "-f", 
			"rawvideo", "-pix_fmt", "rgb24", "-vf",
			"scale=" + WIDTH + ":" + HEIGHT, "pipe:1"
		);
		pb.redirectError(ProcessBuilder.Redirect.DISCARD);
		Process proc = pb.start();
		InputStream in = new BufferedInputStream(proc.getInputStream());
		FileOutputStream out = new FileOutputStream(args[1]);
		byte[] rgb = new byte[FRAME_BYTES];
		byte[] frameOut = new byte[WIDTH * HEIGHT * 4 + 4];
		ByteBuffer bb = ByteBuffer.wrap(frameOut).order(ByteOrder.BIG_ENDIAN);
		int frame = 0;
		System.out.println("emitting...");

		while (true) {
			int read = 0;
			while (read < FRAME_BYTES) {
				int r = in.read(rgb, read, FRAME_BYTES - read);
				if (r < 0)
					break;
				read += r;
			}
			if (read < FRAME_BYTES) {
				break;
			}
			bb.clear();
			for (int i = 0; i < FRAME_BYTES; i += 3) {
				bb.put((byte) 0); // padding (so i can abuse LOAD/STORE WORD)
				bb.put(rgb[i]); // R
				bb.put(rgb[i + 1]); // G
				bb.put(rgb[i + 2]); // B
			}
			bb.putInt(0xCAFEBABE); // frame delimmter
			out.write(frameOut);
			System.out.println("done frame " + frame);
			frame++;
		}
		proc.waitFor();
		System.out.println("ok: " + frame + " frames written");
	}
}
