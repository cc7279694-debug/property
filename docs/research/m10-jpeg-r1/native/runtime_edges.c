/* Isolated supervisor/load diagnostics, never a JPEG integrity test or JNI. */
#include <dlfcn.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char **argv) {
  if (argc == 2 && !strcmp(argv[1], "--abort")) abort();
  if (argc == 2 && !strcmp(argv[1], "--segv")) { raise(SIGSEGV); return 99; }
  if (argc < 3) return 64;
  void *handle = dlopen(argv[2], RTLD_NOW | RTLD_LOCAL);
  if (!handle) {
    fprintf(stderr, "LOAD_ERROR: %s\n", dlerror());
    return 50;
  }
  dlerror();
  const char *symbol = !strcmp(argv[1], "--missing-symbol") ? "r1_intentionally_absent_symbol" : "main";
  void *address = dlsym(handle, symbol);
  const char *error = dlerror();
  if (error || !address) {
    fprintf(stderr, "SYMBOL_ERROR: %s\n", error ? error : "null symbol");
    dlclose(handle);
    return 51;
  }
  if (!strcmp(argv[1], "--invoke")) {
    /* POSIX Android dlsym function conversion, same standalone C entry point.
       No VM, JNI, Android Bitmap, Repository or user record is involved. */
    int (*entry)(int, char **) = (int (*)(int, char **))address;
    int code = entry(argc - 2, argv + 2);
    if (dlclose(handle) != 0) return 52;
    return code;
  }
  if (dlclose(handle) != 0) return 52;
  puts("SHARED_LOAD_SYMBOL_AND_CLOSE_OK_NOT_JNI");
  return 0;
}
