package io.cubyz.base;

import io.cubyz.api.ClientRegistries;
import io.cubyz.api.CubyzRegistries;
import io.cubyz.api.Registry;
import io.cubyz.base.entity_models.*;
import io.cubyz.base.rotation.*;
import io.cubyz.blocks.RotationMode;
import io.cubyz.entity.EntityModel;
import io.cubyz.models.CubeModel;

/**
 * Registers objects that are only available on the client.
 */

public class ClientProxy extends CommonProxy {

	public void init() {
		super.init();
		ClientRegistries.GUIS.register(new WorkbenchGUI());
	}
	
	public void preInit() {
		registerRotationModes(CubyzRegistries.ROTATION_MODE_REGISTRY);
		registerEntityModels(CubyzRegistries.ENTITY_MODEL_REGISTRY);
		CubeModel.registerCubeModels();
	}

	private void registerRotationModes(Registry<RotationMode> reg) {
		reg.register(new NoRotation());
		reg.register(new TorchRotation());
		reg.register(new LogRotation());
		reg.register(new StackableRotation());
		reg.register(new FenceRotation());
	}

	private void registerEntityModels(Registry<EntityModel> reg) {
		reg.register(new Quadruped());
	}
	
}
