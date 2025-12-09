all: borg_test
borg_test:
	mill borg.test
generate_verilog:
	mill borg.run
#just_tt_test:
#	make -C test
#tt_test: generate_verilog just_test
tt_docs:
	python tt/tt_tool.py  --create-pdf
nix:
	nix develop --command make borg_test
clean:
	rm -f src/*.sv
	git clean -dfx
.PHONY: all clean tt_docs generate_verilog just_test test docs nix
