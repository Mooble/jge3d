package engine.render.ubos;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.vecmath.Vector3f;

import org.lwjgl.BufferUtils;

import engine.render.UBO.Type;

public class TransformationMatrices implements UBOInterface {
	//private Matrix4f perspective;
    private static final int size = 16;
    private static final String name = "TransformationMatrices";
    private static final String names[] = {
	    "projection",
    	"lookat"
    };
    		
    private float projection[];
    private float lookat[];
    
	public TransformationMatrices(float fov, float aspect, float zNear, float zFar, Vector3f eye, Vector3f focus, Vector3f up) {
		projection = new float[size];
		lookat = new float[size];
		buildProjectionMatrix(fov, aspect, zNear, zFar);
		buildLookAtMatrix(eye,focus,up);
	}
	
	public FloatBuffer createBuffer() {
		FloatBuffer buf = BufferUtils.createFloatBuffer(projection.length+lookat.length);
		
		buf.put(projection);
		buf.put(lookat);
		buf.flip();
		
		return buf;
	}
	
	public void buildProjectionMatrix(float fov, float aspect, float zNear, float zFar) {
		final float screen_size = (float) (zNear*Math.tan( fov*(Math.PI/360) )); //(float)Math.tan(fov);
		float left = -screen_size, right = screen_size, bottom = -screen_size / aspect, top = screen_size / aspect;
				
		//TODO: calculate these values instead of this f'd magic number 
		//float r = 5f;
		//float t = 5f;
		//This assumes that near plane of frustum is symmetric.  If not
		//Then look here: http://www.songho.ca/opengl/gl_projectionmatrix.html

		float[][] matrix = {
			{2*zNear/(right-left),	0,    	(right + left)/(right-left),    		  0},
			{0,				2*zNear/(top-bottom),	(top+bottom)/(top-bottom),				0},
			{0,				0,						-(zFar + zNear) / (zFar - zNear), -(2 * zFar * zNear) / (zFar - zNear)},
			{0,				0,						-1,      0}
		};
		
		System.out.println("projection");
		debugMatrix(matrix);
		
		for(int row = 0; row < 4; row++){
			for(int col = 0; col < 4; col++){
				projection[(row*4)+col] = matrix[row][col];
			}
		}
		
	}
	
	public void buildLookAtMatrix(Vector3f eye, Vector3f focus, Vector3f up) {
		/*
		Vector3f f = new Vector3f();
		Vector3f s = new Vector3f();
		Vector3f u = new Vector3f();
		
		f.sub(focus, eye);
		f.normalize();
		up.normalize();
		s.cross(f, up);
		u.cross(s, f);
		float[][] matrix = {
			{	s.x,	s.y,	s.z,	0},
			{	u.x,	u.y,	u.z,	0},
			{	-f.x,	-f.y,	-f.z,	0},
			{	0,		0,		0,		1},
		};
		*/
		Vector3f x_axis = new Vector3f();
		Vector3f y_axis = new Vector3f();
		Vector3f z_axis = new Vector3f();
		float x_temp;
		float y_temp;
		float z_temp;
		
		z_axis.sub(focus,eye);
		z_axis.normalize();
		
		x_axis.cross(up, z_axis);
		x_axis.normalize();
		 
		y_axis.cross(z_axis, x_axis);
		//y_axis.normalize();
		
		x_temp = -1 * x_axis.dot(eye);
		y_temp = -1 * y_axis.dot(eye);
		z_temp = -1 * z_axis.dot(eye);
		
		//rotation_matrix * translation_matrix, done by hand.
		//eye.negate();
		float[][] matrix = {
			{ x_axis.x, y_axis.x, z_axis.x, 0},
			{ x_axis.y, y_axis.y, z_axis.y, 0},
			{ x_axis.z, y_axis.z, z_axis.z, 0},
			{ x_temp,  y_temp,  z_temp,  1},
		};
		  
		System.out.println("lookat");
		debugMatrix(matrix);
		
		for(int row = 0; row < 4; row++){
			for(int col = 0; col < 4; col++){
				lookat[(row*4)+col] = matrix[row][col];
			}
		}
	}
	
	public int getSize() {
		return names.length;
	}
	
	public String[] getNames() {
		return names;
	}
	
	public ByteBuffer getNamesAsBuffer() {
		int name_size=0;
		for(String name: names){
			name_size+=name.length();
		}
		ByteBuffer buf = BufferUtils.createByteBuffer(name_size);
		
		for(String name: names) {
			buf.put(name.getBytes());
		}		
		
		buf.flip();

		return buf;
	}
	
	public String getName() {
		return name;
	}
	
	public IntBuffer getIndices() {
		return IntBuffer.wrap(new int[size]);
	}
	
	public Type getType() {
		return Type.PERSPECTIVE;
	}
	
	public void debugMatrix(float matrix[][]) {
		float m[] = new float[size];
		System.out.print("[\n");
		for(int row = 0; row < 4; row++){
			System.out.print("    [");
			for(int col = 0; col < 4; col++){
				m[(row*4)+col] = matrix[row][col];
				if(col==3)
					System.out.format(" %f ",m[(row*4)+col]);
				else
					System.out.format(" %f, ",m[(row*4)+col]);
			}
			System.out.print(" ]\n");
		}
		System.out.print("]\n");
	}
	public void debugMatrix(float m[]) {
		System.out.print("[\n");
		for(int row = 0; row < 4; row++){
			System.out.print("    [");
			for(int col = 0; col < 4; col++){
				if(col==3)
					System.out.format(" %f ",m[(row*4)+col]);
				else
					System.out.format(" %f, ",m[(row*4)+col]);
			}
			System.out.print(" ]\n");
		}
		System.out.print("]\n");
	}
}