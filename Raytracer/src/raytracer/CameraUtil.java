package raytracer;

import java.nio.FloatBuffer;

public class CameraUtil {
	
	/**
	 * Vector utility class
	 * Everything should be pretty simple to use, just standard vector math implementations
	 * @author bvandeki2
	 */
	public final static class Vector3 {
		public final float x, y, z;
		
		public Vector3(final float x, final float y, final float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public static float dot(final Vector3 a, final Vector3 b) {
			return (a.x * b.x + a.y * b.y + a.z * b.z);
		}
		
		public static Vector3 cross(final Vector3 a, final Vector3 b) {
			return (new Vector3(
					a.y * b.z - a.z * b.y,
					a.z * b.x - a.x - b.z,
					a.x * b.y - a.y - b.x));
		}
		
		public float length() {
			return ((float) Math.sqrt(x * x + y * y + z * z));
		}
		
		public Vector3 normalize() {
			float invlen = 1.0f / length();
			return (new Vector3(invlen * x, invlen * y, invlen * z));
		}
	}
	
	/**
	 * <p>Fill a 4x4 matrix mat (in the form of a size 16 {@code java.nio.FloatBuffer} 
	 * with the right values for a camera with the specified location, look point and up vector
	 * The code is based off of the DirectX documentation of LookAt method</p>
	 */
	 public static void lookAt(
			 float camx, float camy, float camz,
			 float lookx, float looky, float lookz,
			 float xup, float yup, float zup,
			 FloatBuffer mat) {
		 assert mat.capacity() == 16;
		 
	 }
}
