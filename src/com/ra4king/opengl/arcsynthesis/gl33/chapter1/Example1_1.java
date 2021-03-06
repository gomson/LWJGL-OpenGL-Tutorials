package com.ra4king.opengl.arcsynthesis.gl33.chapter1;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import com.ra4king.opengl.GLProgram;

public class Example1_1 extends GLProgram {
	public static void main(String[] args) {
		new Example1_1().run(true);
	}
	
	private int program, vbo;
	
	public Example1_1() {
		super("Example 1.1 - Hello Triangle", 500, 500, true);
	}
	
	@Override
	public void init() {
		glClearColor(0, 0, 0, 0);
		
		int vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, readFromFile("example1.1.vert"));
		
		glCompileShader(vs);
		
		if(glGetShaderi(vs, GL_COMPILE_STATUS) == GL_FALSE) {
			System.err.println("Failure in compiling vertex shader. Error log:\n" + glGetShaderInfoLog(vs, glGetShaderi(vs, GL_INFO_LOG_LENGTH)));
			System.exit(0);
		}
		
		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, readFromFile("example1.1.frag"));
		
		glCompileShader(fs);
		
		if(glGetShaderi(fs, GL_COMPILE_STATUS) == GL_FALSE) {
			System.err.println("Failure in compiling fragment shader. Error log:\n" + glGetShaderInfoLog(fs, glGetShaderi(fs, GL_INFO_LOG_LENGTH)));
			destroy();
		}
		
		program = glCreateProgram();
		glAttachShader(program, vs);
		glAttachShader(program, fs);
		
		glLinkProgram(program);
		
		if(glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
			System.err.println("Failure in linking program. Error log:\n" + glGetProgramInfoLog(program, glGetProgrami(program, GL_INFO_LOG_LENGTH)));
			destroy();
		}
		
		glDetachShader(program, vs);
		glDetachShader(program, fs);
		
		glDeleteShader(vs);
		glDeleteShader(fs);
		
		vbo = glGenBuffers();
		
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, (FloatBuffer)BufferUtils.createFloatBuffer(12).put(new float[] { 0.75f, 0.75f, 0.0f, 1.0f,
				0.75f, -0.75f, 0.0f, 1.0f,
				-0.75f, -0.75f, 0.0f, 1.0f }).flip(), GL_STATIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		// In core OpenGL, Vertex Array Objects (VAOs) are required for all draw calls. VAOs will be explained in Chapter 5.
		glBindVertexArray(glGenVertexArrays());
	}
	
	@Override
	public void render() {
		glClear(GL_COLOR_BUFFER_BIT);
		
		glUseProgram(program);
		
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 4, GL_FLOAT, false, 0, 0);
		
		glDrawArrays(GL_TRIANGLES, 0, 3);
		
		glDisableVertexAttribArray(0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		glUseProgram(0);
	}
}
