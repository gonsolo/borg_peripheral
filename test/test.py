# SPDX-FileCopyrightText: © 2025 Tiny Tapeout
# SPDX-License-Identifier: Apache-2.0

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import ClockCycles

from tqv import TinyQV

PERIPHERAL_NUM = 0

@cocotb.test()
async def test_simple_integer_adder(dut):
    dut._log.info("Start test_simple_integer_adder (123 + 456)")

    clock = Clock(dut.clk, 100, unit="ns")
    cocotb.start_soon(clock.start())

    tqv = TinyQV(dut, PERIPHERAL_NUM)
    await tqv.reset()

    dut._log.info("Testing simple addition: 123 + 456")

    OPERAND_A = 123
    OPERAND_B = 456
    EXPECTED_SUM = OPERAND_A + OPERAND_B  # 579

    dut._log.info(f"A = {OPERAND_A}, B = {OPERAND_B}")
    dut._log.info(f"Expected Sum = {EXPECTED_SUM}")

    await tqv.write_word_reg(0, OPERAND_A)
    assert await tqv.read_word_reg(0) == OPERAND_A

    await tqv.write_word_reg(4, OPERAND_B)
    assert await tqv.read_word_reg(4) == OPERAND_B

    await ClockCycles(dut.clk, 1)

    ACTUAL_SUM = await tqv.read_word_reg(8)

    dut._log.info(f"Actual Sum Read: {ACTUAL_SUM}")

    assert ACTUAL_SUM == EXPECTED_SUM, f"Addition failed: Expected {EXPECTED_SUM}, got {ACTUAL_SUM}"

    dut._log.info("Simple Integer Adder Test Passed!")
