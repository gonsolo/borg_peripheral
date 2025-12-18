# SPDX-FileCopyrightText: © 2025 Tiny Tapeout
# SPDX-License-Identifier: Apache-2.0

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import ClockCycles
from tqv import TinyQV

# Ensure this matches your peripheral index (you mentioned 23 or 39 earlier)
PERIPHERAL_NUM = 0 

@cocotb.test()
async def test_borg_simple_increment(dut):
    dut._log.info("Start test_borg_simple_increment")

    # 1. Setup Clock (100ns = 10MHz)
    clock = Clock(dut.clk, 100, unit="ns")
    cocotb.start_soon(clock.start())

    tqv = TinyQV(dut, PERIPHERAL_NUM)
    
    # 2. Reset the TinyQV system
    # This will trigger the (!rst_n) logic to reset your Chisel registers
    await tqv.reset()

    # 3. Test Case: Write value, Read back (Value + 1)
    # address=0 corresponds to ADDR_A in your simple Borg.scala
    test_value = 0x12345678
    dut._log.info(f"Writing {hex(test_value)} to Address 0")
    await tqv.write_word_reg(0, test_value)

    # 4. Read back the result
    # In Borg.scala: val result_to_read = RegNext(reg_value + 1.U)
    # TinyQV.read_word_reg waits for data_ready (which is also RegNext(is_reading))
    dut._log.info("Reading back result from Address 0...")
    actual_value = await tqv.read_word_reg(0)
    
    expected_value = (test_value + 1) & 0xFFFFFFFF
    
    dut._log.info(f"Read value: {hex(actual_value)}")
    dut._log.info(f"Expected:   {hex(expected_value)}")

    assert actual_value == expected_value, \
        f"Increment failed! Got {hex(actual_value)}, expected {hex(expected_value)}"

    # 5. Multiple Iterations to verify pipeline/reset stability
    for i in range(5):
        val = i * 0x100
        await tqv.write_word_reg(0, val)
        res = await tqv.read_word_reg(0)
        assert res == (val + 1), f"Iteration {i} failed: {hex(res)} != {hex(val+1)}"

    dut._log.info("Borg Simple Increment Test Passed!")
