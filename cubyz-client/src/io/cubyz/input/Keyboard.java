package io.cubyz.input;

import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;

import io.cubyz.Logger;

public class Keyboard {

	static ArrayList<Integer> pressedKeys = new ArrayList<Integer>();
	private static int bufferLen = 256;
	static final char[] charBuffer = new char[bufferLen]; // Pseudo-circular buffer of the last chars, to avoid problems if the user is a fast typer or uses macros or compose key.
	private static int lastStart = 0, lastEnd = 0, current = 0;
	static int currentKeyCode;
	static boolean hasKeyCode;
	static int keyMods;
	
	public static void pushChar(char ch) {
		int next = (current+1)%bufferLen;
		if(next == lastStart) {
			Logger.warning("Char buffer is full. Ignoring char '"+ch+"'.");
			return;
		}
		charBuffer[current] = ch;
		current = next;
	}
	
	public static boolean hasCharSequence() {
		return lastStart != lastEnd;
	}
	
	/**
	 * Returns the last chars input by the user.
	 * @return chars typed in by the user. Calls to backspace are encrypted using '\0'.
	 */
	public static char[] getCharSequence() {
		char[] sequence = new char[(lastEnd - lastStart + bufferLen)%bufferLen];
		int index = 0;
		for(int i = lastStart; i != lastEnd; i = (i+1)%bufferLen) {
			sequence[index++] = charBuffer[i];
		}
		return sequence;
	}
	
	public static void pushKeyCode(int keyCode) {
		if(keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			pushChar('\0');
		}
		currentKeyCode = keyCode;
		hasKeyCode = true;
	}
	
	public static boolean hasKeyCode() {
		return hasKeyCode;
	}
	
	/**
	 * Resets buffers.
	 */
	public static void release() {
		releaseKeyCode();
		lastStart = lastEnd;
		lastEnd = current;
	}
	
	/**
	 * Reads key code, keeps it on buffer.
	 * @return key code
	 */
	public static int getKeyCode() {
		return currentKeyCode;
	}
	
	/**
	 * Reads key code, does not keep it on buffer.
	 * @return key code
	 */
	public static int releaseKeyCode() {
		int kc = currentKeyCode;
		currentKeyCode = 0;
		hasKeyCode = false;
		return kc;
	}
	
	public static boolean isKeyPressed(int key) {
		return pressedKeys.contains(key);
	}
	
	/**
	 * Key mods are additional control key pressed with the current key. (e.g. C is pressed with Shift+Ctrl)
	 * @return key mods
	 */
	public static int getKeyMods() {
		return keyMods;
	}
	
	public static void setKeyMods(int mods) {
		keyMods = mods;
	}
	
	public static void setKeyPressed(int key, boolean press) {
		if (press) {
			if (!pressedKeys.contains(key)) {
				pressedKeys.add(key);
			}
		} else {
			if (pressedKeys.contains(key)) {
				pressedKeys.remove((Integer) key);
			}
		}
	}

}
