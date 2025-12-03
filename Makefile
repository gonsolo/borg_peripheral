TEST=.github/workflows/test.yaml
DOCS=.github/workflows/docs.yaml
GDS=.github/workflows/gds.yaml
#PLATFORM=ubuntu-24.04=catthehacker/ubuntu:act-24.04
PLATFORM=ubuntu-24.04=-self-hosted
#SECRET=GITHUB_TOKEN=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
ENV=ACTIONS_RUNTIME_TOKEN=12345
ARTIFACT=/tmp/artifacts

all: test
#run_workflow_test:
#	act --workflows $(TEST) --platform $(PLATFORM) --env $(ENV) --artifact-server-path $(ARTIFACT)
#run_workflow_doc:
#	act --workflows $(DOCS) --platform $(PLATFORM) --env $(ENV) --artifact-server-path $(ARTIFACT)
# Not working because of a bug in act
#run_workflow_gds:
#	act --workflows $(GDS) --platform $(PLATFORM) --env $(ENV) --artifact-server-path $(ARTIFACT)
docs:
	tt/tt_tool.py  --create-pdf
generate_verilog:
	sbt "runMain tinygpu.Main"
test: generate_verilog just_test
just_test:
	make -C test
nix:
	nix-shell --pure --run 'make -C test'

.PHONY: all docs generate_verilog just_test nix test
