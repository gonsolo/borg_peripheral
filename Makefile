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
tt_gds:
	python tt/tt_tool.py --create-user-config --ihp
nix:
	nix develop --command make all
clean:
	rm -f src/*.sv
	git clean -dfx
.PHONY: all borg_test clean generate_verilog just_test test tt_docs tt_gds tt_test tt_test_only \
	docs nix
