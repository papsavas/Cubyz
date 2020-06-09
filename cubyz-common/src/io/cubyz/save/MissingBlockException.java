package io.cubyz.save;

import io.cubyz.api.Resource;

public class MissingBlockException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MissingBlockException(Resource id) {
		super("No such block : " + id.toString());
	}
	
}
