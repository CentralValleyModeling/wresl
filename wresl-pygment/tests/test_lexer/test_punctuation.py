import pytest
from pygments.token import Punctuation

from . import get_token_type


# ---------------------------------------------------------------------------
# Punctuation
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("ch", ["{", "}", "(", ")", "[", "]", ","])
def test_punctuation(ch):
    assert get_token_type(ch) == Punctuation