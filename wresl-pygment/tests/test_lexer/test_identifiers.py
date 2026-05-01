from pygments.token import Name, Number

from . import get_token_type, get_all_token_types


# ---------------------------------------------------------------------------
# Identifiers / Names
# ---------------------------------------------------------------------------

def test_simple_identifier():
    assert get_token_type("StreamNetwork") == Name


def test_identifier_with_digits():
    assert get_token_type("Node1") == Name


def test_identifier_with_underscore():
    assert get_token_type("My_Reservoir") == Name


def test_identifier_does_not_start_with_digit():
    # "1Node" — the "1" should be an integer, "Node" a name
    tts = get_all_token_types("1Node")
    assert tts[0] == Number.Integer
    assert tts[1] == Name