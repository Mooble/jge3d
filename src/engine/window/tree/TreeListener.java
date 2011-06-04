package engine.window.tree;

import engine.controller.Config;

import javax.vecmath.Vector3f;

import de.matthiasmann.twl.TreeTable;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import engine.entity.Camera;
import engine.entity.Entity;
import engine.entity.EntityList;

public class TreeListener implements Runnable{
	TreeTable tree;
	TableSingleSelectionModel selection;
	EntityList objects;
	
	public TreeListener(TreeTable tree, TableSingleSelectionModel selection, EntityList objects){
		this.tree = tree;
		this.selection = selection;
		this.objects = objects;
	}
	
	public void run() {
		int row = selection.getFirstSelected();
		if(row >= 0) {
			Node n = (Node)tree.getNodeFromRow(row);
	
			Entity newFocus;
			try {
				newFocus = objects.getItem("hurr" + "-" + (String)n.getData(0).toString());
				if( newFocus != null){
					stopResetAndMove(newFocus);
				}
			} catch (Exception e) {
				System.out.println("No config to catch");
				e.printStackTrace();
			}
		}
	}
	
	private synchronized void stopResetAndMove(Entity newFocus){
		//Reset position of all other objects
		for(String name: objects.getKeySet()) {
			//For everything but the camera do the following
			if(!name.equals(Camera.CAMERA_NAME)) {
				for( int i = 0; i < 100; i++){
					//Stop the movement
					objects.getItem(name).activate();
					objects.getItem(name).setDamping(1.0f,1.0f);
					objects.getItem(name).setAngularFactor(0.0f, new Vector3f(0,0,0));
					
					//Move the object back to its original position
					objects.getItem(name).setAngularIdentity();
					objects.getItem(name).setPosition(Config.getPosition(name));
				}
			}
		}
		
		Camera cam = ((Camera)objects.getItem(Camera.CAMERA_NAME));
		//Focus to this ent
		cam.focusOn(newFocus);
		
		try {
			wait(10);
		} catch (InterruptedException e) {}
		
		for(String name: objects.getKeySet()) {
			//For everything but the camera do the following
			if(!name.equals(Camera.CAMERA_NAME) && !name.equals(newFocus.getProperty("name"))) {
				//Stop the movement.
				objects.getItem(name).setDamping(0.0f,0.0f);
				objects.getItem(name).applyImpulse(new Vector3f(0,0,-1), newFocus.getPosition());
			}
		}
		
		newFocus.setDamping(1.0f,0.0f);
		newFocus.activate();
		newFocus.setAngularFactor(0.0f, new Vector3f(1,1,0));
		
		cam.setDeclination(0.0);
		cam.setRotation(0.0);
		cam.setDistance(2.0f);
	}
}
