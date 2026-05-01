import pytest
from pygments.token import (
    Keyword
)

from wresl_pygment import WreslLexer
from . import get_token_type

@pytest.mark.parametrize("kw", [
    "always", "local", "global", "daysin", "month", "wateryear",
])
def test_builtin_constant(kw):
    assert get_token_type(kw) == Keyword.Constant

@pytest.mark.parametrize("month", [
    "jan", "feb", "mar", "apr", "may", "jun",
    "jul", "aug", "sep", "oct", "nov", "dec",
])
def test_month_names(month):
    assert get_token_type(month) == Keyword.Constant

@pytest.mark.parametrize("month", [
    "prevjan", "prevfeb", "prevmar", "prevapr", "prevmay", "prevjun",
    "prevjul", "prevaug", "prevsep", "prevoct", "prevnov", "prevdec",
])
def test_prev_month_names(month):
    assert get_token_type(month) == Keyword.Constant

@pytest.mark.parametrize("step", ["1mon", "1day"])
def test_timestep_constants(step):
    assert get_token_type(step) == Keyword.Constant
