package io.cubyz.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Transformation {

	private TransformationProduct transformationProduct;
	
	private TransformationOrthoProjection transformationOrthoProjection;

	private final Matrix4f projectionMatrix;

	private final Matrix4f worldMatrix;

	private final Matrix4f lightViewMatrix;

	private static final Matrix4f modelViewMatrix = new Matrix4f();

	private final Matrix4f orthoMatrix;

	public static final Vector3f xVec = new Vector3f(1, 0, 0); // There is no need to create a new object every time
																// this is needed.
	public static final Vector3f yVec = new Vector3f(0, 1, 0);

	public Transformation() {
		worldMatrix = new Matrix4f();
		projectionMatrix = new Matrix4f();
		this.transformationProduct = new TransformationProduct(new Matrix4f());
		this.transformationOrthoProjection = new TransformationOrthoProjection(new Matrix4f());
		orthoMatrix = new Matrix4f();
		lightViewMatrix = new Matrix4f();
	}

	public final Matrix4f getProjectionMatrix(float fov, float width, float height, float zNear, float zFar) {
		float aspectRatio = width / height;
		projectionMatrix.identity();
		projectionMatrix.perspective(fov, aspectRatio, zNear, zFar);
		return projectionMatrix;
	}

	public Matrix4f getWorldMatrix(Vector3f offset, Vector3f rotation, float scale) {
		worldMatrix.identity().translate(offset).rotateX(rotation.x).rotateY(rotation.y)
				.rotateZ(rotation.z).scale(scale);
		return worldMatrix;
	}

	public final Matrix4f getOrthoProjectionMatrix(float left, float right, float bottom, float top, float zNear, float zFar) {
		return transformationOrthoProjection.getOrthoProjectionMatrix(left, right, bottom, top, zNear, zFar);
	}

	public Matrix4f getOrtoProjModelMatrix(Spatial gameItem) {
		return gameItem.getOrtoProjModelMatrix(orthoMatrix);
	}
	
	public static Matrix4f getModelMatrix(Vector3f position, Vector3f rotation, Vector3f scale) {
		modelViewMatrix.identity()
			.translate(position)
			.rotateX(-rotation.x)
			.rotateY(-rotation.y)
			.rotateZ(-rotation.z)
			.scale(scale);
		return modelViewMatrix;
	}
	
	public static Matrix4f getModelMatrix(Vector3f position, Vector3f rotation, float scale) {
		modelViewMatrix.identity()
			.translate(position)
			.rotateX(-rotation.x)
			.rotateY(-rotation.y)
			.rotateZ(-rotation.z)
			.scale(scale);
		return modelViewMatrix;
	}
	
	public Matrix4f getViewMatrix(Camera camera) {
		return transformationProduct.getViewMatrix(camera);
	}
	
	public Matrix4f getViewMatrix(Vector3f position, Vector3f rotation) {
		return transformationProduct.getViewMatrix(position, rotation);
	}
	
	public Matrix4f getLightViewMatrix(Vector3f position, Vector3f rotation) {
		lightViewMatrix.identity();
		// First do the rotation so camera rotates over its position
		lightViewMatrix.rotate(rotation.x, xVec).rotate(rotation.y, yVec);
		// Then do the translation
		lightViewMatrix.translate(-position.x, -position.y, -position.z);
		return lightViewMatrix;
	}

	public static Matrix4f getModelViewMatrix(Spatial spatial, Matrix4f viewMatrix) {
		return getModelViewMatrix(spatial.modelViewMatrix, viewMatrix);
	}
	
	public static Matrix4f getModelViewMatrix(Matrix4f modelMatrix, Matrix4f viewMatrix) {
		//Matrix4f viewCurr = new Matrix4f(viewMatrix);
		//return viewCurr.mul(modelMatrix);
		return modelMatrix.mulLocal(viewMatrix); // seems to work, and doesn't allocate a new Matrix4f
	}
}
