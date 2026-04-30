import pytest
from pygments.token import (
    Comment, Keyword, Name, Number, Operator,
    Punctuation, String, Text, Whitespace
)

from wresl_pygment import WreslLexer
from . import get_token_type, get_all_token_types


@pytest.mark.parametrize("kw", [
    "model", "sequence", "group", "include",
    "goal", "objective", "weight", "define",
    "dvar", "svar", "order", "initial",
])
def test_structural_keyword(kw):
    assert get_token_type(kw) == Keyword

@pytest.mark.parametrize("kw", [
    "MODEL", "Sequence", "GrOuP",
])
def test_structural_keywords_case_insensitive(kw):
    assert get_token_type(kw) == Keyword

def test_keyword_not_matched_mid_identifier():
    tt = get_token_type("Mymodel")
    assert tt == Name

def test_model_block():
    code = "model SimpleOperations { }"
    tts = get_all_token_types(code)
    assert tts[0] == Keyword          # model
    assert tts[1] == Name             # SimpleOperations
    assert tts[2] == Punctuation      # {
    assert tts[3] == Punctuation      # }

def test_sequence_block():
    code = "sequence First { model SimpleOperations condition always order 1 }"
    tts = get_all_token_types(code)
    assert tts[0] == Keyword          # sequence
    assert tts[1] == Name             # First

   