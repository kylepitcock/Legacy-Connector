package legacyconnector.v1_12_2.org.lwjgl.opengl;

/**
 * The capabilities of the (always modern) context, as LWJGL 2 code queries them: everything the core profile has, except shaders.
 * Legacy GLSL targets the fixed-function pipeline and cannot run on the core profile, so shader objects are virtual (GlCompat) and
 * the honest answer is "no shader support": mods then draw their fixed-function fallbacks instead of effects that would be invisible.
 */
public class ContextCapabilities {
    public final boolean OpenGL11 = true, OpenGL12 = true, OpenGL13 = true, OpenGL14 = true, OpenGL15 = true, OpenGL20 = false,
            OpenGL21 = false, OpenGL30 = true, OpenGL31 = true, OpenGL32 = true, OpenGL33 = true, OpenGL40 = true, OpenGL41 = true,
            OpenGL42 = true, OpenGL43 = true, OpenGL44 = false, OpenGL45 = false;
    public final boolean GL_ARB_multitexture = true, GL_ARB_texture_non_power_of_two = true, GL_ARB_vertex_buffer_object = true,
            GL_ARB_framebuffer_object = true, GL_EXT_framebuffer_object = true, GL_ARB_shader_objects = false,
            GL_ARB_vertex_shader = false, GL_ARB_fragment_shader = false, GL_ARB_shading_language_100 = false,
            GL_ARB_occlusion_query = true, GL_ARB_texture_cube_map = true, GL_ARB_texture_env_combine = true,
            GL_ARB_texture_env_crossbar = false, GL_ARB_vertex_program = false, GL_ARB_fragment_program = false,
            GL_ARB_imaging = false, GL_ARB_depth_texture = true, GL_ARB_shadow = true, GL_ARB_point_sprite = true,
            GL_ARB_pixel_buffer_object = true, GL_ARB_vertex_array_object = false, GL_ARB_texture_float = true,
            GL_ARB_texture_rg = true, GL_ARB_draw_buffers = true, GL_ARB_geometry_shader4 = false, GL_ARB_instanced_arrays = false,
            GL_ARB_map_buffer_range = true, GL_ARB_uniform_buffer_object = true, GL_ARB_compatibility = false;
    public final boolean GL_EXT_blend_func_separate = true, GL_EXT_blend_minmax = true, GL_EXT_blend_subtract = true,
            GL_EXT_blend_color = true, GL_EXT_texture_filter_anisotropic = true, GL_EXT_texture_compression_s3tc = true,
            GL_EXT_packed_depth_stencil = true, GL_EXT_stencil_two_side = true, GL_EXT_stencil_wrap = true,
            GL_EXT_texture_env_combine = true, GL_EXT_secondary_color = true, GL_EXT_fog_coord = true,
            GL_EXT_gpu_shader4 = false, GL_EXT_texture_lod_bias = true, GL_EXT_framebuffer_blit = true,
            GL_EXT_framebuffer_multisample = true, GL_EXT_bgra = true, GL_EXT_abgr = false, GL_EXT_compiled_vertex_array = false;
    public final boolean GL_NV_fog_distance = false, GL_NV_texture_rectangle = false, GL_ATI_texture_float = false,
            GL_KHR_debug = true, GL_AMD_debug_output = false, GL_ARB_debug_output = true;
}
