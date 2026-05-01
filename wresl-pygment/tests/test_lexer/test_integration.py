from pygments.token import Comment, Keyword, Name, Number, Operator, String

from . import get_all_token_types


# ---------------------------------------------------------------------------
# Integration: realistic WRESL snippets
# ---------------------------------------------------------------------------

def test_model_block_include_file():
    code = """
    model SimpleOperations {
        include group StreamNetwork
        include 'weights.wresl'
    }
    """
    tts = get_all_token_types(code)
    assert Keyword in tts
    assert String in tts
    assert Name in tts


def test_sequence_block():
    code = """
    sequence First {
        model SimpleOperations
        condition always
        order 1
    }
    """
    tts = get_all_token_types(code)
    assert tts.count(Keyword) >= 3          # sequence, model, order
    assert Keyword.Declaration in tts        # condition
    assert Keyword.Constant in tts           # always
    assert Number.Integer in tts             # 1


def test_goal_with_lhs_rhs():
    code = "goal MyGoal { lhs X = rhs Y }"
    tts = get_all_token_types(code)
    assert Keyword in tts           # goal
    assert Keyword.Type in tts      # lhs, rhs
    assert Operator in tts          # =


def test_if_else_block():
    code = """
    if X > 0 {
        value 1.0
    } elseif X < 0 {
        value -1.0
    } else {
        value 0
    }
    """
    tts = get_all_token_types(code)
    assert tts.count(Keyword.Declaration) >= 3   # if, elseif, else, value x3


def test_select_statement():
    code = "select Flow from LookupTable given Month = jan use linear"
    tts = get_all_token_types(code)
    assert Keyword.Declaration in tts    # select, from, given, use
    assert Keyword.Constant in tts       # jan
    assert Name in tts                   # Flow, LookupTable, Month


def test_mixed_comments_and_code():
    code = """! define the model
    model Ops { // inline comment
        /* block
           comment */
        include 'ops.wresl'
    }
    """
    tts = get_all_token_types(code)
    assert Comment.Single in tts
    assert Comment.Multiline in tts
    assert Keyword in tts
    assert String in tts