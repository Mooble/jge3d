/*
 * This class is what holds any object in the world.
 * 	The user can add/remove custom properties (though a few are 
 * 	unremovable for internal engine reasons)
 * 
 *	//TODO: Maybe come up with actions like rotate, etc that a user might want to access
 *			Programmatically, and use skynet code to queue them up and play them back?
 *			Would function for animations and such too?
 */
package engine.entity;

import engine.Engine;
import engine.render.Model;
import engine.render.Shader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.PairCachingGhostObject;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

public class Entity{
	// Properties
	protected CollisionObject collision_object;
	private HashMap<String, Object> data;
	private ArrayList<EntityListener> listeners;
	//protected RenderObject model;
	private boolean shouldDraw = true;
	private Shader shader;

	private ArrayList<Method> collision_functions = new ArrayList<Method>(); 
	
	public static enum ObjectType {
		ghost, rigidbody, actor
	};

	protected ObjectType object_type;

	/* Properties the engine uses a lot */
	public static final String NAME = "name";
	public static final String COLLIDABLE = "collidable";
	public static final String TIME_TO_LIVE = "TTL";

	// Required keys
	public static String[] reqKeys = { NAME, COLLIDABLE, TIME_TO_LIVE };

	// Keep track of number of entities for naming purposes
	private static int num_entities = 0;

	// For making entity groups (complex bodies)
	private EntityList subEntities;

	/* Constructors */
	public Entity(float mass, boolean collide, Model model, Shader shader) {
		initialSetup(mass, collide, model, shader);
	}

	public Entity(String name, float mass, boolean collide, Model model, Shader shader) {
		initialSetup(name, mass, collide, model, shader);
	}
	
	public Entity(Entity ent) {
		String new_name = (String)ent.getProperty(Entity.NAME);
		float mass = ((RigidBody)ent.getCollisionObject()).getInvMass();
		boolean collide = (boolean)ent.getProperty(Entity.COLLIDABLE);
		Shader shader = (Shader)ent.getProperty("shader");		
				
		//TODO: Error checking for model cast
		initialSetup(new_name, mass, collide, (Model)ent.getProperty("model"), shader);
	}
	
	// Sets the initial name of the body in the list
	// Also sets some default options to the ent
	private void initialSetup(float mass, boolean c, Model model, Shader shader) {
		initialSetup("ent" + String.valueOf(num_entities), mass, c, model, shader);
	}

	private void initialSetup(String name, float mass, boolean c, Model model, Shader shader) {
		
		listeners = new ArrayList<EntityListener>();

		num_entities++;
		data = new HashMap<String, Object>();
		data.put("name", name);
		data.put("collidable", c);
		data.put("TTL", 0);
		//TODO: Generate this based on model instead
		//this.model = model;
		setProperty("model",model);
				
		CollisionShape shape = model.getCollisionShape();
		if(c){
			createRigidBody(mass, shape);
			object_type = ObjectType.rigidbody;
		}else{
			createGhostBody(mass, shape);
			object_type = ObjectType.ghost;
		}
	}

	/* Initializing segments */
	// Creates the initial settings for a rigidbody
	// This function is what we use to make things rotate over multiple axes
	private void createRigidBody(float mass, CollisionShape shape) {
		// rigid body is dynamic if and only if mass is non zero,
		// otherwise static
		boolean isDynamic = (mass != 0f);

		Vector3f localInertia = new Vector3f(0f, 0f, 0f);
		if (isDynamic) {
			shape.calculateLocalInertia(mass, localInertia);
		}

		DefaultMotionState motion_state = new DefaultMotionState(
			new Transform());
		RigidBodyConstructionInfo cInfo = new RigidBodyConstructionInfo(mass,
			motion_state, shape, localInertia);

		collision_object = new RigidBody(cInfo);

		// This is extremely important; if you forget this
		// then nothing will rotate
		Transform identity = new Transform();
		identity.setIdentity();
		((RigidBody) collision_object).setWorldTransform(identity);
		((RigidBody) collision_object).setMassProps(mass, localInertia);
		((RigidBody) collision_object).updateInertiaTensor();
	}

