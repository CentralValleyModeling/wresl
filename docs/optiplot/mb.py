import pulp

from .sense import SENSE_MAP
from .logs import get_logger

LOGGER = get_logger(__name__)


def calc_slope_intercept(
    x: pulp.LpVariable,
    y: pulp.LpVariable,
    expression: pulp.LpConstraint,
):
    LOGGER.info(f"reducing constraint '{expression.name}': {expression}")
    if y not in expression.keys():
        LOGGER.debug(f"constraint is not a function of {y.name}")
        return _calc_intercept_for_1_var(x, expression)
    elif x not in expression.keys():
        LOGGER.debug(f"constraint is not a function of {x.name}")
        return _calc_intercept_for_1_var(y, expression)
    else:
        return _calc_slope_intercept_2_vars(x, y, expression)


def _calc_intercept_for_1_var(v: pulp.LpVariable, expression: pulp.LpConstraint):
    c: float = -expression.constant
    v_const = 1.0
    var: pulp.LpVariable
    for var, const in expression.items():
        if var is v:
            v_const = const
        else:
            LOGGER.debug(
                f"treating variable as solved constant: {var.name}={var.value()}"
            )
            c -= var.value() * const
    c = c / v_const
    if v_const < 0:
        kind = SENSE_MAP[-expression.sense]
    else:
        kind = SENSE_MAP[expression.sense]
    LOGGER.debug(f"constraint reduced to: {v.name} {kind} {c:+f}")
    return 0.0, c


def _calc_slope_intercept_2_vars(
    x: pulp.LpVariable,
    y: pulp.LpVariable,
    expression: pulp.LpConstraint,
):
    b: float = -expression.constant
    m = 1.0
    y_const = 1.0
    var: pulp.LpVariable
    for var, const in expression.items():
        if var is x:
            m = m * -const
        elif var is y:
            m = m / const
            y_const = const
        else:
            LOGGER.debug(
                "non-target variable in expression, "
                + f"treating as solved constant: {var.name}={var.value()}"
            )
            b -= var.value() * const
    b = b / y_const
    if y_const < 0:
        kind = SENSE_MAP[-expression.sense * -1]
    else:
        kind = SENSE_MAP[-expression.sense]
    LOGGER.debug(f"constraint reduced to: {y.name} {kind} {m} * {x.name} {b:+f}")
    return m, b
