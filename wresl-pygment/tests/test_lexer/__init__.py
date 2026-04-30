from wresl_pygment import WreslLexer
from pygments.token import Whitespace

LEXER = WreslLexer()


def read_tokens(source: str):        
    for tt, val in LEXER.get_tokens(source):
        if tt not in (Whitespace,) and val.strip():
            yield (tt, val)

def get_all_token_types(source: str):
    return tuple(tt for tt, _ in read_tokens(source))

def get_token_type(source: str):
    _types = tuple(tt for tt, _ in read_tokens(source))
    if len(_types) != 1:
        raise ValueError(f"Token stream longer than expected: {_types}")
    return _types[0]