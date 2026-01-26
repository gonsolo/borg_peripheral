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
    """
    Python driver to abstract TinyQV bus transactions into Borg-specific actions.
    Mirroring the Scala BorgDriver.
    """

    def __init__(self, tqv):
        self.tqv = tqv
        # Address Map
        self.ADDR_A = 0
        self.ADDR_B = 4
        self.ADDR_ADD = 8
        self.ADDR_MUL = 12
        self.ADDR_C = 16

    async def write_reg(self, reg_idx, val_float):
        addr = reg_idx * 4
        bits = float_to_bits(np.float32(val_float))
        await self.tqv.write_word_reg(addr, bits)

    async def read_addr(self, addr):
        bits = await self.tqv.read_word_reg(addr)
        return bits_to_float(bits)

    async def reset(self):
        await self.tqv.reset()

async def run_basic_math_test(dut, driver, a, b, epsilon):
    """
    Factored out test execution logic.
    Identical flow to the Scala runBasicMathTest.
    """
    a_32 = np.float32(a)
    b_32 = np.float32(b)

    # Phase 1: Upload Data
    await driver.write_reg(0, a_32)
    await driver.write_reg(1, b_32)

    # Phase 2: Retrieve Results
    add_res = await driver.read_addr(driver.ADDR_ADD)
    mul_res = await driver.read_addr(driver.ADDR_MUL)

    # Phase 3: Assertions
    expected_add = a_32 + b_32
    expected_mul = a_32 * b_32

    assert abs(add_res - expected_add) < epsilon, f"Add failed: {a_32}+{b_32}"
    assert abs(mul_res - expected_mul) < epsilon, f"Mul failed: {a_32}*{b_32}"

    dut._log.info(f"Checked: {a_32} & {b_32} -> Add: {add_res}, Mul: {mul_res}")


PERIPHERAL_NUM = 39

@cocotb.test()
async def test_borg_vulkan_style_math(dut):
    dut._log.info("Starting Modular Borg Test")

    # Setup environment
    test_data = load_test_data()
    epsilon = test_data["epsilon"]

    clock = Clock(dut.clk, 100, unit="ns")
    cocotb.start_soon(clock.start())

    # Initialize Driver
    tqv = TinyQV(dut, PERIPHERAL_NUM)
    driver = BorgDriver(tqv)
    await driver.reset()

    # Run batched pairs
    for a, b in test_data["pairs"]:
        await run_basic_math_test(dut, driver, a, b, epsilon)

    dut._log.info("All Modular Borg Tests Passed!")
