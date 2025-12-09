all: borg_test tt_test tt_docs
borg_test:
	mill borg.test
generate_verilog:
	mill borg.run
	mill hardfloat.run
tt_test_only:
	make -C test
tt_test: generate_verilog tt_test_only
tt_docs: generate_verilog
	python tt/tt_tool.py  --create-pdf
nix:
	nix develop --command make all
clean:
	rm -f src/*.sv
	git clean -dfx
.PHONY: all clean tt_docs generate_verilog just_test test docs nix
