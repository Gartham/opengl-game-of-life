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
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.GLBuffers;

public final class OGOLEventListener implements GLEventListener {

	private static final int BOARD_WIDTH = 1000, BOARD_HEIGHT = 1000;

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

	private static int[] getTriangleIndices(boolean[][] board) {
		List<Integer> indices = new ArrayList<>();

		for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[i].length; j++) {
				if (board[i][j]) {
					indices.add(i * board[0].length + j * board.length);
					indices.add((i + 1) * board[0].length + j * board.length);
					indices.add((i + 1) * board[0].length + (j + 1) * board.length);

					indices.add(i * board[0].length + j * board.length);
					indices.add((i + 1) * board[0].length + (j + 1) * board.length);
					indices.add(i * board[0].length + (j + 1) * board.length);
				}
			}
		}
		Integer[] i = indices.toArray(new Integer[indices.size()]);
		int[] res = new int[i.length];
		for (int j = 0; j < res.length; j++)
			res[j] = i[j];
		return res;
	}

	private static float[] generateVertBuff(int boardwidth, int boardheight) {
		float[] data = new float[(boardwidth + 1) * (boardheight + 1) * 2];
		for (int i = 0; i <= boardwidth; i++)
			for (int j = 0; j <= boardheight; j++) {
				data[(i * (boardheight + 1) + j) * 2] = i * 2f / boardwidth - 1;
				data[(i * (boardheight + 1) + j) * 2 + 1] = j * 2f / boardheight - 1;
			}
		return data;
	}

	private int prog, vertBuffHandle, vertIndicesBuffHandle, vao;
	private final GameOfLife gol = new GameOfLife(BOARD_WIDTH, BOARD_HEIGHT);
	{
		gol.randomize();
	}

	private FloatBuffer vertbuff = GLBuffers.newDirectFloatBuffer(generateVertBuff(BOARD_WIDTH, BOARD_HEIGHT));
	private IntBuffer vertindices = GLBuffers.newDirectIntBuffer(getTriangleIndices(gol.getBoard()));
//			(tilize(gol.getBoard()));
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

		drawable.getGL().getGL4().glEnable(GL4.GL_DEBUG_OUTPUT);
		drawable.getGL().getGL4().glEnable(GL4.GL_DEBUG_OUTPUT_SYNCHRONOUS);

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
			IntBuffer b = IntBuffer.allocate(2);
			gl.glGenBuffers(2, b);
			vertBuffHandle = b.get(0);
			vertIndicesBuffHandle = b.get(1);
		}

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertBuffHandle);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, vertbuff.capacity() * Float.BYTES, vertbuff, GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vertIndicesBuffHandle);
		gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, vertindices.capacity() * Integer.BYTES, vertindices,
				GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);

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

		gl.glEnableVertexAttribArray(0);
		gl.glVertexAttribPointer(0, 2, GL.GL_FLOAT, false, 0, 0);

		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vertIndicesBuffHandle);
		gl.glDrawElements(GL.GL_TRIANGLES, vertindices.capacity() / 2, GL.GL_UNSIGNED_INT, 0);
//		gl.glDrawArrays(GL.GL_TRIANGLES, 0, vertbuff.capacity() / 2);

		gl.glDisableVertexAttribArray(0);
		gl.glUseProgram(0);

		// Tick game.
		gol.tick();
		vertindices = GLBuffers.newDirectIntBuffer(getTriangleIndices(gol.getBoard()));

		// Reassign to buffer.
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vertIndicesBuffHandle);
		gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, vertindices.capacity() * Integer.BYTES, vertindices,
				GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);

	}
}