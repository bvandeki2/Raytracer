/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
package raytracer;

import static org.lwjgl.BufferUtils.createByteBuffer;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.glfw.GLFW.nglfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.nglfwGetWindowSize;
import static org.lwjgl.glfw.GLFWNativeGLX.glfwGetGLXContext;
import static org.lwjgl.glfw.GLFWNativeWGL.glfwGetWGLContext;
import static org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display;
import static org.lwjgl.opencl.CL10.CL_CONTEXT_PLATFORM;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_CPU;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL10.CL_MEM_WRITE_ONLY;
import static org.lwjgl.opencl.CL10.CL_PLATFORM_VENDOR;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_LOG;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_STATUS;
import static org.lwjgl.opencl.CL10.CL_SUCCESS;
import static org.lwjgl.opencl.CL10.clBuildProgram;
import static org.lwjgl.opencl.CL10.clCreateCommandQueue;
import static org.lwjgl.opencl.CL10.clCreateContext;
import static org.lwjgl.opencl.CL10.clCreateKernel;
import static org.lwjgl.opencl.CL10.clCreateProgramWithSource;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL10.clFinish;
import static org.lwjgl.opencl.CL10.clReleaseCommandQueue;
import static org.lwjgl.opencl.CL10.clReleaseContext;
import static org.lwjgl.opencl.CL10.clReleaseEvent;
import static org.lwjgl.opencl.CL10.clReleaseKernel;
import static org.lwjgl.opencl.CL10.clReleaseMemObject;
import static org.lwjgl.opencl.CL10.clReleaseProgram;
import static org.lwjgl.opencl.CL10.clSetKernelArg1i;
import static org.lwjgl.opencl.CL10.clSetKernelArg1p;
import static org.lwjgl.opencl.CL10GL.clCreateFromGLTexture2D;
import static org.lwjgl.opencl.CL10GL.clEnqueueAcquireGLObjects;
import static org.lwjgl.opencl.CL10GL.clEnqueueReleaseGLObjects;
import static org.lwjgl.opencl.CLUtil.checkCLError;
import static org.lwjgl.opencl.Info.clGetPlatformInfoStringUTF8;
import static org.lwjgl.opencl.Info.clGetProgramBuildInfoInt;
import static org.lwjgl.opencl.Info.clGetProgramBuildInfoStringASCII;
import static org.lwjgl.opencl.KHRGLSharing.CL_GLX_DISPLAY_KHR;
import static org.lwjgl.opencl.KHRGLSharing.CL_GL_CONTEXT_KHR;
import static org.lwjgl.opencl.KHRGLSharing.CL_WGL_HDC_KHR;
import static org.lwjgl.opengl.ARBCLEvent.glCreateSyncFromCLeventARB;
import static org.lwjgl.opengl.CGL.CGLGetCurrentContext;
import static org.lwjgl.opengl.CGL.CGLGetShareGroup;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_INFO_LOG_LENGTH;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_RGBA8UI;
import static org.lwjgl.opengl.GL30.GL_RGBA_INTEGER;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL32.glDeleteSync;
import static org.lwjgl.opengl.GL32.glWaitSync;
import static org.lwjgl.opengl.WGL.wglGetCurrentDC;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memDecodeUTF8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opencl.APPLEGLSharing;
import org.lwjgl.opencl.CLCapabilities;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLPlatform.Filter;
import org.lwjgl.opencl.CLProgramCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.system.libffi.Closure;

import raytracer.Window.GLFWWindow;

public class Renderer {

	private static final ByteBuffer source;

