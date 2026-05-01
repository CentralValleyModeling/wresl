.. _reference-index:

Reference
=========

This section is a comprehensive reference for the WRESL+ language. Use it to look up exact syntax, available keywords, and supported functions.

Keywords
--------

The following keywords are recognized by the WRESL+ parser (case-insensitive):

.. list-table::

   * - Category
     - Keywords
   * - Variable definition
     - ``define``, ``svar``, ``dvar``, ``const``
   * - Variable content
     - ``value``, ``timeseries``, ``alias``, ``external``, ``std``, ``integer``, ``binary``
   * - Bounds
     - ``lower``, ``upper``, ``unbounded``
   * - Metadata
     - ``kind``, ``units``, ``convert``
   * - Goals/Objectives
     - ``goal``, ``objective``, ``lhs``, ``rhs``, ``penalty``, ``constrain``
   * - Table lookups
     - ``select``, ``from``, ``given``, ``use``, ``where``
   * - Summation
     - ``sum``
   * - Conditions
     - ``condition``, ``always``, ``case``
   * - Logical operators
     - ``.and.``, ``.or.``, ``.not.``
   * - Control flow
     - ``if``, ``elseif``, ``else``
   * - Model structure
     - ``model``, ``group``, ``sequence``, ``initial``, ``include``, ``order``, ``timestep``
   * - Special identifiers
     - ``month``, ``wateryear``, ``day``, ``daysin``, ``daysinmonth``, ``daysintimestep``
   * - Unit conversions
     - ``taf_cfs``, ``cfs_taf``, ``af_cfs``, ``cfs_af``
   * - Calendar months
     - ``jan``, ``feb``, ``mar``, ``apr``, ``may``, ``jun``, ``jul``, ``aug``, ``sep``, ``oct``, ``nov``, ``dec``
   * - Previous months
     - ``prevjan``, ``prevfeb``, ..., ``prevdec``

Variable Definition Syntax
--------------------------

**TimeArray** — An optional parenthesized integer or identifier, e.g., ``(12)``, that declares the variable as a time array with multiple slots (one per sub-timestep). When omitted, the variable holds a single value per timestep. This is used for variables that need to store values across multiple sub-periods within a timestep.

State Variable
~~~~~~~~~~~~~~

.. code-block:: wresl

   define|svar [(TimeArray)] [[local]] Name { SvarContent }

Where ``SvarContent`` is one of:

- ``value Expression``
- ``timeseries ['BPart'] kind 'K' units 'U' [convert 'C']``
- ``select Column from Table [given Assign use ID] [where Assignments]``
- ``sum(i=Expr1,Expr2) Expression``
- One or more ``case Name { condition LogicalExpr|always  value|select|sum ... }``

Examples:

.. code-block:: wresl

   ! Source: CalSim3 DCR 9.5.0 — arcs-Inflows.wresl
   define I_SHSTA {timeseries kind 'INFLOW' units 'TAF' convert 'CFS'}

   ! Source: CalSim3 DCR 9.5.0 — arcs-Reservoirs.wresl
   define S_SHSTAlevel1 {value 550}

   ! Source: CalSim3 DCR 9.5.0 — arcs-Reservoirs.wresl
   define A_SHSTAlast {
     select area from res_info
       given storage = 1000*S_SHSTA(-1)
       use linear
       where res_num = 4
   }

   ! Source: WRESL+ Language Reference (2018)
   svar Z {sum(i=1, 5, 1) S03(-i) + I10(-i)}

   ! Source: WRESL+ Language Reference (2018)
   svar NMTest {
     case February {
       condition month == feb
       value S10(-12) + sumI10_part
     }
     case Others {
       condition always
       value S10(prevfeb) + sumI10_part
     }
   }

Decision Variable
~~~~~~~~~~~~~~~~~

.. code-block:: wresl

   define|dvar [(TimeArray)] [[local]] Name { DvarContent }

