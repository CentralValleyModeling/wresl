import pytest
from pygments.token import (
    Keyword
)

from . import get_all_token_types, get_token_type

@pytest.mark.parametrize("kw", [
    "condition", "case", "value", "if", "elseif", "else",
    "select", "given", "use", "where", "from", "sum",
    "penalty", "kind", "units", "convert", "nosolver",
    "timeseries", "timestep", "alias", "external",
    "upper", "lower", "unbounded", "constrain", "never",
    "linear", "maximum", "minimum", "variable",
])
def test_directive_keyword(kw):
    assert get_token_type(kw) == Keyword.Declaration

def test_condition_always_sequence():
    code = "condition always"
    assert get_all_token_types(code) == (Keyword.Declaration, Keyword.Constant)