	static {
		try {
			source = ioResourceToByteBuffer("raytracer.cl", 4096);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** The event callbacks run on the main thread. We use this queue to apply any changes in the rendering thread. */
	private final Queue<Runnable> events = new ConcurrentLinkedQueue<Runnable>();

	private final GLFWWindow window;

	private boolean shouldInitBuffers = true;
	private boolean rebuild;

	// OPENCL

	private final IntBuffer errcode_ret;

	private final CLPlatform platform;
	private final CLDevice   device;

	private final CLContextCallback clContextCB;

	private final long clContext;
	private final long clQueue;
	private       long clProgram;
	private       long clKernel;
	private       long clTexture;

	private final PointerBuffer kernel2DGlobalWorkSize = BufferUtils.createPointerBuffer(2);

	// OPENGL

	private int glTexture;

	private int vao;
	private int vbo;
	private int vsh;
	private int fsh;
	private int glProgram;

	private int projectionUniform;
	private int sizeUniform;

	// VIEWPORT

	private int ww, wh;

	private int fbw, fbh;

	// EVENT SYNCING

	private final PointerBuffer syncBuffer = BufferUtils.createPointerBuffer(1);

	private boolean syncGLtoCL; // false if we can make GL wait on events generated from CL queues.
	private long    clEvent;
	private long    glFenceFromCLEvent;

	private boolean syncCLtoGL; // false if we can make CL wait on sync objects generated from GL.

	Closure debugProc;

	public Renderer(CLPlatform platform, GLFWWindow window, int deviceType, boolean debugGL) {
		this.platform = platform;

		this.window = window;

		IntBuffer size = BufferUtils.createIntBuffer(2);

		nglfwGetWindowSize(window.handle, memAddress(size), memAddress(size) + 4);
		ww = size.get(0);
		wh = size.get(1);

		nglfwGetFramebufferSize(window.handle, memAddress(size), memAddress(size) + 4);
		fbw = size.get(0);
		fbh = size.get(1);

		glfwMakeContextCurrent(window.handle);
		GLCapabilities glCaps = GL.createCapabilities();
		if ( !glCaps.OpenGL30 )
			throw new RuntimeException("OpenGL 3.0 is required to run this demo.");

		debugProc = debugGL ? GLUtil.setupDebugMessageCallback() : null;

		glfwSwapInterval(0);

		errcode_ret = BufferUtils.createIntBuffer(1);

		try {
			// Find devices with GL sharing support
			Filter<CLDevice> glSharingFilter = new Filter<CLDevice>() {
				@Override
				public boolean accept(CLDevice device) {
					CLCapabilities caps = device.getCapabilities();
					return caps.cl_khr_gl_sharing || caps.cl_APPLE_gl_sharing;
				}
			};
			List<CLDevice> devices = platform.getDevices(deviceType, glSharingFilter);
			if ( devices == null ) {
				devices = platform.getDevices(CL_DEVICE_TYPE_CPU, glSharingFilter);
				if ( devices == null )
					throw new RuntimeException("No OpenCL devices found with KHR_gl_sharing support.");
			}
			this.device = devices.get(0);

			// Create the context
			PointerBuffer ctxProps = BufferUtils.createPointerBuffer(7);
			switch ( Platform.get() ) {
				case WINDOWS:
					ctxProps
						.put(CL_GL_CONTEXT_KHR)
						.put(glfwGetWGLContext(window.handle))
						.put(CL_WGL_HDC_KHR)
						.put(wglGetCurrentDC());
					break;
				case LINUX:
					ctxProps
						.put(CL_GL_CONTEXT_KHR)
						.put(glfwGetGLXContext(window.handle))
						.put(CL_GLX_DISPLAY_KHR)
						.put(glfwGetX11Display());
					break;
				case MACOSX:
					ctxProps
						.put(APPLEGLSharing.CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE)
						.put(CGLGetShareGroup(CGLGetCurrentContext()));
			}
			ctxProps
				.put(CL_CONTEXT_PLATFORM)
				.put(platform)
				.put(NULL)
				.flip();
			clContext = clCreateContext(ctxProps, device.address(), clContextCB = new CLContextCallback() {
				@Override
				public void invoke(long errinfo, long private_info, long cb, long user_data) {
					System.err.println("[LWJGL] cl_context_callback");
					System.err.println("\tInfo: " + memDecodeUTF8(errinfo));
				}
			}, NULL, errcode_ret);
			checkCLError(errcode_ret);

			// create command queues for every GPU and init kernels

			// create command queue and upload color map buffer
			clQueue = clCreateCommandQueue(clContext, device.address(), NULL, errcode_ret);
			checkCLError(errcode_ret);

			// load program(s)
			if ( deviceType == CL_DEVICE_TYPE_GPU )
				log("OpenCL Device Type: GPU");
			else
				log("OpenCL Device Type: CPU");

			log("OpenGL glCaps.GL_ARB_sync = " + glCaps.GL_ARB_sync);
			log("OpenGL glCaps.GL_ARB_cl_event = " + glCaps.GL_ARB_cl_event);

			buildProgram();

			// Detect GLtoCL synchronization method
			syncGLtoCL = !glCaps.GL_ARB_cl_event; // GL3.2 or ARB_sync implied
			log(syncGLtoCL
				    ? "GL to CL sync: Using clFinish"
				    : "GL to CL sync: Using OpenCL events"
			);

			// Detect CLtoGL synchronization method
			syncCLtoGL = !device.getCapabilities().cl_khr_gl_event;
			log(syncCLtoGL
				    ? "CL to GL sync: Using glFinish"
				    : "CL to GL sync: Using implicit sync (cl_khr_gl_event)"
			);

			vao = glGenVertexArrays();
			glBindVertexArray(vao);

			vbo = glGenBuffers();
			glBindBuffer(GL_ARRAY_BUFFER, vbo);
			
			FloatBuffer quad = BufferUtils.createFloatBuffer(4 * 4).put(new float[] {
				0.0f, 0.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f, 1.0f,
				1.0f, 1.0f, 1.0f, 1.0f
			});
			quad.flip();
			glBufferData(GL_ARRAY_BUFFER, quad, GL_STATIC_DRAW);

			vsh = glCreateShader(GL_VERTEX_SHADER);
			glShaderSource(vsh, "#version 150\n" +
			                    "\n" +
			                    "uniform mat4 projection;\n" +
			                    "\n" +
			                    "uniform vec2 size;\n" +
			                    "\n" +
			                    "in vec2 posIN;\n" +
			                    "in vec2 texIN;\n" +
			                    "\n" +
			                    "out vec2 texCoord;\n" +
			                    "\n" +
			                    "void main(void) {\n" +
			                    "\tgl_Position = projection * vec4(posIN * size, 0.0, 1.0);\n" +
			                    "\ttexCoord = texIN;\n" +
			                    "}");
			glCompileShader(vsh);
			String log = glGetShaderInfoLog(vsh, glGetShaderi(vsh, GL_INFO_LOG_LENGTH));
			if ( !log.isEmpty() )
				System.err.println("VERTEX SHADER LOG: " + log);

			fsh = glCreateShader(GL_FRAGMENT_SHADER);
			glShaderSource(fsh, "#version 150\n" +
			                    "\n" +
			                    "uniform isampler2D tex;\n" +
			                    "\n" +
			                    "in vec2 texCoord;\n" +
			                    "\n" +
			                    "out vec4 fragColor;\n" +
			                    "\n" +
			                    "void main(void) {\n" +
			                    "\tfragColor = texture(tex, texCoord) / 255.0;\n" +
			                    "}");
			glCompileShader(fsh);
			log = glGetShaderInfoLog(fsh, glGetShaderi(fsh, GL_INFO_LOG_LENGTH));
			if ( !log.isEmpty() )
				System.err.println("FRAGMENT SHADER LOG: " + log);

			glProgram = glCreateProgram();
			glAttachShader(glProgram, vsh);
			glAttachShader(glProgram, fsh);
			glLinkProgram(glProgram);
			log = glGetProgramInfoLog(glProgram, glGetProgrami(glProgram, GL_INFO_LOG_LENGTH));
			if ( !log.isEmpty() )
				System.err.println("PROGRAM LOG: " + log);

			int posIN = glGetAttribLocation(glProgram, "posIN");
			int texIN = glGetAttribLocation(glProgram, "texIN");

			glVertexAttribPointer(posIN, 2, GL_FLOAT, false, 4 * 4, 0);
			glVertexAttribPointer(texIN, 2, GL_FLOAT, false, 4 * 4, 2 * 4);

			glEnableVertexAttribArray(posIN);
			glEnableVertexAttribArray(texIN);

			projectionUniform = glGetUniformLocation(glProgram, "projection");
			sizeUniform = glGetUniformLocation(glProgram, "size");

			glUseProgram(glProgram);

			glUniform1i(glGetUniformLocation(glProgram, "tex"), 0);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		glDisable(GL_DEPTH_TEST);
		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

		initGLObjects();
		glFinish();

		setKernelConstants();

		glfwSetWindowSizeCallback(window.handle, window.windowsizefun = new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, final int width, final int height) {
				if ( width == 0 || height == 0 )
					return;

				events.add(new Runnable() {
					@Override
					public void run() {
						Renderer.this.ww = width;
						Renderer.this.wh = height;

						shouldInitBuffers = true;
					}
				});
			}
		});

		glfwSetFramebufferSizeCallback(window.handle, window.framebuffersizefun = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, final int width, final int height) {
				if ( width == 0 || height == 0 )
					return;

				events.add(new Runnable() {
					@Override
					public void run() {
						Renderer.this.fbw = width;
						Renderer.this.fbh = height;

						shouldInitBuffers = true;
					}
				});
			}
		});

