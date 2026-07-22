import colorsys
from typing import Any

import matplotlib.colors as mc
import pulp

from .logs import get_logger

LOGGER = get_logger(__name__)

# Generic color
DEFAULT_COLOR = "xkcd:blue"
EQUALITY_COLOR = "xkcd:blue"
VARIABLE_LIMIT_COLOR = "xkcd:red"
SOLUTION_COLOR = "xkcd:green"
SINGLE_INTERACTIVITY_COLOR = "xkcd:cyan"


# Linestyles
DEFAULT_LINESTYLE = "-"
EQUALITY_LINESTYLE = "-"
SINGLE_INTERACTIVITY_LINESTYLE = "-"
DEFAULT_LINEWIDTH = 1
EQUALITY_LINEWIDTH = 4
SINGLE_INTERACTIVITY_LINEWIDTH = 2
SINGLE_INTERACTIVITY_ALPHA = 0.4
SINGLE_INTERACTIVITY_ZORDER = 30

# Markers
SOLUTION_MARKER = "o"

# Generic Line
DEFAULT_LINE_STYLE: dict[str, Any] = dict(
    color=DEFAULT_COLOR,
    alpha=1.0,
    zorder=50,
    linestyle=DEFAULT_LINESTYLE,
    lw=DEFAULT_LINEWIDTH,
)
# Generic Area
DEFAULT_AREA_STYLE: dict[str, Any] = dict(
    color=DEFAULT_COLOR,
    alpha=0.5,
    zorder=40,
)
# Solution
SOLUTION_STYLE: dict[str, Any] = dict(
    color=SOLUTION_COLOR,
    marker=SOLUTION_MARKER,
    zorder=98,
)


class LineAndAreaStyle:
    def __init__(self):
        self.__line_style_dict = dict(DEFAULT_LINE_STYLE)
        self.__area_style_dict = dict(DEFAULT_AREA_STYLE)
        self.__line_label = None

    @property
    def line_style_dict(self) -> dict:
        return self.__line_style_dict

    @property
    def area_style_dict(self) -> dict:
        return self.__area_style_dict

    @property
    def line_label(self) -> str | None:
        return self.__line_label

    def set_color(self, color):
        self.__area_style_dict["color"] = color
        self.__line_style_dict["color"] = color

    def set_label(self, label):
        self.__line_label = label

    def set_linestyle(self, linestyle):
        self.__line_style_dict["linestyle"] = linestyle

    def set_linewidth(self, linewidth):
        self.__line_style_dict["lw"] = linewidth

    def set_alpha(self, alpha):
        self.__line_style_dict["alpha"] = alpha
        self.__area_style_dict["alpha"] = alpha * 0.5

    def set_zorder(self, zorder):
        self.__line_style_dict["zorder"] = zorder
        self.__area_style_dict["zorder"] = zorder - 10

    def plot_area(self) -> bool:
        return True


class ConstraintStyle(LineAndAreaStyle):
    def __init__(self, constraint: pulp.LpConstraint, interactivity: int):
        super().__init__()
        self.set_label(constraint.name)
        if constraint.sense == 0:
            self.set_linestyle(EQUALITY_LINESTYLE)
            self.set_linewidth(EQUALITY_LINEWIDTH)

        if interactivity == 0:
            LOGGER.warning("style for constraint without interactivity requested")
            self.set_alpha(0.1)
            self.set_zorder(30)
        elif interactivity == 1:
            self.set_alpha(SINGLE_INTERACTIVITY_ALPHA)
            self.set_color(SINGLE_INTERACTIVITY_COLOR)
            self.set_zorder(SINGLE_INTERACTIVITY_ZORDER)

    def plot_area(self) -> bool:
        return True


class VariableLimitStyle(LineAndAreaStyle):
    def __init__(self):
        super().__init__()
        self.set_color(VARIABLE_LIMIT_COLOR)


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
    except Exception:
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
