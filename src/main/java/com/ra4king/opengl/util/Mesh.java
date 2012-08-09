package com.ra4king.opengl.util;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class Mesh {
	private int vao;
	private ArrayList<RenderCmd> renderCommands;
	
	public Mesh(InputStream file) throws Exception {
		renderCommands = new ArrayList<RenderCmd>();
		
		XmlPullParser xml = XmlPullParserFactory.newInstance().newPullParser();
		xml.setInput(file, "UTF-8");
		
		xml.next();
		
		xml.require(XmlPullParser.START_TAG, null, "mesh");
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		
		ByteBuffer attributeData = BufferUtils.createByteBuffer(0), indexData = BufferUtils.createByteBuffer(0);
		
		do {
			xml.nextTag();
			
			switch(xml.getName()) {
				case "attribute": {
					int index = Integer.parseInt(xml.getAttributeValue(null, "index"));
					String type = xml.getAttributeValue(null, "type");
					int size = Integer.parseInt(xml.getAttributeValue(null, "size"));
					
					Attribute attrib = new Attribute(index,type,size);
					attributes.add(attrib);
					
					xml.next();
					xml.require(XmlPullParser.TEXT, null, null);
					
					attributeData = attrib.storeData(attributeData, clean(xml.getText().trim().replace("\r\n", " ").replace('\n', ' ').split(" ")));
					
					xml.next();
					xml.require(XmlPullParser.END_TAG, null, "attribute");
					
					break;
				}
				case "indices": {
					String primitive = xml.getAttributeValue(null, "cmd");
					String type = xml.getAttributeValue(null, "type");
					
					RenderCmd cmd = new RenderCmd(primitive,type);
					renderCommands.add(cmd);
					
					xml.next();
					xml.require(XmlPullParser.TEXT, null, null);
					
					indexData = cmd.storeData(indexData, clean(xml.getText().trim().replace("\r\n", " ").replace('\n', ' ').split(" ")));
					
					xml.next();
					xml.require(XmlPullParser.END_TAG, null, "indices");
					
					break;
				}
				case "arrays": {
					String primitive = xml.getAttributeValue(null, "cmd");
					int start = Integer.parseInt(xml.getAttributeValue(null, "start"));
					int count = Integer.parseInt(xml.getAttributeValue(null, "count"));
					
					RenderCmd cmd = new RenderCmd(primitive,start,count);
					renderCommands.add(cmd);
					
					xml.next();
					xml.require(XmlPullParser.END_TAG, null, "arrays");
					
					break;
				}
				case "mesh":
					break;
				default:
					throw new IllegalArgumentException("Invalid TAG name: " + xml.getName());
			}
		} while(xml.next() != XmlPullParser.END_DOCUMENT);
		
		if(attributes.size() == 0)
			throw new IllegalArgumentException("There must be at least 1 set of attributes.");
		if(renderCommands.size() == 0)
			throw new IllegalArgumentException("There must be at least 1 render command.");
		
		attributeData.flip();
		indexData.flip();
		
		int vbo1 = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo1);
		glBufferData(GL_ARRAY_BUFFER, (ByteBuffer)BufferUtils.createByteBuffer(attributeData.capacity()).put(attributeData).flip(),GL_STATIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		int vbo2 = -1;
		if(indexData.hasRemaining()) {
			vbo2 = glGenBuffers();
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo2);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, (ByteBuffer)BufferUtils.createByteBuffer(indexData.capacity()).put(indexData).flip(),GL_STATIC_DRAW);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		}
		
		vao = glGenVertexArrays();
		glBindVertexArray(vao);
		
		glBindBuffer(GL_ARRAY_BUFFER, vbo1);
		for(Attribute attrib : attributes) {
			glEnableVertexAttribArray(attrib.index);
			glVertexAttribPointer(attrib.index, attrib.size, attrib.type.dataType, attrib.type.normalized, 0, attrib.offset);
		}
		
		if(vbo2 >= 0)
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo2);
		
		glBindVertexArray(0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	}
	
	private String[] clean(String[] data) {
		ArrayList<String> clean = new ArrayList<String>();
		for(String s : data)
			if(!(s = s.trim()).isEmpty())
				clean.add(s);
		return clean.toArray(new String[0]);
	}
	
	public void render() {
		glBindVertexArray(vao);
		for(RenderCmd cmd : renderCommands)
			cmd.render();
		glBindVertexArray(0);
	}
	
	private static class RenderCmd {
		private boolean isIndexedCmd;
		private int primitive;
		private int start;
		private int count;
		private RenderCommandType type;
		
		public RenderCmd(String primitive, String type) {
			isIndexedCmd = true;
			this.primitive = getPrimitive(primitive);
			this.type = RenderCommandType.getRenderType(type);
		}
		
		public RenderCmd(String primitive, int start, int count) {
			isIndexedCmd = false;
			this.primitive = getPrimitive(primitive);
			this.start = start;
			this.count = count;
		}
		
		public ByteBuffer storeData(ByteBuffer b, String[] data) {
			start = b.position();
			count = data.length;
			
			ByteBuffer b2 = BufferUtils.createByteBuffer(start+count*type.size);
			b2.put((ByteBuffer)b.flip());
			type.store(b2, data);
			
			return b2;
		}
		
		private static int getPrimitive(String name) {
			switch(name) {
				case "triangles": return GL_TRIANGLES;
				case "tri-fan": return GL_TRIANGLE_FAN;
				case "tri-strip": return GL_TRIANGLE_STRIP;
				case "lines": return GL_LINES;
				case "line-strip": return GL_LINE_STRIP;
				case "line-loop": return GL_LINE_LOOP;
				case "points": return GL_POINTS;
				default: throw new IllegalArgumentException("Invalid primitive name: " + name);
			}
		}
		
		public void render() {
			if(isIndexedCmd)
				glDrawElements(primitive, count, type.dataType, start);
			else
				glDrawArrays(primitive, start, count);
		}
	}
	
	private enum RenderCommandType {
		UBYTE("ubyte",GL_UNSIGNED_BYTE,1), USHORT("ushort",GL_UNSIGNED_SHORT,2), UINT("uint",GL_UNSIGNED_INT,4);
		
		private String name;
		private int dataType;
		private int size;
		
		private RenderCommandType(String name, int type, int size) {
			this.name = name;
			this.dataType = type;
			this.size = size;
		}
		
		public void store(ByteBuffer b, String[] data) {
			switch(dataType) {
				case GL_UNSIGNED_BYTE:
					for(String s : data)
						b.put(Byte.parseByte(s));
					break;
				case GL_UNSIGNED_SHORT:
					ShortBuffer sb = b.asShortBuffer();
					for(String s : data)
						sb.put(Short.parseShort(s));
					break;
				case GL_UNSIGNED_INT:
					IntBuffer ib = b.asIntBuffer();
					for(String s : data)
						ib.put(Integer.parseInt(s));
					break;
			}
			
			b.position(b.capacity());
		}
		
		public static RenderCommandType getRenderType(String name) {
			for(RenderCommandType rt : values()) {
				if(rt.name.equals(name))
					return rt;
			}
			
			throw new IllegalArgumentException("Unsupported render command type!");
		}
	}
	
	private class Attribute {
		private int index;
		private AttributeType type;
		private int size;
		private int offset;
		
		public Attribute(int index, String type, int size) {
			this.index = index;
			this.size = size;
			
			this.type = AttributeType.getAttributeType(type);
		}
		
		public ByteBuffer storeData(ByteBuffer b, String[] data) {
			offset = b.position();
			
			ByteBuffer b2 = BufferUtils.createByteBuffer(offset+data.length*type.size);
			b2.put((ByteBuffer)b.flip());
			type.store(b2, data);
			
			return b2;
		}
	}
	
	private enum AttributeType {
		FLOAT("float",false,GL_FLOAT,4),
		INT("int",false,GL_INT,4), UINT("uint",false,GL_UNSIGNED_INT,4), NORM_INT("norm-int",true,GL_INT,4), NORM_UINT("norm-uint",true,GL_UNSIGNED_INT,4),
		SHORT("short",false,GL_SHORT,2), USHORT("ushort",false,GL_UNSIGNED_SHORT,2), NORM_SHORT("norm-short",true,GL_SHORT,2), NORM_USHORT("norm-ushort",true,GL_UNSIGNED_SHORT,2),
		BYTE("byte",false,GL_BYTE,1), UBYTE("ubyte",false,GL_UNSIGNED_BYTE,1), NORM_BYTE("norm-byte",true,GL_BYTE,1), NORM_UBYTE("norm-ubyte",true,GL_UNSIGNED_BYTE,1);
		
		private String name;
		private boolean normalized;
		private int dataType;
		private int size;
		
		private AttributeType(String name, boolean normalized, int type, int size) {
			this.name = name;
			this.normalized = normalized;
			this.dataType = type;
			this.size = size;
		}
		
		public void store(ByteBuffer b, String[] data) {
			switch(dataType) {
				case GL_FLOAT:
					FloatBuffer fb = b.asFloatBuffer();
					for(String s : data)
						fb.put(Float.parseFloat(s));
					break;
				case GL_INT:
				case GL_UNSIGNED_INT:
					IntBuffer ib = b.asIntBuffer();
					for(String s : data)
						ib.put(Integer.parseInt(s));
					break;
				case GL_SHORT:
				case GL_UNSIGNED_SHORT:
					ShortBuffer sb = b.asShortBuffer();
					for(String s : data)
						sb.put(Short.parseShort(s));
					break;
				case GL_BYTE:
				case GL_UNSIGNED_BYTE:
					for(String s : data)
						b.put(Byte.parseByte(s));
					break;
			}
			
			b.position(b.capacity());
		}
		
		public static AttributeType getAttributeType(String name) {
			for(AttributeType at : values()) {
				if(at.name.equals(name))
					return at;
			}
			
			throw new IllegalArgumentException("Unsupported attribute type!");
		}
	}
}