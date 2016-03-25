/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
package raytracer;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_DEBUG_CONTEXT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_ALL;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_CPU;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL10.CL_PLATFORM_VENDOR;
import static org.lwjgl.opencl.Info.clGetDeviceInfoLong;
import static org.lwjgl.opencl.Info.clGetPlatformInfoStringASCII;
import static org.lwjgl.opencl.Info.clGetPlatformInfoStringUTF8;
import static org.lwjgl.opencl.KHRICD.CL_PLATFORM_ICD_SUFFIX_KHR;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CLCapabilities;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLPlatform.Filter;
import org.lwjgl.system.Platform;

/*
		THIS DEMO USES CODE PORTED FROM JogAmp.org
		Original code: http://github.com/mbien/jocl-demos
		Original author: Michael Bien

   ___         ___                      ___
	  /  /\       /  /\         ___        /  /\    http://jocl.jogamp.org/
	 /  /:/      /  /::\       /__/\      /  /::\   a http://jogamp.org/ project.
	/__/::\     /  /:/\:\      \  \:\    /  /:/\:\
	\__\/\:\   /  /:/~/::\      \  \:\  /  /:/~/::\
	   \  \:\ /__/:/ /:/\:\ ___  \__\:\/__/:/ /:/\:\
		\__\:\\  \:\/:/__\//__/\ |  |:|\  \:\/:/__\/
		/  /:/ \  \::/     \  \:\|  |:| \  \::/
	   /__/:/   \  \:\      \  \:\__|:|  \  \:\
	   \__\/     \  \:\      \__\::::/    \  \:\
				  \__\/          ~~~~      \__\/
			   ___          ___       ___          ___          ___
			  /  /\        /  /\     /  /\        /__/\        /  /\
			 /  /::\      /  /::\   /  /:/_       \  \:\      /  /:/
			/  /:/\:\    /  /:/\:\ /  /:/ /\       \  \:\    /  /:/      ___     ___
		   /  /:/  \:\  /  /:/~/://  /:/ /:/_  _____\__\:\  /  /:/  ___ /__/\   /  /\
		  /__/:/ \__\:\/__/:/ /://__/:/ /:/ /\/__/::::::::\/__/:/  /  /\\  \:\ /  /:/
		  \  \:\ /  /:/\  \:\/:/ \  \:\/:/ /:/\  \:\~~\~~\/\  \:\ /  /:/ \  \:\  /:/
		   \  \:\  /:/  \  \::/   \  \::/ /:/  \  \:\  ~~~  \  \:\  /:/   \  \:\/:/
			\  \:\/:/    \  \:\    \  \:\/:/    \  \:\       \  \:\/:/     \  \::/
			 \  \::/      \  \:\    \  \::/      \  \:\       \  \::/       \__\/
			  \__\/        \__\/     \__\/        \__\/        \__\/

		 _____          ___           ___           ___           ___
		/  /::\        /  /\         /__/\         /  /\         /  /\
	   /  /:/\:\      /  /:/_       |  |::\       /  /::\       /  /:/_
	  /  /:/  \:\    /  /:/ /\      |  |:|:\     /  /:/\:\     /  /:/ /\
	 /__/:/ \__\:|  /  /:/ /:/_   __|__|:|\:\   /  /:/  \:\   /  /:/ /::\
	 \  \:\ /  /:/ /__/:/ /:/ /\ /__/::::| \:\ /__/:/ \__\:\ /__/:/ /:/\:\
	  \  \:\  /:/  \  \:\/:/ /:/ \  \:\~~\__\/ \  \:\ /  /:/ \  \:\/:/~/:/
	   \  \:\/:/    \  \::/ /:/   \  \:\        \  \:\  /:/   \  \::/ /:/
		\  \::/      \  \:\/:/     \  \:\        \  \:\/:/     \__\/ /:/
		 \__\/        \  \::/       \  \:\        \  \::/        /__/:/
					   \__\/         \__\/         \__\/         \__\/
*/

public final class Window {

	private static final Set<String> params = new HashSet<String>(8);

	private static int initWidth  = 512;
	private static int initHeight = 512;

	private Window() {
	}

	public static void main(String... args) {
		GLFWErrorCallback errorfun;
		glfwSetErrorCallback(errorfun = GLFWErrorCallback.createPrint());
		if ( glfwInit() != GLFW_TRUE ) {
			System.out.println("Unable to initialize glfw");
			System.exit(-1);
		}

		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		if ( Platform.get() == Platform.MACOSX ) {
			glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
			glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		} else
			glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
		final boolean debugGL = params.contains("debugGL");
		if ( debugGL )
			glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);

