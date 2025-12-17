![](../../workflows/gds/badge.svg) ![](../../workflows/docs/badge.svg) ![](../../workflows/test/badge.svg) ![](../../workflows/fpga/badge.svg)

# Borg Peripheral

This is a full peripheral for TinyQV intended to be taped out with
Tinytapeout. Right now it implements a floating point adder.

## Working

* Mill test (Arch, Nix)
* TT test, docs (Arch, Github, Nix)
* TT gds (Arch, Github, Nix)
* Chisel floating point addition + test

## TODO

* Chisel floating point multiplication + test
* Pull request for ttsky25a-tinyQV
* Test there
* When pico-ice arrives: FPGA testing
* Submit for next shuttle
