import pytest
from pygments.token import Keyword, Number

from . import get_token_type, get_all_token_types


# ---------------------------------------------------------------------------
# Numbers
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("n", ["0", "1", "42", "100"])
def test_integer(n):
    assert get_token_type(n) == Number.Integer


@pytest.mark.parametrize("n", ["3.14", "0.5", ".5", "1.", "1.0e5", "2.5E-3"])
def test_float(n):
    assert get_token_type(n) == Number.Float


def test_order_integer_in_context():
    code = "order 1"
    tts = get_all_token_types(code)
    assert tts[0] == Keyword          # order
    assert tts[1] == Number.Integer   # 1