/* Same original pixel algorithm, a separate exclusive output after the first
 * catalogue generation timed out. Never replaces any earlier fixture. */
#define main catalogue_main_unused
#include "generate_fixtures.c"
#undef main
int main(int argc, char **argv) {
  const Fixture fixture = {"valid_40mp_progressive_444.jpg",8000,5000,
      0,0,1,1,0,0,85,UINT32_C(0x13579bdf)};
  if (argc != 2) return 64;
  return generate_one(argv[1], &fixture);
}
