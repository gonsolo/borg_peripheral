# SPDX-FileCopyrightText: © 2025 Tiny Tapeout
# SPDX-License-Identifier: Apache-2.0

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import ClockCycles
from tqv import TinyQV

PERIPHERAL_NUM = 0 

@cocotb.test()
async def test_borg_addition(dut):
    dut._log.info("Start test_borg_addition")

    # 1. Setup Clock (10MHz)
    clock = Clock(dut.clk, 100, unit="ns")
    cocotb.start_soon(clock.start())

    tqv = TinyQV(dut, PERIPHERAL_NUM)
    
    # 2. Reset
    await tqv.reset()

    # 3. Test Case: Write A, Write B, Read Result
    val_a = 0x1234
    val_b = 0xABCD
    
    dut._log.info(f"Writing A={hex(val_a)} to Addr 0 and B={hex(val_b)} to Addr 4")
    
    # Write to Operand A (Addr 0)
    await tqv.write_word_reg(0, val_a)
    # Write to Operand B (Addr 4)
    await tqv.write_word_reg(4, val_b)

    # 4. Read back the result (Addr 8)
    dut._log.info("Reading back result from Addr 8...")
    actual_sum = await tqv.read_word_reg(8)
    
    expected_sum = (val_a + val_b) & 0xFFFFFFFF
    
    dut._log.info(f"Read sum: {hex(actual_sum)}")
    dut._log.info(f"Expected: {hex(expected_sum)}")

    assert actual_sum == expected_sum, \
        f"Addition failed! Got {hex(actual_sum)}, expected {hex(expected_sum)}"

    # 5. Verify individual registers still hold their values
    read_a = await tqv.read_word_reg(0)
    assert read_a == val_a, f"Operand A corrupted! Got {hex(read_a)}"

    # 6. Stress test with multiple additions
    for i in range(10):
        a = i * 100
        b = i * 200
        await tqv.write_word_reg(0, a)
        await tqv.write_word_reg(4, b)
        
        # Read result
        res = await tqv.read_word_reg(8)
        assert res == (a + b), f"Iter {i} failed: {res} != {a+b}"

    dut._log.info("Borg Integer Addition Test Passed!")
