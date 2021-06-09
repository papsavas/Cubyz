package io.cubyz.rendering;

import org.joml.Matrix4f;

public class TransformationOrthoProjection {

	private final Matrix4f orthoMatrix;
	


	public TransformationOrthoProjection(Matrix4f Matrix4f) {
		orthoMatrix = Matrix4f;
		
	}
	
	public final Matrix4f getOrthoProjectionMatrix(float left, float right, float bottom, float top, float zNear, float zFar) {
		orthoMatrix.identity();
		orthoMatrix.setOrtho(left, right, bottom, top, zNear, zFar);
		return orthoMatrix;
	}


}
