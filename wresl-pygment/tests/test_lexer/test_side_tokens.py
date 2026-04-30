import pytest
from pygments.token import Keyword, Name

from . import get_token_type


# ---------------------------------------------------------------------------
# Side tokens  (Keyword.Type)
# ---------------------------------------------------------------------------

def test_lhs():
    assert get_token_type("lhs") == Keyword.Type


def test_rhs():
    assert get_token_type("rhs") == Keyword.Type


def test_lhs_case_insensitive():
    assert get_token_type("LHS") == Keyword.Type


def test_side_not_matched_mid_word():
    # "clhs" should be a Name, not lhs embedded in it
    assert get_token_type("Clhs") == Name