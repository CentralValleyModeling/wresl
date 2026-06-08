import colorsys

import matplotlib.colors as mc

from .logs import get_logger

LOGGER = get_logger(__name__)


# From https://stackoverflow.com/questions/37765197/darken-or-lighten-a-color-in-matplotlib
def lighten_color(color, amount=0.5):
    """
    Lightens the given color by multiplying (1-luminosity) by the given amount.
    Input can be matplotlib color string, hex string, or RGB tuple.

    Examples:
    >> lighten_color('g', 0.3)
    >> lighten_color('#F034A3', 0.6)
    >> lighten_color((.3,.55,.1), 0.5)
    """

    try:
        c = mc.cnames[color]
    except:
        c = color
    c = colorsys.rgb_to_hls(*mc.to_rgb(c))
    lightness = 1 - amount * (1 - c[1])
    if lightness > 1:
        LOGGER.warning(f"truncating lightness to white: {lightness=}")
        lightness = 1
    elif lightness < 0:
        LOGGER.warning(f"truncating lightness to black: {lightness=}")
        lightness = 0
    return colorsys.hls_to_rgb(c[0], lightness, c[2])
