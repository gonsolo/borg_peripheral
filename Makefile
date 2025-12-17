TT_TOOL=python tt/tt_tool.py

all: borg_test tt_test tt_docs tt_gds
borg_test:
	mill borg.test
generate_verilog:
	mill borg.run
tt_test_only:
	make -C test
tt_test: generate_verilog tt_test_only
tt_docs: generate_verilog
	$(TT_TOOL) --create-pdf
tt_gds:
	$(TT_TOOL) --create-user-config --ihp
	$(TT_TOOL) --harden --ihp
nix:
	nix develop --command make tt_gds #borg_test tt_test tt_docs
clean:
	git clean -dfx
print_stats:
	./tt/tt_tool.py --print-stats
.PHONY: all borg_test clean docs generate_verilog just_test nix print_stats test tt_docs tt_gds \
	tt_test tt_test_only
