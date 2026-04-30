from pygments.token import String, Text

from . import get_token_type, get_all_token_types


# ---------------------------------------------------------------------------
# Strings
# ---------------------------------------------------------------------------

def test_single_quoted_string():
    assert get_token_type("'weights.wresl'") == String


def test_double_quoted_string():
    assert get_token_type('"weights.wresl"') == String


def test_string_with_spaces():
    tt = get_token_type("'some file path.wresl'")
    assert tt == String


def test_empty_string():
    tt = get_token_type("''")
    # empty string body — may lex as String or fall through; just not an error token
    assert tt in (String, Text)


def test_string_containing_keyword():
    # keywords inside strings must not be re-tokenised
    tts = get_all_token_types("'model'")
    assert tts[0] == String
    assert len(tts) == 1