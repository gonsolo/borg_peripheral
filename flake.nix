{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/5d78ec6766f99525b239b5c005972044aa4dae34";
    alejandra.url = "github:kamadorueda/alejandra/4.0.0";
  };

  outputs = {
    self,
    nixpkgs,
    alejandra,
  }: let
    system = "x86_64-linux";
    pkgs = nixpkgs.legacyPackages.${system};

    pythonEnv = pkgs.python313.withPackages (p: [
      p.cairosvg
      p.cocotb
      p.chevron
      p.gitpython
      p.klayout
      p.matplotlib
      p.mistune
      p.pip
      p.pyaml
      p.requests
      p.pytest
      p.gdstk
    ]);
  in {
    devShells.${system}.default = pkgs.mkShell {
      # Use nativeBuildInputs for tools that provide executables
      nativeBuildInputs = [
        pkgs.bash-completion
        pkgs.bzip2
        pkgs.cmake
        pkgs.coreutils
        pkgs.gcc
        pkgs.git
        pkgs.gnugrep
        pkgs.gnumake
        pkgs.gnused
        pkgs.iverilog
        pkgs.jdk25
        pkgs.librelane
        pkgs.mill
        pkgs.pkg-config
        pkgs.verilator
        pkgs.which
        pythonEnv
      ];

      shellHook = ''
        export GONSOLO_PROJECT="borg_peripheral"

        # PURE MODE COMPATIBILITY:
        # 1. Mill/Java require a HOME to write lockfiles and caches.
        # If we are in --ignore-environment, HOME is empty.
        if [ -z "$HOME" ] || [ "$HOME" = "/" ]; then
          export HOME=$(pwd)/.nix-home
          mkdir -p $HOME
          echo "Notice: Pure mode detected. Using local $HOME for caches."
        fi

        # 2. Point to the JDK25 home so Java apps don't have to search the PATH
        export JAVA_HOME=${pkgs.jdk25}

        echo "Entering $GONSOLO_PROJECT development shell..."
      '';
    };

    formatter.${system} = alejandra.defaultPackage.${system};
  };
}
