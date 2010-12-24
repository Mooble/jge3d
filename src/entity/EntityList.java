/*
 * The list of entities that the renderer will be drawing.
 * 
 * The reason its in a class rather than just using a hashmap is it will eventually 
 * listen to all of the entities it contains, in case their name changes.  (It can change 
 * the key they are saved in)
 * 
 * //TODO:	Listen to the entities it contains.  Yes, that means right now the class has 
 * 			no purpose other than being a placeholder
 */
package entity;

import java.util.HashMap;

public class EntityList {
	private HashMap<String,Entity> names;
	
	public EntityList(){ names = new HashMap<String,Entity>();}
	public boolean addItem(Entity e){
		if(e.keyExists("name")){
			names.put((String)e.getProperty("name"), e);
			names.size();
			return true;
		}else{
			return false;
		}
	}
	public void removeItem(String name){names.remove(name);}
	public Entity getItem(String name){
		//System.out.println("Length of list: " + String.valueOf(names.size()));
		//System.out.println("List: " + names.toString());
		return names.get(name);
	}
	public void drawList(){ 
		for(String key:names.keySet()){ 
			names.get(key).draw(); 
		}
	}
	
	public int size(){return names.size();}
}