	protected void createGhostBody(float mass, CollisionShape shape) {
		// rigid body is dynamic if and only if mass is non zero,
		// otherwise static
		PairCachingGhostObject ghost = new PairCachingGhostObject();
		boolean isDynamic = (mass != 0f);

		Vector3f localInertia = new Vector3f(0f, 0f, 0f);
		if (isDynamic) {
			shape.calculateLocalInertia(mass, localInertia);
		}

		ghost.setCollisionShape(shape);
		ghost.setCollisionFlags(CollisionFlags.NO_CONTACT_RESPONSE);
		// This is extremely important; if you forget this
		// then nothing will rotate
		// ghost.setMassProps(mass, localInertia);
		// ghost.updateInertiaTensor();
		Transform identity = new Transform();
		identity.setIdentity();
		collision_object = new CollisionObject();
		collision_object = ghost;
		collision_object.setWorldTransform(identity);
	}

	/*
	 * End of Constructors
	 * 
	 * /* MUTATORS
	 */
	public void setPosition(Object p) {
		/*
		 * There's no straight-forward way to move a RigidBody to some location
		 * So that's what this class does. It takes an Object because of skynet
		 * code TODO: Remove skynet code
		 */
		try {
			Vector3f pos = ((Vector3f) p);
			Transform trans = collision_object.getWorldTransform(new Transform());
			trans.setIdentity();
			trans.origin.set(pos);
			collision_object.setWorldTransform(trans);

		} catch (Exception e) {
			System.out.print(
				p.toString()
				+ "<< Possible Incorrect data type for position, must be Vector3f\n"
			);
			e.printStackTrace();
		}
	}

	public void setProperty(String key, Object val) {
		data.put(key, val);
		for(EntityListener listener : listeners){
			listener.entityPropertyChanged(key, this);
		}
	}

	public void removeProperty(String key) {
		// Protect our required keys. Don't delete those, oh no!
		boolean req = false;
		for (int i = 0; i < reqKeys.length; i++) {
			if (reqKeys[i].equals(key)) req = true;
		}
		if (!req) {
			data.remove(key);
		}
	}

	/*
	public void setModel(Model model) {
		this.model = model;
	}
	*/

	public void setShouldDraw(boolean shouldDraw) {
		this.shouldDraw = shouldDraw;
	}

	/* ACCESSORS */
	public Vector3f getPosition() {
		Transform out = new Transform();
		out = collision_object.getWorldTransform(new Transform());
		return new Vector3f(out.origin);

	}

	public EntityList getSubEntities() {
		return subEntities;
	}

	public Set<String> getKeys() {
		return data.keySet();
	}

	public boolean keyExists(String prop_name) {
		for (int i = 0; i < data.keySet().size(); i++) {
			// TODO: Probably a better way to loop through the keys, but I'm
			// lazy
			if ((data.keySet().toArray())[i].equals(prop_name)) return true;
		}
		return false;
	}

	public Object getProperty(String key) {
		return data.get(key);
	}

	/*
	public Model getModel() {
		return (Model)model;
	}
	*/

	public Set<String> getKeySet() {
		return data.keySet();
	}

	public boolean shouldDraw() {
		return shouldDraw;
	}

	/* MISC */
	public void drawFixedPipe() {
		
		if (shouldDraw) {
			//TODO: Error checking for model cast
			Model model = (Model)getProperty("model");
			if(model != null) {
				model.drawFixedPipe(this);
			}
		}
	}
	
	
	public void drawProgrammablePipe() {
		if (shouldDraw) {
			//TODO: Error checking for model cast
			Model model = (Model)getProperty("model");
			if(model != null) {
				if(shader == null)
					model.drawProgrammablePipe(this);
				else
					model.drawProgrammablePipe(this,shader);
			}
		}
	}

	public void setCollisionFlags(int kinematic_object) {
		collision_object.setCollisionFlags(kinematic_object);
	}

	public void setGravity(Vector3f gravity) {
		if (object_type == ObjectType.rigidbody){ 
			((RigidBody) collision_object).setGravity(gravity);
		}else {
			System.out.println("Method [setGravity] not supported for ghost object");
		}
	}

	public void applyImpulse(Vector3f impulse, Vector3f position) {
		if (object_type == ObjectType.rigidbody){ 
			((RigidBody) collision_object).applyImpulse(impulse, position);
		} else {
			System.out.println("Method [applyImpulse] not supported for ghost object");
		}
	}
	
	public void applyTorqueImpulse(Vector3f impulse) {
		if (object_type == ObjectType.rigidbody){ 
			((RigidBody) collision_object).applyTorqueImpulse(impulse);
		} else {
			System.out.println("Method [applyTorqueImpulse] not supported for ghost object");
		}
	}
	
