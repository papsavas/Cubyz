package io.cubyz.client.loading;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import io.cubyz.ClientOnly;
import io.cubyz.ClientSettings;
import io.cubyz.Constants;
import io.cubyz.Logger;
import io.cubyz.api.CubyzRegistries;
import io.cubyz.api.Mod;
import io.cubyz.api.Resource;
import io.cubyz.api.Side;
import io.cubyz.base.AddonsMod;
import io.cubyz.base.BaseMod;
import io.cubyz.blocks.Block;
import io.cubyz.client.Cubyz;
import io.cubyz.client.GameLauncher;
import io.cubyz.entity.EntityType;
import io.cubyz.modding.ModLoader;
import io.cubyz.rendering.Mesh;
import io.cubyz.rendering.ModelLoader;
import io.cubyz.ui.LoadingGUI;
import io.cubyz.utils.ResourceContext;
import io.cubyz.utils.ResourceManager;

/**
 * Loads all mods.
 */

public class LoadThread extends Thread {

	static int i = -1;
	static Runnable run;
	static ArrayList<Runnable> runnables = new ArrayList<>();
	
	public static void addOnLoadFinished(Runnable run) {
		runnables.add(run);
	}
	
	public void run() {
		setName("Load-Thread");
		Cubyz.renderDeque.add(ClientSettings::load); // run in render thread due to some graphical reasons
		LoadingGUI l = LoadingGUI.getInstance();
		l.setStep(1, 0, 0);
		
		l.setStep(2, 0, 0); // load mods
		
		// Load Mods (via reflection)
		ArrayList<File> modSearchPath = new ArrayList<>();
		modSearchPath.add(new File("mods"));
		modSearchPath.add(new File("mods/" + Constants.GAME_VERSION));
		ArrayList<String> modPaths = new ArrayList<>();
		
		for (File sp : modSearchPath) {
			if (!sp.exists()) {
				sp.mkdirs();
			}
			for (File mod : sp.listFiles()) {
				if (mod.isFile()) {
					modPaths.add(mod.getAbsolutePath());
					System.out.println("- Add " + mod.getName());
				}
			}
		}
		
		Logger.log("Seeking mods..");
		long start = System.currentTimeMillis();
		// Load all mods:
		ArrayList<Class<?>> allClasses = new ArrayList<>();
		for(String path : modPaths) {
			loadModClasses(path, allClasses);
		}
		long end = System.currentTimeMillis();
		Logger.log("Took " + (end - start) + "ms for reflection");
		if (!allClasses.contains(BaseMod.class)) {
			allClasses.add(BaseMod.class);
			allClasses.add(AddonsMod.class);
			Logger.log("Manually adding BaseMod (probably on distributed JAR)");
		}
		for (Class<?> cl : allClasses) {
			Logger.log("Mod class present: " + cl.getName());
			try {
				ModLoader.mods.add(cl.getConstructor().newInstance());
			} catch (Exception e) {
				Logger.warning("Error while loading mod:");
				Logger.throwable(e);
			}
		}
		Logger.log("Mod list complete");
		ModLoader.sortMods();
		
		// TODO re-add pre-init
		l.setStep(2, 0, ModLoader.mods.size());
		for (int i = 0; i < ModLoader.mods.size(); i++) {
			l.setStep(2, i+1, ModLoader.mods.size());
			Object mod = ModLoader.mods.get(i);
			Logger.log("Pre-initiating " + mod);
			ModLoader.preInit(mod, Side.CLIENT);
		}
		
		// Between pre-init and init code
		l.setStep(3, 0, ModLoader.mods.size());
		
		for (int i = 0; i < ModLoader.mods.size(); i++) {
			Object mod = ModLoader.mods.get(i);
			ModLoader.registerEntries(mod, "block");
		}
		for (int i = 0; i < ModLoader.mods.size(); i++) {
			Object mod = ModLoader.mods.get(i);
			ModLoader.registerEntries(mod, "item");
		}
		for (int i = 0; i < ModLoader.mods.size(); i++) {
			Object mod = ModLoader.mods.get(i);
			ModLoader.registerEntries(mod, "entity");
		}
		for (int i = 0; i < ModLoader.mods.size(); i++) {
			Object mod = ModLoader.mods.get(i);
			ModLoader.registerEntries(mod, "biome");
		}
		
		for (int i = 0; i < ModLoader.mods.size(); i++) {
			l.setStep(3, i+1, ModLoader.mods.size());
			Object mod = ModLoader.mods.get(i);
			Logger.log("Initiating " + mod);
			ModLoader.init(mod);
		}
		
		Object lock = new Object();
		run = new Runnable() {
			public void run() {
				i++;
				boolean finishedMeshes = false;
				if(i < CubyzRegistries.BLOCK_REGISTRY.size() || i < CubyzRegistries.ENTITY_REGISTRY.size()) {
					if(i < CubyzRegistries.BLOCK_REGISTRY.size()) {
						Block b = CubyzRegistries.BLOCK_REGISTRY.registered(new Block[0])[i];
						ClientOnly.createBlockMesh.accept(b);
					}
					if(i < CubyzRegistries.ENTITY_REGISTRY.size()) {
						EntityType e = CubyzRegistries.ENTITY_REGISTRY.registered(new EntityType[0])[i];
						if (!e.useDynamicEntityModel()) {
							ClientOnly.createEntityMesh.accept(e);
						}
					}
					if(i < CubyzRegistries.BLOCK_REGISTRY.size()-1 || i < CubyzRegistries.ENTITY_REGISTRY.size()-1) {
						Cubyz.renderDeque.add(run);
						l.setStep(4, i+1, CubyzRegistries.BLOCK_REGISTRY.size());
					} else {
						finishedMeshes = true;
						synchronized (lock) {
							lock.notifyAll();
						}
					}
				} else {
					finishedMeshes = true;
					synchronized (lock) {
						lock.notifyAll();
					}
				}
				if(finishedMeshes) {
					try {
						GameLauncher.logic.skyBodyMesh = new Mesh(ModelLoader.loadModel(new Resource("cubyz:sky_body.obj"), ResourceManager.lookupPath(ResourceManager.contextToLocal(ResourceContext.MODEL3D, new Resource("cubyz:sky_body.obj")))));
					} catch (Exception e) {
						Logger.throwable(e);
					}
				}
			}
		};
		Cubyz.renderDeque.add(run);
		try {
			synchronized (lock) {
				lock.wait();
			}
		} catch (InterruptedException e) {
			return;
		}
		
		l.setStep(5, 0, ModLoader.mods.size());
		for (int i = 0; i < ModLoader.mods.size(); i++) {
			l.setStep(5, i+1, ModLoader.mods.size());
			Object mod = ModLoader.mods.get(i);
			Logger.log("Post-initiating " + mod);
			ModLoader.postInit(mod);
		}
		l.finishLoading();
		
		for (Runnable r : runnables) {
			r.run();
		}
		
		System.gc();
	}
	
	public static void loadModClasses(String pathToJar, ArrayList<Class<?>> modClasses) {
		try {
			JarFile jarFile = new JarFile(pathToJar);
			Enumeration<JarEntry> e = jarFile.entries();
	
			URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
			URLClassLoader cl = URLClassLoader.newInstance(urls);
	
			while (e.hasMoreElements()) {
			    JarEntry je = e.nextElement();
			    if(je.isDirectory() || !je.getName().endsWith(".class") || je.getName().contains("module-info")){
			        continue;
			    }
			    // -6 because of .class
			    String className = je.getName().substring(0,je.getName().length()-6);
			    className = className.replace('/', '.');
			    Class<?> c = cl.loadClass(className);
			    if(c.isAnnotationPresent(Mod.class)) modClasses.add(c);
	
			}
			jarFile.close();
		} catch(IOException | ClassNotFoundException e) {
			Logger.throwable(e);
		}
	}
	
}