		List<CLPlatform> platforms = CLPlatform.getPlatforms(new Filter<CLPlatform>() {
			@Override
			public boolean accept(CLPlatform platform) {
				CLCapabilities caps = platform.getCapabilities();
				return caps.cl_khr_gl_sharing || caps.cl_APPLE_gl_sharing;
			}
		});

		if ( platforms.isEmpty() )
			throw new IllegalStateException("No OpenCL platform found that supports OpenGL context sharing.");

		Collections.sort(platforms, new Comparator<CLPlatform>() {
			@Override
			public int compare(CLPlatform p1, CLPlatform p2) {
				// Prefer platforms that support GPU devices
				boolean gpu1 = !p1.getDevices(CL_DEVICE_TYPE_GPU).isEmpty();
				boolean gpu2 = !p2.getDevices(CL_DEVICE_TYPE_GPU).isEmpty();
				int cmp = gpu1 == gpu2 ? 0 : (gpu1 ? -1 : 1);
				if ( cmp != 0 )
					return cmp;

				return clGetPlatformInfoStringUTF8(p1.address(), CL_PLATFORM_VENDOR).compareTo(
					clGetPlatformInfoStringUTF8(p1.address(), CL_PLATFORM_VENDOR)
				);
			}
		});

		final CLPlatform platform = platforms.get(0);

		String platformID;
		if ( platform.getCapabilities().cl_khr_icd )
			platformID = clGetPlatformInfoStringASCII(platform.address(), CL_PLATFORM_ICD_SUFFIX_KHR); // less spammy
		else
			platformID = clGetPlatformInfoStringUTF8(platform.address(), CL_PLATFORM_VENDOR);

		boolean hasCPU = false;
		boolean hasGPU = false;
		for ( CLDevice device : platform.getDevices(CL_DEVICE_TYPE_ALL) ) {
			long type = clGetDeviceInfoLong(device.address(), CL_DEVICE_TYPE);
			if ( type == CL_DEVICE_TYPE_CPU )
				hasCPU = true;
			else if ( type == CL_DEVICE_TYPE_GPU )
				hasGPU = true;
		}

		Thread[] threads = new Thread[hasCPU && hasGPU ? 2 : 1];
		GLFWWindow[] windows = new GLFWWindow[threads.length];

		final CountDownLatch latch = new CountDownLatch(windows.length);
		final CyclicBarrier barrier = new CyclicBarrier(windows.length + 1);

		for ( int i = 0; i < threads.length; i++ ) {
			final int deviceType = i == 1 || !hasGPU ? CL_DEVICE_TYPE_CPU : CL_DEVICE_TYPE_GPU;

			String ID = platformID + " - " + (deviceType == CL_DEVICE_TYPE_CPU ? "CPU" : "GPU");
			final GLFWWindow window = new GLFWWindow(glfwCreateWindow(initWidth, initHeight, ID, NULL, NULL), ID, new CountDownLatch(1));
			glfwSetWindowPos(window.handle, 200 + initWidth * i + 32 * i, 200);

			windows[i] = window;
			threads[i] = new Thread(platformID) {
				@Override
				public void run() {
					Renderer demo = null;
					try {
						demo = new Renderer(platform, window, deviceType, debugGL);
					} catch (Exception e) {
						e.printStackTrace();
					}

					try {
						barrier.await();
						if ( demo != null )
							demo.renderLoop();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						latch.countDown();
					}
				}
			};
			threads[i].start();
		}

		try {
			barrier.await();
		} catch (Exception e) {
			e.printStackTrace();
		}

		for ( int i = 0; i < windows.length; i++ )
			glfwShowWindow(windows[i].handle);

		System.out.println("GAME ON!");

		while ( latch.getCount() != 0 ) {
			glfwPollEvents();

			for ( int i = 0; i < windows.length; i++ ) {
				if ( windows[i] != null && windows[i].signal.getCount() == 0 ) {
					windows[i].destroy();
					windows[i] = null;
				}
			}
		}

		CL.destroy();
		glfwTerminate();
		errorfun.free();

		System.out.println("GAME OVER!");
	}

	static class GLFWWindow {

		final long handle;

		final String ID;

		/** Used to signal that the rendering thread has completed. */
		final CountDownLatch signal;

		GLFWWindowSizeCallback      windowsizefun;
		GLFWFramebufferSizeCallback framebuffersizefun;
		GLFWKeyCallback             keyfun;
		GLFWMouseButtonCallback     mousebuttonfun;
		GLFWCursorPosCallback       cursorposfun;
		GLFWScrollCallback          scrollfun;

		private GLFWWindow(long handle, String ID, CountDownLatch signal) {
			this.handle = handle;
			this.ID = ID;
			this.signal = signal;
		}

		void destroy() {
			Callbacks.glfwFreeCallbacks(handle);
			glfwDestroyWindow(handle);
		}

	}

}