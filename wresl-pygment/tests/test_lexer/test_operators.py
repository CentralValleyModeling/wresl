import pytest
from pygments.token import Operator

from . import get_token_type


# ---------------------------------------------------------------------------
# Comparison and math operators  (Operator)
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("op", [">", "<", ">=", "<=", "==", "="])
def test_comparison_operator(op):
    assert get_token_type(op) == Operator


@pytest.mark.parametrize("op", ["+", "-", "*", "/", ":"])
def test_math_operator(op):
    assert get_token_type(op) == Operator