.. _explanation-index:

.. role:: wresl(code)
   :language: wresl

Explanation
===========

*Understanding-oriented: background, concepts, and design rationale.*

This section introduces the core ideas behind WRESL+ to the readers to have a solid conceptual foundation about the LP model structure and utilized syntaxes.

WRESL+ & MILP Solvers
---------------------

At each simulation time step, WRESL+ defines a **linear programming (LP) problem**, a set of known inputs, unknown decision variables, and constraints, that a solver optimizes. The language cleanly separates three concerns:

.. list-table:: 
    :header-rows: 1

    * - Concern
      - WRESL+ Construct
      - Role
    * - Known inputs
      - State Variables
      - Provide data to the solver
    * - Model "Results"
      - Decision Variables
      - Determined by the solver
    * - Rules & Objectives
      - Goals / Objectives
      - Constraints the solver must satisfy

Key Concepts
------------

State Variables
~~~~~~~~~~~~~~~

State variables represent known or precomputed values at each time step. They can be:

- **Fixed values** (:wresl:`value`): constants or expressions computed from other variables
- **Time series** (:wresl:`timeseries`): data read from HEC-DSS files (e.g., historical inflows like ``I_SHSTA``)
- **Table lookups** (:wresl:`select X from Y where Z`): values interpolated from external data tables
- **Conditional** (:wresl:`case` or :wresl:`condition`): values that vary based on runtime conditions (e.g., wet vs. dry year operations)
- **Summations** (:wresl:`sum`): aggregated values over an index range

State Variables are fully determined *before* the solver runs — they provide inputs and parameters to the optimization.

Decision Variables
~~~~~~~~~~~~~~~~~~

Decision variables are the unknowns that the linear programming solver determines. They have:

- **Bounds** (:wresl:`lower` or :wresl:`upper`): feasible range for the variable
- **Standard form** (:wresl:`std`): non-negative by default (:wresl:`lower 0 upper unbounded`)
- **Integer option** (:wresl:`integer`): restricts the variable to integer values
- **Metadata** (:wresl:`kind` and :wresl:`units`): descriptive labels for output (e.g., :wresl:`kind 'FLOW-CHANNEL'`, :wresl:`units 'CFS'`)

The solver adjusts Decision Variables (e.g., ``C_SHSTA``, ``D_REDBLF``) to satisfy goals while optimizing the objective function.

Goals
~~~~~

Goals define linear constraints that the solver must satisfy or approximate. They can be:

- **Simple constraints**: equality (``=``) or inequality (``<``, ``>``) between expressions (e.g., mass balance at a reservoir)
- **Penalty-based**: when a constraint cannot be strictly enforced, a penalty cost is applied for violation (e.g., relaxing minimum instream flow requirements)
- **Conditional**: different constraints apply under different conditions using :wresl:`case` or :wresl:`condition` (e.g., summer vs. winter operating rules)

Objectives
~~~~~~~~~~

Objectives define the optimization direction by assigning weights to decision variables. The solver minimizes the total weighted cost, so higher-weighted violations are more strongly discouraged.

Constants and Aliases
~~~~~~~~~~~~~~~~~~~~~

- **Constants** (`const`): fixed numeric values that never change during a simulation (e.g., reservoir dead pool level).
- **Aliases** (`alias`): alternative names for computed expressions, primarily used to tag output for post-processing (e.g., defining ``OMFlow`` as an alias for ``C_OMR014`` to label Old and Middle River flow output).

Model Structure
---------------

A WRESL+ model is organized hierarchically. Here is a hypothetical main file structure:

.. code-block::

    main.wresl
    ├── initial { ... }                                   ! Define constant variables
    ├── sequence first  { model Network order 1 }         ! Mass balance, no operations
    ├── sequence second { model Operations order 2 }      ! Add in storage operations
    ├── sequence third  { model Regulations order 33 }    ! Add in regulations
    ├── group MassBalance { ... }                         ! Shared define/goal statements
    ├── MODEL Network { ... }                             ! Include statements
    └── MODEL Operations { ... }                          ! Include statements