	public void clearForces() {
		if (object_type == ObjectType.rigidbody){ 
			((RigidBody) collision_object).clearForces();
		} else {
			System.out.println("Method [clearForces] not supported for ghost object");
		}
	}

	public CollisionObject getCollisionObject() {
		return collision_object;
	}

	public void setMotionState(DefaultMotionState defaultMotionState) {
		if (object_type == ObjectType.rigidbody){ 
			((RigidBody) collision_object).setMotionState(defaultMotionState);
		} else { 
			System.out.println("Method [setActivation] not supported for ghost object");
		}
	}

	public ObjectType getObjectType() {
		return object_type;
	}

	public void activate() {
		collision_object.activate();
	}

	public void setCollisionShape(CollisionShape createCollisionShape) {
		// Sets the new collision shape
		Vector3f scalevec = collision_object.getCollisionShape().getLocalScaling(new Vector3f());
		collision_object.setCollisionShape(createCollisionShape);
		collision_object.getCollisionShape().setLocalScaling(scalevec);

		// This is to correct for the fact that the center of mass
		// is not the same as the origin of the model
		// we have to do this here because the offset is calculated
		// by the model which doesn't get associated until now
		// TODO: This has not been tested at all
		// Someone should see if this actually corrects for the
		// offset problem
		//Transform offset = new Transform();
		//offset.origin.set(model.getCenter());
		Transform position = new Transform();
		position.origin.set(this.getPosition());
		//this.setMotionState(new DefaultMotionState(position, offset));
	}

	public void setAngularFactor(float factor, Vector3f velocity) {
		if (object_type == ObjectType.rigidbody) {
			((RigidBody) collision_object).setAngularFactor(factor);
			((RigidBody) collision_object).setAngularVelocity(velocity);
		} else {
			System.out.println("Method [setAngularFactor] not supported for ghost object");
		}
	}

	public void setDamping(float linear_damping, float angular_damping) {
		if (object_type == ObjectType.rigidbody) {
			// ((RigidBody)
			// collision_object).setInterpolationLinearVelocity(velocity);
			((RigidBody) collision_object).setDamping(linear_damping,
				angular_damping);
			// ((RigidBody) collision_object).applyDamping(0);
		} else {
			System.out.println("Method [setVelocity] not supported for ghost object");
		}
	}

	public void setAngularIdentity() {
		if (object_type == ObjectType.rigidbody) {
			// ((RigidBody)
			// collision_object).setInterpolationLinearVelocity(velocity);
			DefaultMotionState motionState = new DefaultMotionState();
			Transform t = new Transform();
			t.setIdentity();
			motionState.setWorldTransform(t);
			((RigidBody) collision_object).setMotionState(motionState);
			// ((RigidBody) collision_object).applyDamping(0);
		} else {
			System.out.println("Method [setVelocity] not supported for ghost object");
		}
	}
	
	public void addCollisionFunctions(String... names){
		for(String method_name : names){
			try {
				collision_functions.add(
					EntityCallbackFunctions.class.getMethod(method_name, Entity.class, Entity.class, Engine.class)
				);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public void removeCollisionFunctions(String... names){
		for(String method_name : names){
			boolean found = false;
			for(Method method : collision_functions){
				if(method.getName().equals(method_name)){
					collision_functions.remove(method);
					found = true;
				}
			}
			if(found){ break; }
		}
	}

	public ArrayList<Method> getCollisionFunctions() {
		return collision_functions;
	}
	
	public void setScale(Vector3f scale) {
		collision_object.getCollisionShape().setLocalScaling(scale);
	}
	
	public Vector3f getScale() {
		return collision_object.getCollisionShape().getLocalScaling(new Vector3f());
	}

	public double distanceFrom(Entity other_entity) {
		Vector3f this_pos = this.getPosition();
		Vector3f other_pos = other_entity.getPosition();
		
		return Math.abs(
			Math.sqrt(
				Math.pow(this_pos.x + other_pos.x, 2) +
				Math.pow(this_pos.y + other_pos.y, 2) +
				Math.pow(this_pos.z + other_pos.z, 2)
			)
		);
	}

	public boolean collidesWithRay(Vector3f position, Vector3f ray_to) {
		return true; //TODO: Obvious
	}

	public void getTransformation(float[] body_matrix) {
		Transform transform_matrix = new Transform();
		transform_matrix = this.getCollisionObject().getWorldTransform(new Transform());
		transform_matrix.getOpenGLMatrix(body_matrix);
	}
}
