all: test docs
generate_verilog:
	mill borg.run
just_test:
	make -C test
test: generate_verilog just_test
docs:
	python3 tt/tt_tool.py  --create-pdf
nix:
	nix develop --command make all
clean:
	rm -f src/*.sv
.PHONY: all clean docs generate_verilog just_test test docs nix