Where ``DvarContent`` is one of:

- ``std kind 'K' units 'U'``
- ``[lower Expr] [upper Expr] kind 'K' units 'U'``
- ``binary kind 'K' units 'U'``
- ``integer std kind 'K' units 'U'``
- ``integer [lower Expr] [upper Expr] kind 'K' units 'U'``

Examples:

.. code-block:: wresl

   ! Source: WRESL+ Language Reference (2018)
   dvar C_Delta_SWP {std kind 'FLOW-CHANNEL' units 'CFS'}
   dvar QPD {lower -100. upper unbounded kind 'FLOW' units 'CFS'}
   dvar Integer2 {integer lower 0 upper 3 kind 'INTEGER' units 'NA'}

Constant
~~~~~~~~

.. code-block:: wresl

   const [[local]] Name { Number }

Example:

.. code-block:: wresl

   ! Source: CalSim3 DCR 9.5.0 — arcs-Reservoirs.wresl
   define S_SHSTAlevel1 {value 550}

Alias
~~~~~

.. code-block:: wresl

   define [(TimeArray)] [[local]] Name { alias Expression [kind 'K'] [units 'U'] }

Example:

.. code-block:: wresl

   ! Source: CalSim3 DCR 9.5.0 — SoDeltaChannels.wresl
   define OMFlow {alias C_OMR014 kind 'FLOW-CHANNEL' units 'CFS'}

   ! Source: WRESL+ Language Reference (2018)
   alias Estlim3 {max(300., Est_rel) kind 'DEBUG' units 'CFS'}

External
~~~~~~~~

.. code-block:: wresl

   define [[local]] Name { external LibraryName[.dll|.f90] }

Where ``LibraryName`` is the filename of an external DLL or Fortran 90 library. The external function is called by the evaluator at runtime.

Example:

.. code-block:: wresl

   ! Source: CalSim3 DCR 9.5.0 — CVGroundwater_ExtFuncs.wresl
   define InitGWSystem {EXTERNAL interfacetogw_x64.dll}

Goal Syntax
-----------

Simple goal
~~~~~~~~~~~

.. code-block:: wresl

   goal [(TimeArray)] [[local]] Name { Expression < | > | = Expression }

Examples:

.. code-block:: wresl

   ! Source: CalSim3 DCR 9.5.0 — constraints-Connectivity.wresl
   goal continuitySHSTA {I_SHSTA - C_SHSTA - D_SHSTA_WTPJMS - E_SHSTA = S_SHSTA * taf_cfs - S_SHSTA(-1) * taf_cfs + 0.}

   ! Source: WRESL+ Language Reference (2018)
   goal Test {X > Y}

Complex goal with LHS/RHS
~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: wresl

   goal [(TimeArray)] [[local]] Name {
     lhs Expression
     rhs Expression
     [lhs > rhs  constrain | penalty Expression]
     [lhs < rhs  constrain | penalty Expression]
   }

Example:

.. code-block:: wresl

   ! Source: WRESL+ Language Reference (2018)
   goal NMTest {
     lhs X + Y
     rhs Z
     lhs > rhs constrain
     lhs < rhs penalty 0
   }

Complex goal with cases
~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: wresl

   goal [(TimeArray)] [[local]] Name {
     lhs Expression
     case CaseName {
       condition LogicalExpression | always
       rhs Expression
       [lhs > rhs  constrain | penalty Expression]
       [lhs < rhs  constrain | penalty Expression]
     }
     ...
   }

Example:

.. code-block:: wresl

   ! Source: WRESL+ Language Reference (2018)
   goal TestAction1 {
     lhs D_J1 + D_B1
     case April {
       condition month == apr
       rhs CNW + X
       lhs < rhs penalty 0
     }
     case Others {
       condition always
       rhs 3000. * 16.0 / daysin + Z
       lhs < rhs penalty 0
       lhs > rhs constrain
     }
   }

Objective Syntax
----------------

