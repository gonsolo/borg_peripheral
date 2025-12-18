# SPDX-FileCopyrightText: © 2025 Tiny Tapeout
# SPDX-License-Identifier: Apache-2.0

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import ClockCycles
from tqv import TinyQV
import struct

# Helper to convert a Python float to a 32-bit integer bit-pattern (IEEE-754)
def float_to_bits(f):
    return struct.unpack('<I', struct.pack('<f', f))[0]

# Helper to convert a 32-bit integer bit-pattern back to a Python float
def bits_to_float(b):
    return struct.unpack('<f', struct.pack('<I', b & 0xFFFFFFFF))[0]

PERIPHERAL_NUM = 0  # Adjust if necessary

@cocotb.test()
async def test_borg_float_addition(dut):
    dut._log.info("Start test_borg_float_addition")

    # 1. Setup Clock (10MHz)
    clock = Clock(dut.clk, 100, unit="ns")
    cocotb.start_soon(clock.start())

    tqv = TinyQV(dut, PERIPHERAL_NUM)
    
    # 2. Reset
    await tqv.reset()

    # 3. Test Case: 1.5 + 2.75 = 4.25
    val_a = 1.5
    val_b = 2.75
    
    bits_a = float_to_bits(val_a)
    bits_b = float_to_bits(val_b)
    
    dut._log.info(f"Writing A={val_a} ({hex(bits_a)}) and B={val_b} ({hex(bits_b)})")
    
    # Write to Operand A (Addr 0)
    await tqv.write_word_reg(0, bits_a)
    # Write to Operand B (Addr 4)
    await tqv.write_word_reg(4, bits_b)

    # 4. Read back the result (Addr 8)
    dut._log.info("Reading back result from Addr 8...")
    raw_res = await tqv.read_word_reg(8)
    actual_sum = bits_to_float(raw_res)
    
    expected_sum = val_a + val_b
    
    dut._log.info(f"Read bits: {hex(raw_res)}")
    dut._log.info(f"Interpreted sum: {actual_sum}")
    dut._log.info(f"Expected sum: {expected_sum}")

    # Use math.isclose or a small epsilon for float comparisons
    assert abs(actual_sum - expected_sum) < 1e-6, \
        f"Float addition failed! Got {actual_sum}, expected {expected_sum}"

    # 5. Stress test with multiple additions
    test_pairs = [
        (10.0, 20.0),
        (0.1, 0.2),
        (-5.5, 2.25),
        (100.0, 0.0),
        (1.23e-2, 4.56e-2)
    ]

    for a, b in test_pairs:
        await tqv.write_word_reg(0, float_to_bits(a))
        await tqv.write_word_reg(4, float_to_bits(b))
        
        raw_res = await tqv.read_word_reg(8)
        res = bits_to_float(raw_res)
        
        # Checking with tolerance because floating point math is tricky
        assert abs(res - (a + b)) < 1e-6, f"Failed: {a} + {b} = {res}"
        dut._log.info(f"Passed: {a} + {b} = {res}")

    dut._log.info("Borg Floating Point Addition Test Passed!")
