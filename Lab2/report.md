# Report Lab 2

## How our exploit works

Our exploit overwrites the return address of `add_alias` to point towards a nopslide which runs into shellcode that opens up a shell with root access. The shell has root access due to that addhost that runs the exploit has the setuid bit set and root is owner.

## How we came up with our exploit

We constructed a string in the python script `exploit.py` which adds a nopslide, the exploit, and an address together. The goal was for our string to overwrite the return address of the function `add_alias` when it used our malicious string in its buffer. The reason we are able to overwrite the return address by using this buffer is because the function used to fill the buffer, `sprintf`, doesn't limit the amount of character written to the buffer, in contrast to `sprintf`.

In order to find out how long our malicious string needed to be in order for the last character to overwrite the return address we ran the program in gdb with the alphabet as our input argument instead of of malicious string. We could then use "info frame" in order to check which character had overwritten the return address, allowing us to know the needed length of our malicious string. This length turned out to be 261 characters. Since the shellcode is 75 characters, we used 185 characters for the nopslide, and the final character was the address we overwrote the return address with.

Since our malicious string is read into the buffer, it will exist in both args and local variables in the memory. We also used gdb to find out where the local variables are so that we could overwrite the return address with the address pointing towards local variables. Due to environmental differences caused by gdb and ssh we tried both higher and lower values than what gdb said. We ended up with 0xbffffa80 for ssh and 0xbffff9b4 for running locally in the vm.

## How to run the exploit
run 
```bash
addhostalias "$(python exploit.py)" a a
```

## Screenshots
Using gdb in order to find the memory address of locals.

![alt text](image.png)

Running the exploit

![alt text](image-1.png)