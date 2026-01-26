# SPDX-FileCopyrightText: © 2025 Andreas Wendleder
# SPDX-License-Identifier: CERN-OHL-S-2.0

import cocotb
from cocotb.clock import Clock
from tqv import TinyQV
import struct
import json
import os
import numpy as np

def float_to_bits(f):
    return struct.unpack('<I', struct.pack('<f', f))[0]

def bits_to_float(b):
    return struct.unpack('<f', struct.pack('<I', b & 0xFFFFFFFF))[0]

# Load shared test cases
def load_test_data():
    curr_dir = os.path.dirname(os.path.abspath(__file__))
    json_path = os.path.join(curr_dir, "..", "data", "test_cases.json")
    with open(json_path, "r") as f:
        return json.load(f)

PERIPHERAL_NUM = 39

@cocotb.test()
async def test_borg_float_addition_and_multiplication(dut):
    dut._log.info("Starting Borg Floating Point Addition Test")
    
    test_data = load_test_data()
    test_pairs = test_data["pairs"]
    EPSILON = test_data["epsilon"]

    clock = Clock(dut.clk, 100, unit="ns")
    cocotb.start_soon(clock.start())
    tqv = TinyQV(dut, PERIPHERAL_NUM)
    await tqv.reset()

    ADDR_A, ADDR_B, ADDR_ADD, ADDR_MUL = 0, 4, 8, 12

    for a, b in test_pairs:
        # Cast to float32 to match hardware precision logic
        a_32 = np.float32(a)
        b_32 = np.float32(b)
        
        # Write Operands
        await tqv.write_word_reg(ADDR_A, float_to_bits(a_32))
        await tqv.write_word_reg(ADDR_B, float_to_bits(b_32))
        
        # Calculate expected values using float32 arithmetic
        expected_add = a_32 + b_32
        expected_mul = a_32 * b_32
        
        # Read and Check Addition
        add_res_bits = await tqv.read_word_reg(ADDR_ADD)
        add_res = bits_to_float(add_res_bits)
        
        # Use np.isclose or keep your abs() check with the corrected expected value
        assert abs(add_res - expected_add) < EPSILON, \
            f"Add failed: {a_32} + {b_32} = {add_res} (Expected {expected_add})"
        
        # Read and Check Multiplication
        mul_res_bits = await tqv.read_word_reg(ADDR_MUL)
        mul_res = bits_to_float(mul_res_bits)
        assert abs(mul_res - expected_mul) < EPSILON, \
            f"Mul failed: {a_32} * {b_32} = {mul_res} (Expected {expected_mul})"
        
        dut._log.info(f"Passed: {a_32} and {b_32} (Add: {add_res}, Mul: {mul_res})")

    # Final sanity check on Register A
    read_bits_a = await tqv.read_word_reg(ADDR_A)
    last_a_32 = np.float32(test_pairs[-1][0])
    assert read_bits_a == float_to_bits(last_a_32), "Operand A corrupted!"

    dut._log.info("Borg Floating Point Addition and Multiplication Test Passed!")
