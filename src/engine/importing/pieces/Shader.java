package engine.importing.pieces;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

public class Shader {
	/*
    * if the shaders are setup ok we can use shaders, otherwise we just
    * use default settings
    */
    private boolean useShader=false;
    
    /*
    * program shader, to which is attached a vertex and fragment shaders.
    * They are set to 0 as a check because GL will assign unique int
    * values to each
    */
    private int shader=0;
    private int vertShader=0;
    private int fragShader=0;
    
    private FloatBuffer buf;

    public Shader(String path){
        /*
        * create the shader program. If OK, create vertex
        * and fragment shaders
        */
        shader=ARBShaderObjects.glCreateProgramObjectARB();
        
        if(shader!=0){
            vertShader=createVertShader(path + ".vert");
            fragShader=createFragShader(path + ".frag");
        }
        else 
        	useShader=false;

        /*
        * if the vertex and fragment shaders setup sucessfully,
        * attach them to the shader program, link the shader program
        * (into the GL context I suppose), and validate
        */
        if(vertShader != 0 && fragShader != 0){
            ARBShaderObjects.glAttachObjectARB(shader, vertShader);
            ARBShaderObjects.glAttachObjectARB(shader, fragShader);
            ARBShaderObjects.glLinkProgramARB(shader);
            ARBShaderObjects.glValidateProgramARB(shader);
            useShader=printLogInfo(shader);
            buf = BufferUtils.createFloatBuffer(16);
        } else {
        	useShader=false;
        	System.out.println("Failed to create shader for: " + path);
        	System.out.println("\tvertShader: " + vertShader + " && fragShader: " + fragShader);
        }        
    }
    /*
     * With the exception of syntax, setting up vertex and fragment shaders
     * is the same.
     * @param the name and path to the vertex shader
     */
     private int createVertShader(String filename){
         //vertShader will be non zero if successfully created
         vertShader=ARBShaderObjects.glCreateShaderObjectARB(ARBVertexShader.GL_VERTEX_SHADER_ARB);
         //if created, convert the vertex shader code to a String
         String vertexCode;
         if(vertShader==0){return 0;}
         
         vertexCode = getShaderText(filename);

         /*
         * associate the vertex code String with the created vertex shader
         * and compile
         */
         ARBShaderObjects.glShaderSourceARB(vertShader, vertexCode);
         ARBShaderObjects.glCompileShaderARB(vertShader);
         //if there was a problem compiling, reset vertShader to zero
         if(!printLogInfo(vertShader)){
        	 System.out.println("ERROR [vertshader id:" + vertShader + "]:\n" + vertexCode);
             vertShader=0;
         }
         //if zero we won't be using the shader
         return vertShader;
     }

     //same as per the vertex shader except for method syntax
     private int createFragShader(String filename){
     	//fragShader will be non zero if successfully created
         fragShader=ARBShaderObjects.glCreateShaderObjectARB(ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
         if(fragShader==0){return 0;}
         String fragCode="";
         String line;
         try{
        	 InputStreamReader is = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(filename));
             BufferedReader reader = new BufferedReader(is);
             while((line=reader.readLine())!=null){
                 fragCode+=line + "\n";
             }
         }catch(Exception e){
             System.out.println("Failed to read fragment shading code: " + filename);
             return 0;
         }
         ARBShaderObjects.glShaderSourceARB(fragShader, fragCode);
         ARBShaderObjects.glCompileShaderARB(fragShader);
         if(!printLogInfo(fragShader)){
        	 System.out.println("ERROR [fragshader id:" + fragShader + "]:\n" + fragCode);
             fragShader=0;
         }

         return fragShader;
     }
     
    /*
    * If the shader was setup successfully, we use the shader. Otherwise
    * we run normal drawing code.
    */
    public void startShader(int vbo_id, CollisionObject collision_object){
    	if(useShader) {
            ARBShaderObjects.glUseProgramObjectARB(shader);
            
            Transform transform_matrix = new Transform();
    		DefaultMotionState motion_state = (DefaultMotionState) ((RigidBody) collision_object).getMotionState();

    		transform_matrix.set(motion_state.graphicsWorldTrans);
            
    		//Adjust the position and rotation of the object from physics
    		float[] body_matrix = new float[16];
    		
    		transform_matrix.getOpenGLMatrix(body_matrix);
    		buf.put(body_matrix);
    		buf.flip();

        	GL11.glLoadIdentity();
    		int transform = ARBShaderObjects.glGetUniformLocationARB(shader, "transform");
        	//*****Shader drawing*****//
    		ARBShaderObjects.glUseProgramObjectARB(shader);

	    	if(transform>0){
	            ARBShaderObjects.glUniform4ARB(transform, buf);
	    		buf.clear();
	    	}else
	    		ARBShaderObjects.glUseProgramObjectARB(0);
        }
    }
    
    public void stopShader() {
        if(useShader) {
	        //release the shader
	        ARBShaderObjects.glUseProgramObjectARB(0);
        }
    }
    
    /*
    * oddly enough, checking the success when setting up the shaders is
    * verbose upon success. If the reference iVal becomes greater
    * than 1, the setup being examined (obj) has been successful, the
    * information gets printed to System.out, and true is returned.
    */
    private static boolean printLogInfo(int obj){
        IntBuffer iVal = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(
        	obj,
        	ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB,
        	iVal
        );
        
        int length = iVal.get();
        // We have some info we need to output.
        if (length > 1) {
            ByteBuffer infoLog = BufferUtils.createByteBuffer(length);
            iVal.flip();
            ARBShaderObjects.glGetInfoLogARB(obj, iVal, infoLog);
            byte[] infoBytes = new byte[length];
            infoLog.get(infoBytes);
            String out = new String(infoBytes);
            System.out.println("Info log:\n"+out);
        } else {
        	return false;
        }
        
        return true;
    }
    
	protected static ByteBuffer fileBuffer = BufferUtils.createByteBuffer(1024 * 10);
	protected static String getShaderText(String file) {
		/*
		String vertexCode="";
        String line;
        try{
        	InputStreamReader is = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(filename));
        	BufferedReader reader = new BufferedReader(is);
           	while((line=reader.readLine())!=null){
           		vertexCode+=line + "\n";
           	}
        }catch(Exception e){
            System.out.println("Failed to read vertex shading code: " + filename);
            return 0;
        }
		*/
		String shader = null;

		try {
			InputStream source = Shader.class.getResourceAsStream(file);
			if ( source == null ) // dev-mode
				source = new FileInputStream("src/engine/importing/pieces/" + file);

			BufferedInputStream stream = new BufferedInputStream(source);

			byte character;
			while ( (character = (byte)stream.read()) != -1 )
				fileBuffer.put(character);

			stream.close();

			fileBuffer.flip();

			byte[] array = new byte[fileBuffer.remaining()];
			fileBuffer.get(array);
			shader = new String(array);

			fileBuffer.clear();
		} catch (IOException e) {
			System.out.println("Failed to read the shader source file: " + file + " " + e.toString());
		}

		return shader;
	}
}