1. **Sequences** define the order in which models are solved.
2. **Models** group related variables and goals into a solvable unit for one solver pass.
3. **Groups** organize shared definitions that can be included by multiple models.
4. **Initial blocks** set up variables and conditions evaluated once before the main simulation loop.
5. **Include files** modularize definitions across multiple ``.wresl`` files.

This structure supports **multi-cycle solving**, where results from an upstream model feed into downstream calculations (e.g., upstream reservoir releases feeding into downstream Delta operations).

How the Solver Works
--------------------

At each time step, WRIMS:

1. Evaluates all State Variables (inputs, time series, table lookups, conditional values).
2. Constructs the LP problem from Decision Variables (unknowns) and goals (constraints).
3. Applies the objective function weights.
4. Solves the LP to determine optimal Decision Variable values.
5. Advances to the next time step and repeats.

Understanding this cycle is essential: State Variables must be resolvable *before* the solver runs, while Decision Variables are only determined *by* the solver.

Design Philosophy
-----------------

- **Declarative:** Users declare *what* to solve, not *how* to solve it — the optimizer handles the mechanics.
- **Modularity:** Models are composed from reusable ``.wresl`` files via ``include``.
- **Reproducibility:** All logic is explicit, text-based, and version-controllable.
- **Extensibility:** External functions (DLLs), table lookups, and time series data can be integrated.
- **Case-insensitivity:** All keywords can be written in any case (``define``, ``DEFINE``, ``DeFiNe``).

Common Pitfalls Explained
-------------------------

- **Missing :wresl:`kind` and :wresl:`units`:** Decision Variable and timeseries definitions require :wresl:`kind` and :wresl:`units` metadata. Omitting them causes parser errors.
- **Incorrect string quotes:** WRESL+ uses **single quotes** (`'...'`), not double quotes. Using double quotes will cause syntax errors.
- **Wrong comment syntax:** Comments use ``!`` or ``/* */`` **not** ``//``.
- **Wrong logical operators:** Use :wresl:`.and.`, :wresl:`.or.`, :wresl:`.not.` — not ``&&``, ``||``, ``!``.
- **Case order matters:** In :wresl:`case` or :wresl:`condition` blocks, conditions are evaluated top-to-bottom. The first matching condition is used, so place the :wresl:`condition always` case last as a default.
- **Circular dependencies:** Avoid State Variables that reference each other in a cycle, as this creates unresolvable definitions.
- **Include path errors:** File paths in :wresl:`include` statements use single quotes and are relative to the model root.

WRESL vs. WRESL+
----------------

WRESL+ is a modernized superset of the original WRESL language. Key similarities and differences:

.. list-table::
    :header-rows: 1

    * - Feature
      - WRESL
      - WRESL+
    * - Variable keywords
      - :wresl:`define` for all types
      - :wresl:`define` as well as :wresl:`dvar`, :wresl:`svar`, and :wresl:`const`
    * - File Extension
      - ``.wresl``
      - ``.wresl``
    * - Study structure
      - :wresl:`sequence`, :wresl:`model`, :wresl:`include`
      - Adds :wresl:`group` and :wresl:`initial`
    * - Sequence structure
      - All sequences share a timestep
      - Each sequence sets it's own timestep (:wresl:`1MON` or :wresl:`1DAY`)
    * - Conditional include
      - Not supported
      - :wresl:`if`, :wresl:`elseif`, and :wresl:`else` supported
    * - Future arrays
      - Not supported
      - :wresl:`dvar(N)`, :wresl:`svar(N)`, and :wresl:`goal(N)` supported. 

In practice, WRESL+ files are backward-compatible with most WRESL constructs, but WRESL+ adds structured model organization (:wresl:`group`, :wresl:`initial`), a dedicated :wresl:`objective` keyword, conditional includes, future arrays, and explicit variable-type keywords (:wresl:`dvar`, :wresl:`svar`) that the original WRESL lacks.