.. code-block:: wresl

   objective [[local]] Name [=] {
     [VarRef [(TimeArray)], WeightExpression],
     ...
   }

Notes:

- For Decision Variables declared as a time array, the ``(TimeArray)`` in the objective entry must match the declaration.
- For non-time-array Decision Variables, omit the time array specifier.

Example:

.. code-block:: wresl

   ! Source: WRESL+ Language Reference (2018)
   objective XGroup {[X1, 10] [X2, 20]}

   ! Source: CalSim3 DCR 9.5.0 — Weight-table.wresl (abbreviated)
   Objective obj_SYS = {
     [S_TRNTY_1, 200000*taf_cfs],
     [S_TRNTY_2, 93*taf_cfs],
     [S_SHSTA_1, 200000*taf_cfs],
     [S_SHSTA_2, 93*taf_cfs]
   }

Model Structure Syntax
----------------------

Sequence
~~~~~~~~

.. code-block:: wresl

   sequence Name { model ModelRef [condition LogicalExpr] [order INT] [timestep 1MON|1DAY] }

Example:

.. code-block:: wresl

   ! Source: WRESL+ Language Reference (2018)
   sequence CYCLE1 {model Upstream order 1}
   sequence CYCLE4 {model Test2 condition MON==jun order 4}

   ! Source: CalSim3 DCR 9.5.0 — mainCS3_ReOrg_UWplusVF.wresl
   SEQUENCE CYCLE33 {model UPSTREAM condition simulateSacramentoValley >= 0.5 order 33}

Model
~~~~~

.. code-block:: wresl

   model Name { (Pattern | IfIncItems)+ }

Example:

.. code-block:: wresl

   ! Source: CalSim3 DCR 9.5.0 — mainCS3_ReOrg_UWplusVF.wresl (abbreviated)
   model UPSTREAM {
     include group Tdomain
     include group Common_All
     include 'System\System_Sac_cycle1.wresl'
     if simulateSacVA_TisdaleWeir < 0.5 {
       include 'SystemTables_Sac\constraints-Weirs2.wresl'
     }
   }

Group
~~~~~

.. code-block:: wresl

   group Name { (Pattern | IfIncItems)+ }

Example:

.. code-block:: wresl

   ! Source: CalSim3 DCR 9.5.0 — mainCS3_ReOrg_UWplusVF.wresl
   group Common_All {
     include 'CVGroundwater\CalSim3GWregionIndex.wresl'
     include 'Other\NewFacilitySwitches.wresl'
     include 'Other\wytypes\WyTypesGeneral.wresl'
   }

Initial
~~~~~~~

.. code-block:: wresl

   initial { (SimpleValueSvar | LookupTableSvar | IncludeStatement)+ }

Notes:

- Runs once before the main simulation loop.
- Only simple value or lookup table state variables are allowed.
- Include statements are often used to bring in files with these variables.

Example:

.. code-block:: wresl

   ! Source: WRESL+ Language Reference (2018)
   initial {
     svar A {value 10.}
     svar B {value A * 5}
     svar C {value B + 6.0}
   }

   ! Source: CalSim3 DCR 9.5.0 — mainCS3_ReOrg_UWplusVF.wresl (abbreviated)
   initial {
     include 'System\SystemTables_Sac\arcs-Reservoirs.wresl'
     include 'CVGroundwater\CVGroundwater_init1_SJRBASE.wresl'
   }

Include
~~~~~~~

.. code-block:: wresl

   include [[local]] 'filepath.wresl'
   include model ModelName
   include group GroupName

Data Types
----------

- Integer (e.g., ``42``, ``550``)
- Float (e.g., ``3.14``, ``.5``, ``1.98347``)

Operators
---------

.. list-table::

   * - Type
     - Operators
   * - Arithmetic
     - ``+``, ``-``, ``*``, ``/``
   * - Comparison
     - ``==``, ``/=`` (not equal), ``<``, ``>``, ``<=``, ``>=``
   * - Logical
     - ``.and.``, ``.or.``, ``.not.``

