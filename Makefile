TT_TOOL=python tt/tt_tool.py
NIX=nix develop --ignore-environment --command
BORG_TEST=mill --no-server borg.test

all: arch_borg_test tt_test tt_docs tt_gds
arch_borg_test:
	$(BORG_TEST)
nix_borg_test:
	$(NIX) $(BORG_TEST)
generate_verilog:
	mill borg.run
tt_test_only:
	make -C test
tt_test: generate_verilog tt_test_only
tt_docs: generate_verilog
	$(TT_TOOL) --create-pdf
tt_gds:
	$(TT_TOOL) --create-user-config --ihp --no-docker
	$(TT_TOOL) --harden --ihp --no-docker
nix:
	$(NIX) make all
clean:
	git clean -dfx
print_stats:
	./tt/tt_tool.py --print-stats
.PHONY: all borg_test clean docs generate_verilog just_test nix print_stats test tt_docs tt_gds \
	tt_test tt_test_only