		glfwSetKeyCallback(window.handle, window.keyfun = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				
			}
		});

		glfwSetMouseButtonCallback(window.handle, window.mousebuttonfun = new GLFWMouseButtonCallback() {
			@Override
			public void invoke(long window, int button, int action, int mods) {

			}
		});

		glfwSetCursorPosCallback(window.handle, window.cursorposfun = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double xpos, double ypos) {

			}
		});

		glfwSetScrollCallback(window.handle, window.scrollfun = new GLFWScrollCallback() {
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				
			}
		});
	}

	private void log(String msg) {
		System.err.format("[%s] %s\n", window.ID, msg);
	}

	void renderLoop() {
		long startTime = System.currentTimeMillis() + 5000;
		long fps = 0;

		while ( glfwWindowShouldClose(window.handle) == GLFW_FALSE ) {
			Runnable event;
			while ( (event = events.poll()) != null )
				event.run();

			try {
				display();
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}

			glfwSwapBuffers(window.handle);

			if ( startTime > System.currentTimeMillis() ) {
				fps++;
			} else {
				long timeUsed = 5000 + (startTime - System.currentTimeMillis());
				startTime = System.currentTimeMillis() + 5000;
				log(String.format(
					"%s: %d frames in 5 seconds = %.2f",
					clGetPlatformInfoStringUTF8(platform.address(), CL_PLATFORM_VENDOR),
					fps,
					fps / (timeUsed / 1000f)
				));
				fps = 0;
			}
		}

		cleanup();

		window.signal.countDown();
	}

	private interface CLCleaner {
		int release(long object);
	}

	private static void release(long object, CLCleaner cleaner) {
		if ( object == NULL )
			return;

		int errcode = cleaner.release(object);
		checkCLError(errcode);
	}

	private void cleanup() {
		CLCleaner memObjCleaner = new CLCleaner() {
			@Override
			public int release(long object) {
				return clReleaseMemObject(object);
			}
		};

		release(clTexture, memObjCleaner);

		release(clKernel, new CLCleaner() {
			@Override
			public int release(long object) {
				return clReleaseKernel(object);
			}
		});

		release(clProgram, new CLCleaner() {
			@Override
			public int release(long object) {
				return clReleaseProgram(object);
			}
		});

		release(clQueue, new CLCleaner() {
			@Override
			public int release(long object) {
				return clReleaseCommandQueue(object);
			}
		});

		release(clContext, new CLCleaner() {
			@Override
			public int release(long object) {
				return clReleaseContext(object);
			}
		});
		clContextCB.free();

		glDeleteProgram(glProgram);
		glDeleteShader(fsh);
		glDeleteShader(vsh);
		glDeleteBuffers(vbo);
		glDeleteVertexArrays(vao);

		if ( debugProc != null )
			debugProc.free();
	}

	private void display() {
		// make sure GL does not use our objects before we start computing
		if ( syncCLtoGL || shouldInitBuffers )
			glFinish();

		if ( shouldInitBuffers ) {
			initGLObjects();
			setKernelConstants();
		}

		if ( rebuild ) {
			buildProgram();
			setKernelConstants();
		}
		computeCL();

		renderGL();
	}

	// OpenCl

	private void computeCL() {
		kernel2DGlobalWorkSize.put(0, ww).put(1, wh);

		// start computation
		clSetKernelArg1i(clKernel, 0, ww);
		clSetKernelArg1i(clKernel, 1, wh);

		// acquire GL objects, and enqueue a kernel with a probe from the list
		int errcode = clEnqueueAcquireGLObjects(clQueue, clTexture, null, null);
		checkCLError(errcode);

		errcode = clEnqueueNDRangeKernel(clQueue, clKernel, 2,
		                                 null,
		                                 kernel2DGlobalWorkSize,
		                                 null,
		                                 null, null);
		checkCLError(errcode);

		errcode = clEnqueueReleaseGLObjects(clQueue, clTexture, null, !syncGLtoCL ? syncBuffer : null);
		checkCLError(errcode);

		if ( !syncGLtoCL ) {
			clEvent = syncBuffer.get(0);
			glFenceFromCLEvent = glCreateSyncFromCLeventARB(clContext, clEvent, 0);
		}

		// block until done (important: finish before doing further gl work)
		if ( syncGLtoCL ) {
			errcode = clFinish(clQueue);
			checkCLError(errcode);
		}
	}

	// OpenGL

	private void renderGL() {
		glClear(GL_COLOR_BUFFER_BIT);

		//draw slices

		if ( !syncGLtoCL ) {
			glWaitSync(glFenceFromCLEvent, 0, 0);
			glDeleteSync(glFenceFromCLEvent);
			glFenceFromCLEvent = NULL;

			int errcode = clReleaseEvent(clEvent);
			clEvent = NULL;
			checkCLError(errcode);
		}

		glBindTexture(GL_TEXTURE_2D, glTexture);
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	}

	private void buildProgram() {
		if ( clProgram != NULL ) {
			int errcode = clReleaseProgram(clProgram);
			checkCLError(errcode);
		}

		PointerBuffer strings = BufferUtils.createPointerBuffer(1);
		PointerBuffer lengths = BufferUtils.createPointerBuffer(1);

		strings.put(0, source);
		lengths.put(0, source.remaining());

		clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);
		checkCLError(errcode_ret);

		final CountDownLatch latch = new CountDownLatch(1);

		// disable 64bit floating point math if not available
		StringBuilder options = new StringBuilder("-D USE_TEXTURE");

		log("OpenCL COMPILER OPTIONS: " + options);
		final long cl_device_id = device.address();
		final CLProgramCallback buildCallback;
		int errcode = clBuildProgram(clProgram, cl_device_id, options, buildCallback = new CLProgramCallback() {
			@Override
			public void invoke(long program, long user_data) {
				System.err.printf(
					"The cl_program [0x%X] was built %s\n",
					program,
					clGetProgramBuildInfoInt(program, cl_device_id, CL_PROGRAM_BUILD_STATUS) == CL_SUCCESS ? "successfully" : "unsuccessfully"
				);
				String log = clGetProgramBuildInfoStringASCII(program, cl_device_id, CL_PROGRAM_BUILD_LOG);
				if ( !log.isEmpty() )
					System.err.printf("BUILD LOG:\n----\n%s\n-----\n", log);

				latch.countDown();
			}
		}, NULL);
		checkCLError(errcode);

		// Make sure the program has been built before proceeding
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		buildCallback.free();
		rebuild = false;

		// init kernel with constants
		clKernel = clCreateKernel(clProgram, "raytrace", errcode_ret);
		checkCLError(errcode_ret);
	}

	private void initGLObjects() {
		if ( clTexture != NULL ) {
			checkCLError(clReleaseMemObject(clTexture));
			glDeleteTextures(glTexture);
		}

		glTexture = glGenTextures();

		// Init textures
		glBindTexture(GL_TEXTURE_2D, glTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8UI, ww, wh, 0, GL_RGBA_INTEGER, GL_UNSIGNED_BYTE, (ByteBuffer)null);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

		clTexture = clCreateFromGLTexture2D(clContext, CL_MEM_WRITE_ONLY, GL_TEXTURE_2D, 0, glTexture, errcode_ret);
		checkCLError(errcode_ret);
		glBindTexture(GL_TEXTURE_2D, 0);

		glViewport(0, 0, fbw, fbh);

		glUniform2f(sizeUniform, ww, wh);

		FloatBuffer projectionMatrix = BufferUtils.createFloatBuffer(4 * 4);
		glOrtho(0.0f, ww, 0.0f, wh, 0.0f, 1.0f, projectionMatrix);
		glUniformMatrix4fv(projectionUniform, false, projectionMatrix);

		shouldInitBuffers = false;
	}

	private static void glOrtho(float l, float r, float b, float t, float n, float f, FloatBuffer m) {
		m.put(new float[] {
			1.0f, 0.0f, 0.0f, 0.0f,
			0.0f, 1.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 1.0f, 0.0f,
			0.0f, 0.0f, 0.0f, 1.0f,
			});
		m.flip();

		m.put(0 * 4 + 0, 2.0f / (r - l));
		m.put(1 * 4 + 1, 2.0f / (t - b));
		m.put(2 * 4 + 2, -2.0f / (f - n));

		m.put(3 * 4 + 0, -(r + l) / (r - l));
		m.put(3 * 4 + 1, -(t + b) / (t - b));
		m.put(3 * 4 + 2, -(f + n) / (f - n));
	}

	// init kernels with constants

	private void setKernelConstants() {
		clSetKernelArg1p(clKernel, 2, clTexture);
	}
	
	/**
	 * Reads the specified resource and returns the raw data as a ByteBuffer.
	 *
	 * @param resource   the resource to read
	 * @param bufferSize the initial buffer size
	 *
	 * @return the resource data
	 *
	 * @throws IOException if an IO error occurs
	 */
	public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
		ByteBuffer buffer;

		File file = new File(resource);
		if ( file.isFile() ) {
			FileInputStream fis = new FileInputStream(file);
			FileChannel fc = fis.getChannel();
			
			buffer = BufferUtils.createByteBuffer((int)fc.size() + 1);

			while ( fc.read(buffer) != -1 ) ;
			
			fis.close();
			fc.close();
		} else {
			buffer = createByteBuffer(bufferSize);

			InputStream source = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
			if ( source == null )
				throw new FileNotFoundException(resource);

			try {
				ReadableByteChannel rbc = Channels.newChannel(source);
				try {
					while ( true ) {
						int bytes = rbc.read(buffer);
						if ( bytes == -1 )
							break;
						if ( buffer.remaining() == 0 )
							buffer = resizeBuffer(buffer, buffer.capacity() * 2);
					}
				} finally {
					rbc.close();
				}
			} finally {
				source.close();
			}
		}

		buffer.flip();
		return buffer;
	}
	private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
		ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
		buffer.flip();
		newBuffer.put(buffer);
		return newBuffer;
	}
}