{
  # 1. Inputs: Define the sources for dependencies
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable"; 
  };
  # ... (rest of the file)

  # 2. Outputs: Define what this flake provides
  outputs = { self, nixpkgs }:
    let
      # Define the system architecture
      system = "x86_64-linux";

      # Access the specific nixpkgs instance for the defined system
      pkgs = nixpkgs.legacyPackages.${system};

      # 4. Define the complete Python environment
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
      ]);

    in {
      # 5. Define the development shell
      devShells.${system}.default = pkgs.mkShell {
        # Core tools and the Python environment
        buildInputs = [
          pkgs.gnumake
          pkgs.bash-completion
          pkgs.jdk25
          pkgs.sbt
          pkgs.cmake # Needed for running manual CMake if necessary
          pkgs.iverilog
          pythonEnv
        ];
        
        # Optional: Set a prompt prefix
        shellHook = ''
          export GONSOLO_PROJECT="TinyQV-GPU"
          echo "Entering $GONSOLO_PROJECT development shell..."
        '';
      };
    };
}
