package input;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.BoxShape;

import de.matthiasmann.twl.Event;

import physics.Physics;
import window.Window;

import entity.Camera;
import entity.Entity;
import entity.EntityList;
import entity.QueueItem;

public class Input {
	private Camera camera;
	private EntityList objectList;
	private Physics physics;
	private Window window;

	//private int deltaX;
	//private int deltaY;

	//Holds Y coordinate that is reversed for raycasting
	private int adjustY;
	
	// ANGLES
	private static final float LEFT_RIGHT_INC = 0.00001f;
	private static final float UP_DOWN_INC = 0.00001f;

	// DISTANCE
	private static final float IN_OUT_INC = 1f;

	public Input(EntityList objectList, Window window) {
		this.objectList = objectList;
		physics = objectList.getPhysics();
		this.window=window;
	}

	public void init() {
		try {
			Keyboard.create();
			// Mouse.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}

		Keyboard.poll();
		Keyboard.enableRepeatEvents(false);
	}

	public boolean handleEvent(Event evt) {
		//Keyboard.poll();
		//Mouse.poll();
		if(camera != null && Mouse.isInsideWindow()) {
			handleMouse(evt);
			handleKeyboard();
		} else {
			camera = (Camera) objectList.getItem(Camera.CAMERA_NAME);
		}
		return true;
	}

	private void handleKeyboard() {
		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			camera.incrementRotation(LEFT_RIGHT_INC);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			camera.incrementRotation(-LEFT_RIGHT_INC);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
			camera.incrementDeclination(UP_DOWN_INC);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
			camera.incrementDeclination(-UP_DOWN_INC);
		}
	}

	private boolean handleMouse(Event evt){
		boolean button_caught=true;
		//update the changes in position
		//deltaX = Mouse.getEventDX();
		//deltaY = Mouse.getEventDY();

		//fix mouse coordinates
		adjustY = window.getHeight()-1-Mouse.getEventY();	
		switch(evt.getMouseButton()){
			case 1:
				BoxShape boxShape = new BoxShape(new Vector3f(1, 1, 1));
				Entity ent = new Entity(1.0f,boxShape,true);
				ent.applyForce(camera.getRayTo(Mouse.getX(), adjustY), camera.getPosition());
				objectList.enqueue(ent, QueueItem.ADD);
				break;
			case 0://Left Button
				physics.drag(
					camera,
					Mouse.getEventButtonState()? 0 : 1,
					camera.getRayTo(Mouse.getX(),adjustY)
				);
				break;
			case -1:
				
				break;
			default:
				System.out.println("unhandled mouse");
				button_caught=false;
				break;
		}

		if(button_caught==false) {
			switch(evt.getMouseWheelDelta()) {
				case -1: //mouse wheel up
					camera.incrementDistance(IN_OUT_INC);
					break;
				case  1: //mouse wheel down
					camera.incrementDistance(-IN_OUT_INC);
					break;
				default:
					System.out.println("unhandled mouse");
					button_caught=false;
					break;
			}
		}
		physics.motionFunc(camera,Mouse.getEventX(), adjustY);
		return button_caught;
	}
}
