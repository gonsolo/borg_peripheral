{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
        buildInputs = [
                pkgs.gnumake
                pkgs.bash-completion
                pkgs.jdk25
                pkgs.sbt
        ];
    
        shellHook = ''
                local completion_path="${pkgs.bash-completion}/etc/profile.d/bash_completion.sh"
      
                if [ -f "$completion_path" ]; then
                        source "$completion_path"
                        echo "✅ Bash completion enabled."
                fi
        '';
}
