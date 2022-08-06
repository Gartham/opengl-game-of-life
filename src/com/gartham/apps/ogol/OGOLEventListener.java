package com.gartham.apps.ogol;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.gartham.libs.gameoflife.GameOfLife;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.GLBuffers;

public final class OGOLEventListener implements GLEventListener {

	private static float[] tilize(boolean[][] board) {
		List<Float> floats = new ArrayList<>();

		for (int i = 0; i < board.length; i++)
			for (int j = 0; j < board[i].length; j++)
				if (board[i][j]) {
					// First triangle
					floats.add(i * 2f / board.length - 1);
					floats.add(j * 2f / board[i].length - 1);

					floats.add((i + 1) * 2f / board.length - 1);
					floats.add(j * 2f / board[i].length - 1);

					floats.add((i + 1) * 2f / board.length - 1);
					floats.add((j + 1) * 2f / board[i].length - 1);

					// Second triangle
					floats.add(i * 2f / board.length - 1);
					floats.add(j * 2f / board[i].length - 1);

					floats.add((i + 1) * 2f / board.length - 1);
					floats.add((j + 1) * 2f / board[i].length - 1);

					floats.add(i * 2f / board.length - 1);
					floats.add((j + 1) * 2f / board[i].length - 1);
				}

		Float[] f = floats.toArray(new Float[floats.size()]);
		float[] res = new float[f.length];
		for (int i = 0; i < res.length; i++)
			res[i] = f[i];

		return res;
	}

	private int prog, vertBuffHandle, vao;
	private final GameOfLife gol = new GameOfLife(1000, 1000);
	{
		gol.randomize();
	}

	private FloatBuffer vertbuff = GLBuffers.newDirectFloatBuffer(tilize(gol.getBoard()));
//	(new float[] { -1f, -1f, 0, 1f, 1f, 0f, -1f, 1f, 0f });

	public static String loadShader(InputStream is) {
		Scanner s = new Scanner(is);
		StringBuilder sb = new StringBuilder();
		while (s.hasNextLine())
			sb.append(s.nextLine()).append('\n');

		s.close();
		return sb.toString();
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		drawable.getGL().getGL3().glViewport(0, 0, width, height);
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		// Create shaders.
		GL3 gl = drawable.getGL().getGL3();

		int vertshader = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
		gl.glShaderSource(vertshader, 1, new String[] { loadShader(getClass().getResourceAsStream("vertexShader.vs")) },
				null);
		gl.glCompileShader(vertshader);
		int fragshader = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
		gl.glShaderSource(fragshader, 1,
				new String[] { loadShader(getClass().getResourceAsStream("fragmentShader.fs")) }, null);
		gl.glCompileShader(fragshader);

		// Create shader program.
		prog = gl.glCreateProgram();
		gl.glAttachShader(prog, vertshader);
		gl.glAttachShader(prog, fragshader);

		gl.glLinkProgram(prog);

		// Create vertex buffers.
		{
			IntBuffer b = IntBuffer.allocate(1);
			gl.glGenBuffers(1, b);
			vertBuffHandle = b.get(0);
		}

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertBuffHandle);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, vertbuff.capacity() * Float.BYTES, vertbuff, GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

		// Create VAO.
		{
			IntBuffer b = IntBuffer.allocate(1);
			gl.glGenVertexArrays(1, b);
			gl.glBindVertexArray(vao = b.get(0));
		}

	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		GL3 gl = drawable.getGL().getGL3();
		gl.glDeleteProgram(prog);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		drawable.getGL().glClearColor(0, 0, 0, 1);
		drawable.getGL().glClear(GL.GL_COLOR_BUFFER_BIT);

		GL3 gl = drawable.getGL().getGL3();

		// Draw game.
		gl.glUseProgram(prog);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertBuffHandle);
		gl.glEnableVertexAttribArray(0);
		// The last parameter is the number of bytes in the float array above to offset
		// before reading.
		gl.glVertexAttribPointer(0, 2, GL.GL_FLOAT, false, 0, 0);
		// If call was gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 4, 4);, then
		// you could put another float val in front of array (first four bytes will be
		// skipped over in array).

		gl.glDrawArrays(GL.GL_TRIANGLES, 0, vertbuff.capacity() / 2);

		gl.glDisableVertexAttribArray(0);
		gl.glUseProgram(0);

		// Tick game.
		gol.tick();
		vertbuff = GLBuffers.newDirectFloatBuffer(tilize(gol.getBoard()));

		// Reassign to buffer.
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertBuffHandle);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, vertbuff.capacity() * Float.BYTES, vertbuff, GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

	}
}