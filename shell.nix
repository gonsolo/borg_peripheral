{ pkgs ? import <nixpkgs> {} }:

let
  pythonEnv = pkgs.python313.withPackages (p: [
    p.cocotb
    p.pip
  ]);
in

pkgs.mkShell {
    buildInputs = [
        pkgs.gnumake
        pkgs.bash-completion
        pkgs.jdk25
        pkgs.sbt

        pythonEnv
        pkgs.iverilog
    ];
}
