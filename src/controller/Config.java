package controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.vecmath.Vector3f;

import entity.Entity;

import window.tree.Model;
import window.tree.Node;

public class Config {
	static class ConfigItem {
		public Entity fullassembly_focus;
		public Entity lineup_focus;
		public Model treeModel;
		public String name;
		public HashMap<String, Vector3f> positions;
		
		public ConfigItem(String name, Model treeModel, HashMap<String, Vector3f> defaultPositions, Entity fullassembly_focus, Entity lineup_focus) throws Exception{
			this.treeModel = treeModel;
			this.name = name;
			this.positions = defaultPositions;
			this.fullassembly_focus = fullassembly_focus;
			this.lineup_focus = lineup_focus;
			
			if( this.fullassembly_focus == null || this.lineup_focus == null){
				throw new Exception("A focus has been set to null");
			}
		}
	}
	
	private static HashMap<String, ConfigItem> configs = new HashMap<String, ConfigItem>();
	private static String currentKey;
	private static ArrayList<ConfigListener> listeners = new ArrayList<ConfigListener>();
	
	public synchronized static void addConfig(String name, Vector3f position, Model treeModel, HashMap<String, Vector3f> defaultPositions, Entity fullassembly_focus, Entity lineup_focus) throws Exception{
		configs.put(name, new ConfigItem(name, treeModel, defaultPositions, fullassembly_focus, lineup_focus));
		
		if(currentKey == null){
			try {
				changeConfig(name);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public synchronized static Model treeModel() throws Exception{
		if(currentKey == null){
			throw new Exception("TreeModel: No config to fetch");
		}
		return configs.get(currentKey).treeModel;
	}
	
	public synchronized static void changeConfig(String key) throws Exception{
		if(!configs.containsKey(key)){
			throw new Exception("Can't change to a config that doesn't exist...");
		}
		currentKey = key;
		updateObservers();
	}

	public static synchronized String getName() throws Exception {
		if(currentKey == null){
			throw new Exception("Name: No config to fetch");
		}
		return configs.get(currentKey).name;
	}


	public static synchronized void registerObserver(ConfigListener list) {
		listeners.add(list);
	}
	
	private static synchronized void updateObservers(){
		for(ConfigListener c : listeners){
			c.configChanged();
		}
	}
	
	public static synchronized ArrayList<Node> getNodes() throws Exception{
		if(currentKey == null){
			throw new Exception("Name: No config to fetch");
		}
		return configs.get(currentKey).treeModel.getChildren();
	}
	
	public static synchronized Vector3f getPosition(String name){
		String[] args = name.split("-");
		Vector3f temp = configs.get(args[0]).positions.get(args[1]);
		
		return new Vector3f(temp.x, temp.y, temp.z);
	}


	public static synchronized Entity getFullAssemblyFocus() {
		return configs.get(currentKey).fullassembly_focus;
	}
	
	public static synchronized Entity getLineupFocus() {
		return configs.get(currentKey).lineup_focus;
	}
	
	public static Set<String> getKeys() {
		return configs.get(currentKey).positions.keySet();
	}
}