Comments
--------

Single-line comments begin with ``!``:

.. code-block:: wresl

   ! Sacramento River inflow to Shasta
   define I_SHSTA {value 10000}   ! CFS

Strings
-------

Strings use **single quotes**:

.. code-block:: wresl

   kind 'FLOW-CHANNEL'
   units 'CFS'
   include 'System\SystemTables_Sac\arcs-Reservoirs.wresl'

Supported Functions
-------------------

.. list-table::

   * - Function
     - Description
     - Example
   * - ``min(a, b, ...)``
     - Minimum of values
     - ``min(I_SHSTA, maxCapacity)``
   * - ``max(a, b, ...)``
     - Maximum of values
     - ``max(C_SHSTA, 0)``
   * - ``abs(x)``
     - Absolute value
     - ``abs(S_SHSTA - target)``
   * - ``round(x)``
     - Round to nearest integer
     - ``round(ratio)``
   * - ``int(x)``
     - Truncate to integer
     - ``int(2.9) → 2``
   * - ``mod(a, b)``
     - Modulo (remainder)
     - ``mod(month, 12)``
   * - ``pow(a, b)``
     - Power (a raised to b)
     - ``pow(base, exp)``
   * - ``log(x)``
     - Logarithm (also ``log10``)
     - ``log(value)``
   * - ``sin(x)``
     - Sine
     - ``sin(angle)``
   * - ``cos(x)``
     - Cosine
     - ``cos(angle)``
   * - ``tan(x)``
     - Tangent
     - ``tan(angle)``
   * - ``cot(x)``
     - Cotangent
     - ``cot(angle)``
   * - ``asin(x)``
     - Arc sine
     - ``asin(ratio)``
   * - ``acos(x)``
     - Arc cosine
     - ``acos(ratio)``
   * - ``atan(x)``
     - Arc tangent
     - ``atan(slope)``
   * - ``acot(x)``
     - Arc cotangent
     - ``acot(value)``
   * - ``range(var, start, end)``
     - Check if month/year is in range
     - ``range(month, oct, mar)``

Special Built-in Identifiers
----------------------------

.. list-table::

   * - Identifier
     - Description
   * - ``month``
     - Current simulation month (numeric)
   * - ``wateryear``
     - Current water year
   * - ``day``
     - Current simulation day
   * - ``daysin`` / ``daysinmonth``
     - Number of days in current month
   * - ``daysintimestep``
     - Number of days in current timestep. **Note:** This is a built-in evaluator keyword, not a parser-level keyword. It is case-sensitive and must be written in lowercase only.
   * - ``jan`` ... ``dec``
     - Calendar month constants
   * - ``prevjan`` ... ``prevdec``
     - Previous year's month references
   * - ``taf_cfs``
     - TAF to CFS conversion factor for the current timestep
   * - ``cfs_taf``
     - CFS to TAF conversion factor for the current timestep
   * - ``af_cfs``
     - AF to CFS conversion factor for the current timestep
   * - ``cfs_af``
     - CFS to AF conversion factor for the current timestep
   * - ``$m``
     - Multi-step reference
   * - ``i``
     - Loop variable (used inside ``sum`` expressions)

Variable Cross-References
-------------------------

.. list-table::

   * - Syntax
     - Meaning
     - Example
   * - ``varName[modelName]``
     - Reference variable as solved in another model cycle
     - ``C_CAA003_EXP1[WHEELJPOD]``
   * - ``varName(-1)``
     - Reference variable from the previous time step
     - ``S_SHSTA(-1)``
   * - ``varName[modelName](offset)``
     - Cross-model reference with time offset
     - ``EiExpCtrl[PRESETUP](-1)``
   * - ``varName[-1]``
     - Index-based variable reference
     - ``S_SHSTA[-1]``