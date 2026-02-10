# SPDX-FileCopyrightText: © 2025-2026 Andreas Wendleder
# SPDX-License-Identifier: CERN-OHL-S-2.0

from cocotb.clock import Clock
from tqv import TinyQV
import cocotb
import json
import numpy as np
import os
import struct


def float_to_bits(f):
    return struct.unpack("<I", struct.pack("<f", f))[0]


def bits_to_float(b):
    return struct.unpack("<f", struct.pack("<I", b & 0xFFFFFFFF))[0]


def load_test_data():
    curr_dir = os.path.dirname(os.path.abspath(__file__))
    json_path = os.path.join(curr_dir, "..", "data", "test_cases.json")
    with open(json_path, "r") as f:
        return json.load(f)


class BorgDriver:
    def __init__(self, tqv):
        self.tqv = tqv
        # Address Map matches the simplified Borg.scala
        self.ADDR_A = 0
        self.ADDR_B = 4
        self.ADDR_RESULT = 8  
        self.ADDR_INSTR = 60

    async def write_reg(self, reg_idx, val_float):
        addr = reg_idx * 4
        bits = float_to_bits(np.float32(val_float))
        await self.tqv.write_word_reg(addr, bits)

    async def write_instr(self, instr_bits):
        await self.tqv.write_word_reg(self.ADDR_INSTR, instr_bits)

    async def read_result(self):
        # read_word_reg handles the polling of data_ready internally
        bits = await self.tqv.read_word_reg(self.ADDR_RESULT)
        return bits_to_float(bits)

    async def reset(self):
        await self.tqv.reset()


async def run_basic_math_test(dut, driver, a, b, epsilon):
    a_32 = np.float32(a)
    b_32 = np.float32(b)

    # 1. Upload Data
    await driver.write_reg(0, a_32)
    await driver.write_reg(1, b_32)

    # 2. Execute ADD: funct7=0x00, rs2=1, rs1=0
    # In the current Borg.scala, any instr with busy_counter triggers the adder
    instr_add = (0x00 << 25) | (1 << 20) | (0 << 15)
    await driver.write_instr(instr_add)
    
    # 3. Read Result (Wait for 4-cycle pipeline)
    add_res = await driver.read_result()

    # 4. Assertions
    expected_add = a_32 + b_32

    assert abs(add_res - expected_add) < epsilon, f"Add failed: {a_32}+{b_32}={add_res} (Exp: {expected_add})"

    dut._log.info(
        f"Checked Adder: {a_32:8.2f} + {b_32:8.2f} -> Result: {add_res:8.2f}"
    )


PERIPHERAL_NUM = 39


@cocotb.test()
async def test_borg_vulkan_style_math(dut):
    dut._log.info("Starting Single-Port Programmable Borg Adder Test")

    test_data = load_test_data()
    epsilon = test_data["epsilon"]

    clock = Clock(dut.clk, 100, unit="ns")
    cocotb.start_soon(clock.start())

    tqv = TinyQV(dut, PERIPHERAL_NUM)
    driver = BorgDriver(tqv)
    await driver.reset()

    for a, b in test_data["pairs"]:
        await run_basic_math_test(dut, driver, a, b, epsilon)

    dut._log.info("All Borg Adder Tests Passed!")
