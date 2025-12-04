TEST=.github/workflows/test.yaml
DOCS=.github/workflows/docs.yaml
GDS=.github/workflows/gds.yaml
#PLATFORM=ubuntu-24.04=catthehacker/ubuntu:act-24.04
PLATFORM=ubuntu-24.04=-self-hosted
#SECRET=GITHUB_TOKEN=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
ENV=ACTIONS_RUNTIME_TOKEN=12345
ARTIFACT=/tmp/artifacts

all: test docs
generate_verilog:
	sbt "runMain tinygpu.Main"
just_test:
	make -C test
test: generate_verilog just_test
docs:
	tt/tt_tool.py  --create-pdf
nix:
	nix-shell --pure --run 'make docs'

.PHONY: all docs generate_verilog just_test nix test
