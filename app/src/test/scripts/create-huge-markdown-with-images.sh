#!/usr/bin/env bash

printf '# Huge link & image reference test\n' > links.md

printf '![Image %05d](https://picsum.photos/seed/img%05d/800/600)\n' {0..19999} {0..19999} >> links.md