package io.cubyz.rendering;


import org.joml.Matrix4f;
import org.joml.Vector3f;

public class TransformationProduct {
	private final Matrix4f viewMatrix;

	public TransformationProduct(Matrix4f Matrix4f) {
		viewMatrix = Matrix4f;
	}

	public Matrix4f getViewMatrix(Vector3f position, Vector3f rotation) {
		viewMatrix.identity();
		viewMatrix.rotate(rotation.x, Transformation.xVec).rotate(rotation.y, Transformation.yVec);
		viewMatrix.translate(-position.x, -position.y, -position.z);
		return viewMatrix;
	}

	public Matrix4f getViewMatrix(Camera camera) {
		Vector3f cameraPos = camera.getPosition();
		Vector3f rotation = camera.getRotation();
		return getViewMatrix(cameraPos, rotation);
	}
}