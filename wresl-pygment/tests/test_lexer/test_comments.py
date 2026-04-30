from pygments.token import Comment, Keyword

from . import get_token_type, get_all_token_types


# ---------------------------------------------------------------------------
# Comments
# ---------------------------------------------------------------------------

def test_exclamation_comment():
    assert get_token_type("! this is a comment") == Comment.Single


def test_cpp_comment():
    assert get_token_type("// this is a comment") == Comment.Single


def test_block_comment():
    assert get_all_token_types("/* block comment */")[0] == Comment.Multiline


def test_block_comment_multiline():
    code = "/* line one\n   line two */"
    tts = get_all_token_types(code)
    assert all(tt == Comment.Multiline for tt in tts)


def test_comment_suppresses_keywords():
    # 'model' inside a comment must not produce a Keyword token
    tts = get_all_token_types("! model sequence group")
    assert Keyword not in tts