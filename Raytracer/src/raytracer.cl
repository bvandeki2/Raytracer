__kernel void raytrace(
        const int width, const int height,
        __write_only image2d_t output
) {
    unsigned int ix = get_global_id(0);
    unsigned int iy = get_global_id(1);

	write_imageui(output, (int2)(ix, iy), (uint4)(0xFF,0xFF,0x00,0xFF));
}