import pytest
from pygments.token import (
    Name, Punctuation
)

from . import get_all_token_types, get_token_type

@pytest.mark.parametrize("fn", [
    "abs", "int", "real", "exp", "log", "log10",
    "sqrt", "round", "pow", "mod", "min", "max", "range",
])
def test_builtin_function(fn):
    assert get_token_type(fn) == Name.Builtin

def test_future_array_maximum():
    assert get_token_type("$m") == Name.Builtin

def test_function_call_tokens():
    code = "abs(X)"
    tts = get_all_token_types(code)
    assert tts[0] == Name.Builtin    # abs
    assert tts[1] == Punctuation     # (
    assert tts[2] == Name            # X
    assert tts[3] == Punctuation     # )