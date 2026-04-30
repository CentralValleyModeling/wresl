import pytest
from pygments.token import Operator

from . import get_token_type


# ---------------------------------------------------------------------------
# Logical word operators  (Operator.Word)
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("op", [".not.", ".and.", ".or.", ".ne."])
def test_logical_operator(op):
    assert get_token_type(op) == Operator.Word


def test_logical_operator_case_insensitive():
    assert get_token_type(".AND.") == Operator.Word