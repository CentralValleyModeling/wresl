

# WRESL+ User Documentation

WRESL+ (Water Resources Engineering Scripting Language Plus) is a domain-specific language for water resources modeling, used within the WRIMS (Water Resources Integrated Modeling System). It enables users to define state variables, decision variables, goals, and optimization objectives for complex water resource simulations. WRESL+ is designed to support reproducible, modular, and transparent modeling for hydrologic and operations studies.


---


## Contents

- [Explanation](#explanation): Background, concepts, and rationale
  - [WRESL vs. WRESL+](#wresl-vs-wresl): Key differences between the two
- [Tutorials](#tutorials): Step-by-step lessons for beginners
- [Practical Guides](#practical-guides): Recipes for common tasks and goals
- [Reference](#reference): Technical details and valid syntax
- [Debugging Tips](#debugging-tips): Advice for solving common issues
- [Contributing](#contributing-to-the-documentation): How to help improve this documentation

---


## Explanation
*Understanding-oriented: background, concepts, and design rationale.*

This section introduces the core ideas behind WRESL+ to the readers to have a solid conceptual foundation about the LP model structure and utilized syntaxes.

### How WRESL+ based built models are passed to solvers?
At each simulation time step, WRESL+ defines a **linear programming (LP) problem**, a set of known inputs, unknown decision variables, and constraints, that a solver optimizes. The language cleanly separates three concerns:

| Concern | WRESL+ construct | Role | CalSim example |
|---|---|---|---|
| Known inputs | State Variables (SVAR) | Provide data to the solver | `I_SHSTA` (Shasta inflow) |
| Unknowns to solve | Decision Variables (DVAR) | Values determined by the solver | `C_SHSTA` (channel flow at Shasta) |
| Rules & objectives | Goals / Objectives | Constraints the solver must satisfy or optimize | Mass balance, minimum instream flow |

### Key Concepts

#### State Variables (SVAR)
State variables represent known or precomputed values at each time step. They can be:
- **Fixed values** (`value`): constants or expressions computed from other variables
- **Time series** (`timeseries`): data read from HEC-DSS files (e.g., historical inflows like `I_SHSTA`)
- **Table lookups** (`select...from...where`): values interpolated from external data tables
- **Conditional** (`case`/`condition`): values that vary based on runtime conditions (e.g., wet vs. dry year operations)
- **Summations** (`sum`): aggregated values over an index range

SVARs are fully determined *before* the solver runs — they provide inputs and parameters to the optimization.

#### Decision Variables (DVAR)
Decision variables are the unknowns that the linear programming solver determines. They have:
- **Bounds** (`lower`/`upper`): feasible range for the variable
- **Standard form** (`std`): non-negative by default (lower=0, upper=unbounded)
- **Integer option** (`integer`): restricts the variable to integer values
- **Metadata** (`kind`/`units`): descriptive labels for output (e.g., `kind 'FLOW-CHANNEL'`, `units 'CFS'`)

The solver adjusts DVARs (e.g., `C_SHSTA`, `D_REDBLF`) to satisfy goals while optimizing the objective function.

#### Goals (Constraints)
Goals define linear constraints that the solver must satisfy or approximate. They can be:
- **Simple constraints**: equality (`=`) or inequality (`<`, `>`) between expressions (e.g., mass balance at a reservoir)
- **Penalty-based**: when a constraint cannot be strictly enforced, a penalty cost is applied for violation (e.g., relaxing minimum instream flow requirements)
- **Conditional**: different constraints apply under different conditions using `case`/`condition` (e.g., summer vs. winter operating rules)

#### Objectives
Objectives define the optimization direction by assigning weights to decision variables. The solver minimizes the total weighted cost, so higher-weighted violations are more strongly discouraged.

#### Constants and Aliases
- **Constants** (`const`): fixed numeric values that never change during a simulation (e.g., reservoir dead pool level).
- **Aliases** (`alias`): alternative names for computed expressions, primarily used to tag output for post-processing (e.g., defining `OMFlow` as an alias for `C_OMR014` to label Old and Middle River flow output).

### Model Structure
A WRESL+ model is organized hierarchically. Here is the structure from the CalSim3 main file (`mainCS3_ReOrg_UWplusVF.wresl`):

```
mainCS3_ReOrg_UWplusVF.wresl
├── initial { ... }                                  ! Switchboard flags & start year
├── SEQUENCE CYCLE01 { model GENTables order 1 }     ! Generate tables (conditional)
├── SEQUENCE CYCLE02 { model SWPForecast order 2 }   ! SWP forecast (Jan–May)
├── SEQUENCE CYCLE33 { model UPSTREAM order 33 }      ! North of Delta operations
├── SEQUENCE CYCLE35 { model DELTA order 35 }         ! Delta operations
├── group Tdomain { ... }                             ! Shared conversion factors
├── group Common_All { ... }                          ! Common includes for all models
├── MODEL UPSTREAM { ... }                            ! Upstream model includes
└── MODEL DELTA { ... }                               ! Delta model includes
```

1. **Sequences** define the order in which models are solved.
2. **Models** group related variables and goals into a solvable unit for one solver pass.
3. **Groups** organize shared definitions that can be included by multiple models.
4. **Initial blocks** set up variables and conditions evaluated once before the main simulation loop.
5. **Include files** modularize definitions across multiple `.wresl` files.

This structure supports **multi-cycle solving**, where results from an upstream model feed into downstream calculations (e.g., upstream reservoir releases feeding into downstream Delta operations).

### How the Solver Works (Conceptual)
At each time step, WRIMS:
1. Evaluates all SVARs (inputs, time series, table lookups, conditional values).
2. Constructs the LP problem from DVARs (unknowns) and goals (constraints).
3. Applies the objective function weights.
4. Solves the LP to determine optimal DVAR values.
5. Advances to the next time step and repeats.

Understanding this cycle is essential: SVARs must be resolvable *before* the solver runs, while DVARs are only determined *by* the solver.

### Design Philosophy
- **Declarative:** Users declare *what* to solve, not *how* to solve it — the optimizer handles the mechanics.
- **Modularity:** Models are composed from reusable `.wresl` files via `include`.
- **Reproducibility:** All logic is explicit, text-based, and version-controllable.
- **Extensibility:** External functions (DLLs), table lookups, and time series data can be integrated.
- **Case-insensitivity:** All keywords can be written in any case (`define`, `DEFINE`, `Define`).

### Common Pitfalls Explained
- **Missing `kind` and `units`:** DVAR and timeseries definitions require `kind` and `units` metadata. Omitting them causes parser errors.
- **Incorrect string quotes:** WRESL+ uses **single quotes** (`'...'`), not double quotes. Using double quotes will cause syntax errors.
- **Wrong comment syntax:** Comments use `!` or `/* */` not `//`.
- **Wrong logical operators:** Use `.and.`, `.or.`, `.not.` — not `&&`, `||`, `!`.
- **Case order matters:** In `case`/`condition` blocks, conditions are evaluated top-to-bottom. The first matching condition is used, so place the `condition always` case last as a default.
- **Circular dependencies:** Avoid SVARs that reference each other in a cycle, as this creates unresolvable definitions.
- **Include path errors:** File paths in `include` statements use single quotes and are relative to the model root.

### WRESL vs. WRESL+
WRESL+ is a modernized superset of the original WRESL language. Key similarities and differences:

| Feature | WRESL | WRESL+ |
|---|---|---|
| Variable keywords | `define` for all variable types | `define` plus dedicated `dvar`, `svar`, `const` keywords |
| File extension | `.wresl` | `.wresl` (same extension, different parser) |
| Comments | `!`, `//`, and `/* ... */` | `!` and `/* ... */` |
| Case-sensitivity | Case-insensitive | Case-insensitive |
| Model organization | `sequence`, `model`, `include` | Adds `group` and `initial` blocks |
| Multi-cycle support | Built-in via `sequence` / `model` | Same, plus optional `timestep` (1MON/1DAY) per cycle |
| Objective function | Defined through goal weights | Dedicated `objective` keyword with weight syntax |
| Conditional includes | Not supported | `if` / `elseif` / `else` blocks |
| Goal syntax | Full `lhs`/`rhs`, `case`/`condition`, `penalty`/`constrain` | Same |
| External functions | Supported via `external` keyword (`DLL`, `F90`) | Supported via `external` keyword (`DLL`, `F90`) |
| Future arrays | Not supported | `dvar(N)`, `svar(N)`, `goal(N)` for multi-step definitions |

In practice, WRESL+ files are backward-compatible with most WRESL constructs, but WRESL+ adds structured model organization (`group`, `initial`), a dedicated `objective` keyword, conditional includes, future arrays, and explicit variable-type keywords (`dvar`, `svar`) that the original WRESL lacks.

---


## Tutorials
*Learning-oriented: step-by-step lessons for beginners.*

Before starting these tutorials, review the [Explanation](#explanation) section above to understand the core concepts of SVARs, DVARs, goals, and model structure.

### Getting Started with WRESL+

#### Prerequisites
- Download and install the WRIMS GUI from the [official source](https://data.cnra.ca.gov/dataset/wrims-2-gui/resource/93b6243c-57eb-47b5-ad62-7209c0acdfb6).
- Ensure Java is installed and available in your PATH.
- Familiarize yourself with the WRIMS IDE (Eclipse-based environment).

#### Your First Model
This tutorial walks through a simple study adapted from the WRESL+ Language Reference (2018) Quick Start example.

1. Create a main file `main.wresl` with sequences, models, variables, goals, and an objective:
   ```text
   ! Source: WRESL+ Language Reference (2018), Quick Start
   sequence CYCLE1 { model First order 1 }
   sequence CYCLE2 { model Second order 2 }

   model First {
     timeseries I01 { kind 'FLOW-INFLOW' units 'TAF' }
     dvar X1 { std kind 'FLOW-CHANNEL' units 'CFS' }
     dvar X2 { std kind 'FLOW-CHANNEL' units 'CFS' }
     svar C { value I02*5 }
     goal Test { X1 + X2 < C }
     include 'allocation\test1.wresl'
     objective XGroup {
       [ X1, 10 ]
       [ X2, 20 ]
     }
   }

   model Second {
     timeseries I02 { kind 'FLOW-INFLOW' units 'TAF' }
     dvar Y1 { std kind 'FLOW-CHANNEL' units 'CFS' }
     dvar Y2 { std kind 'FLOW-CHANNEL' units 'CFS' }
     svar X1_Upstream { value X1[First] }
     goal Test2 { Y1 + Y2 < I02 + X1_Upstream }
     include 'allocation\test2.wresl'
     objective YGroup {
       [ Y1, 10 ]
       [ Y2, 20 ]
     }
   }
   ```
2. Run the model using the WRIMS GUI or command-line interface.
3. Examine the output: the solver determines `X1`, `X2`, `Y1`, and `Y2` values that satisfy the constraints while optimizing the weighted objectives.

#### What Just Happened?
- `timeseries I01 { kind 'FLOW-INFLOW' units 'TAF' }` declared a **time series input** read from HEC-DSS.
- `dvar X1 { std kind 'FLOW-CHANNEL' units 'CFS' }` created a **decision variable** — an unknown for the solver.
- `svar C { value I02*5 }` created a **state variable** (known value computed from inputs).
- `goal Test { X1 + X2 < C }` created a **constraint** the solver must satisfy.
- `objective XGroup { [X1, 10] [X2, 20] }` defined an **objective function** with weighted decision variables.
- `svar X1_Upstream { value X1[First] }` in the second model references the result of `X1` from the `First` cycle — demonstrating **cross-cycle variable access**.
- The `sequence` / `model` / `include` structure told WRIMS which files to load and in what order.

### Tutorial: Shasta Reservoir Mass Balance
This example demonstrates a realistic reservoir mass balance using actual variable definitions and the continuity goal from CalSim3.

First, the variables are declared across system files:
```text
! Source: CalSim3 DCR 9.5.0 — arcs-Inflows.wresl, arcs-Reservoirs.wresl, arcs-Channels.wresl, arcs-Diversions.wresl

! State variable: known input
define I_SHSTA {timeseries kind 'INFLOW' units 'TAF' convert 'CFS'}   ! Sacramento River inflow

! Decision variables
define S_SHSTA       {std kind 'STORAGE' units 'TAF'}                 ! reservoir storage
define S_SHSTAlevel1 {value 550}                                      ! dead pool (TAF)
define S_SHSTAlevel6 {value 4552}                                     ! full pool (TAF)
define C_SHSTA       {std kind 'CHANNEL' units 'CFS'}                 ! channel flow below dam
define D_SHSTA_WTPJMS {std kind 'DIVERSION' units 'CFS'}              ! diversion to water treatment plant
define E_SHSTA       {lower unbounded kind 'EVAPORATION' units 'CFS'} ! evaporation loss
```

Then the continuity constraint ties them together:
```text
! Mass balance at Shasta: inflow - outflows = change in storage
goal continuitySHSTA {
  I_SHSTA - C_SHSTA - D_SHSTA_WTPJMS - E_SHSTA = S_SHSTA * taf_cfs - S_SHSTA(-1) * taf_cfs + 0.
}
```

**Key points:**
- CalSim models conventionally use **CFS** (cubic feet per second) for flows and **TAF** (thousand acre-feet) for storage. The built-in conversion factor `taf_cfs` converts TAF to CFS for the current time step, ensuring consistent units within constraints.
- `S_SHSTA(-1)` references the storage from the previous time step.
- The `+ 0.` at the end is a CalSim convention for consistency in the continuity equations.

### Tutorial: Conditional Operations with Case/Condition
Real-world operations often depend on conditions such as hydrology, time of year, or regulatory requirements. This example from the WRESL+ Language Reference (2018) assigns values based on the current month:

```text
! Source: WRESL+ Language Reference (2018), Conditional Value State Variable
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
```

Here is a real-world example from the CalSim3 study that determines CVP allocation based on water supply index:

```text
! Source: CalSim3 DCR 9.5.0 — cvpcut.wresl
define WSI_NOD_Alloc {
  case MarchAprilMay {
    condition month >= MAR .and. month <= MAY
    select Alloc from WSI_CVP_NODAg given WSI=WSI_CVP_NOD use linear where month=month
  }
  case otherwise {
    condition always
    value 0.
  }
}
```

And another from CVP operations that sets seasonal balancing targets:

```text
! Source: CalSim3 DCR 9.5.0 — CVP_Balancing.wresl
define S_WKYTN_Septarget {
  case dry {
    condition wyt_SAC == 5
    value 180
  }
  case other {
    condition always
    value 235
  }
}
```

**Key points:**
- Conditions are evaluated top-to-bottom; the first match is used.
- Always end with `condition always` as a default/fallback.
- You can have as many `case` blocks as needed.

### Tutorial: Multi-File Model Organization
As models grow, splitting definitions across files keeps things manageable. Here is an excerpt from the actual CalSim3 main file showing how the study is organized:

**mainCS3_ReOrg_UWplusVF.wresl** (entry point — abbreviated):
```text
! Source: CalSim3 DCR 9.5.0 — mainCS3_ReOrg_UWplusVF.wresl

SEQUENCE CYCLE33 {
    model   UPSTREAM
    condition simulateSacramentoValley >= 0.5
    order   33
}
SEQUENCE CYCLE35 {
    model   DELTA
    condition simulateSacramentoValley >= 0.5
    order   35
}

group Tdomain {
    define SGPHIGH {value 77777}
    define daysindv { alias daysin kind 'DAYS' units 'days'}
    define cfs_cfm  { value daysin * 86400.0}
}

group Common_All {
    include 'CVGroundwater\CalSim3GWregionIndex.wresl'
    include 'Other\NewFacilitySwitches.wresl'
    include 'Other\wytypes\WyTypesGeneral.wresl'
    include 'CVGroundwater\CVGroundwater_ExtFuncs.wresl'
    include 'NorthOfDelta\hydrology\forecast\forecast.wresl'
    include 'System\System_Sac_cycle1.wresl'
}
```

**System/SystemTables_Sac/arcs-Reservoirs.wresl** (variable declarations):
```text
! Source: CalSim3 DCR 9.5.0 — arcs-Reservoirs.wresl
define S_SHSTAlevel1 {value 550}
define S_SHSTA_1     {std kind 'STORAGE-ZONE' units 'TAF'}
define S_SHSTAlevel2 {timeseries kind 'STORAGE-LEVEL' units 'TAF'}
define S_SHSTA_2     {std kind 'STORAGE-ZONE' units 'TAF'}
define S_SHSTAlevel6 {value 4552}
define S_SHSTA_6     {std kind 'STORAGE-ZONE' units 'TAF'}
define S_SHSTA       {std kind 'STORAGE' units 'TAF'}           !SHASTA LAKE
define E_SHSTA       {lower unbounded kind 'EVAPORATION' units 'CFS'}
```

**System/SystemTables_Sac/constraints-Connectivity.wresl** (goals):
```text
! Source: CalSim3 DCR 9.5.0 — constraints-Connectivity.wresl
goal continuitySHSTA  {I_SHSTA - C_SHSTA - D_SHSTA_WTPJMS - E_SHSTA = S_SHSTA * taf_cfs - S_SHSTA(-1) * taf_cfs + 0.}
goal continuityKSWCK  {C_SAC301 + SR_02_KSWCK + SR_03_KSWCK + R_03_PU1_KSWCK - C_KSWCK - E_KSWCK
                       = S_KSWCK * taf_cfs - S_KSWCK(-1) * taf_cfs + 0.}
```

This pattern — separating variables from goals, organizing by geographic location and system component, and using `include` with `group` — is the standard way to organize WRESL+ models.

---


## Practical Guides
*Task-oriented: recipes for accomplishing specific goals.*

Each guide below addresses a specific task. Skim the headings to find what you need.

### How to Define State Variables (SVARs)
State variables hold known or computed values. They can be defined using `define` or `svar`:

```text
! Simple fixed value — Source: WRESL+ Language Reference (2018)
svar X {value 9.0}
svar Y {value max(X, 5.0)}

! Time series from HEC-DSS — Source: CalSim3 DCR 9.5.0 — arcs-Inflows.wresl
define I_SHSTA {timeseries kind 'INFLOW' units 'TAF' convert 'CFS'}

! Table lookup — Source: CalSim3 DCR 9.5.0 — arcs-Reservoirs.wresl
define A_SHSTAlast {
  select area from res_info
    given storage = 1000*S_SHSTA(-1)
    use linear
    where res_num = 4
}

! Case-based (conditional) — Source: CalSim3 DCR 9.5.0 — CVP_Balancing.wresl
define S_WKYTN_Septarget {
  case dry {
    condition wyt_SAC == 5
    value 180
  }
  case other {
    condition always
    value 235
  }
}

! Summation over an index range — Source: WRESL+ Language Reference (2018)
svar Z {sum(i=1, 5, 1) S03(-i) + I10(-i)}

! Summation looking ahead — Source: WRESL+ Language Reference (2018)
svar OroDivEst {sum(i=0,sep-month,1) D_PWR(i)}
```

### How to Define Decision Variables (DVARs)
Decision variables are determined by the optimization solver:

```text
! Standard decision variable (non-negative by default) — Source: WRESL+ Language Reference (2018)
dvar C_Delta_SWP {std kind 'FLOW-CHANNEL' units 'CFS'}

! With explicit bounds — Source: WRESL+ Language Reference (2018)
dvar QPD {lower -100. upper unbounded kind 'FLOW' units 'CFS'}

! Binary decision variable — Source: WRESL+ Language Reference (2018)
dvar B {binary kind 'BINARY' units 'NA'}

! Integer decision variable — Source: WRESL+ Language Reference (2018)
dvar Integer2 {integer lower 0 upper 3 kind 'INTEGER' units 'NA'}

! Integer from CalSim3 — Source: CalSim3 DCR 9.5.0 — AprMayExport.wresl
define Pulse_int_Apr {INTEGER std kind 'INTEGER' units 'NONE'}
```

### How to Define Constants

Constants can be defined using `define` with a `value`; this approach works in both WRESL and WRESL+:
```text
! Source: CalSim3 DCR 9.5.0 — arcs-Reservoirs.wresl
define S_SHSTAlevel1 {value 550}    ! dead pool (TAF)
define S_SHSTAlevel6 {value 4552}   ! full pool (TAF)
```

### How to Define Goals (Constraints)
Goals define constraints for the optimizer:

```text
! Simple equality constraint: mass balance — Source: CalSim3 DCR 9.5.0 — constraints-Connectivity.wresl
goal continuitySHSTA {
  I_SHSTA - C_SHSTA - D_SHSTA_WTPJMS - E_SHSTA = S_SHSTA * taf_cfs - S_SHSTA(-1) * taf_cfs + 0.
}

! Goal with lhs/rhs and penalty structure — Source: WRESL+ Language Reference (2018)
goal NMTest {
  lhs X + Y
  rhs Z
  lhs > rhs constrain          ! hard: do not exceed
  lhs < rhs penalty 0           ! soft: free slack
}

! Goal with case-based conditions — Source: WRESL+ Language Reference (2018)
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
```

### How to Define Aliases
Aliases provide alternative names for computed expressions and are useful for output:

```text
! Source: CalSim3 DCR 9.5.0 — SoDeltaChannels.wresl
define OMFlow {alias C_OMR014 kind 'FLOW-CHANNEL' units 'CFS'}

! Alias with an expression — Source: WRESL+ Language Reference (2018)
alias Estlim3 {max(300., Est_rel) kind 'DEBUG' units 'CFS'}
```

### How to Define Objectives
Objectives specify what the model should optimize, with weighted decision variables:

```text
! Source: WRESL+ Language Reference (2018)
objective XGroup {[X1, 10] [X2, 20]}

! Common weight shorthand — Source: WRESL+ Language Reference (2018)
objective XGroup {weight 10 variable X1 X2 X3}

! Real CalSim3 objective (abbreviated) — Source: CalSim3 DCR 9.5.0 — Weight-table.wresl
Objective obj_SYS = {
  [S_TRNTY_1, 200000*taf_cfs],
  [S_TRNTY_2, 93*taf_cfs],
  [S_SHSTA_1, 200000*taf_cfs],
  [S_SHSTA_2, 93*taf_cfs]
}
```

### How to Include Files
Use `include` to modularize your model across multiple files:

```text
! Source: CalSim3 DCR 9.5.0 — mainCS3_ReOrg_UWplusVF.wresl
include 'CVGroundwater\CalSim3GWregionIndex.wresl'
include 'Other\NewFacilitySwitches.wresl'

! Local include: definitions scoped to the current model only
include [local] 'local_adjustments.wresl'
```

### How to Structure a Model
A complete WRESL+ model uses `sequence`, `model`, and `group` blocks:

```text
! Source: CalSim3 DCR 9.5.0 — mainCS3_ReOrg_UWplusVF.wresl (abbreviated)

! Initial block for pre-simulation setup
initial {
  include 'System\SystemTables_Sac\arcs-Reservoirs.wresl'
  include 'CVGroundwater\CVGroundwater_init1_SJRBASE.wresl'
}

SEQUENCE CYCLE01 {
  model   GENTables
  order   1
}
SEQUENCE CYCLE02 {
  model   SWPForecast
  order   2
}
SEQUENCE CYCLE33 {
  model   UPSTREAM
  condition simulateSacramentoValley >= 0.5
  order   33
}
SEQUENCE CYCLE35 {
  model   DELTA
  condition simulateSacramentoValley >= 0.5
  order   35
}

! Shared definitions available to all models
group Common_All {
  include 'CVGroundwater\CalSim3GWregionIndex.wresl'
  include 'Other\NewFacilitySwitches.wresl'
  include 'Other\wytypes\WyTypesGeneral.wresl'
  include 'NorthOfDelta\hydrology\forecast\forecast.wresl'
}

model UPSTREAM {
  include group Tdomain
  include group Common_All
  include 'System\System_Sac_cycle1.wresl'
}
```

### How to Use Table Lookups
Look up values from external data tables:

```text
! Simple table lookup — Source: WRESL+ Language Reference (2018)
svar D_M {select DCU_M from DCU_MAC where MON = jan}

! Lookup with interpolation — Source: CalSim3 DCR 9.5.0 — arcs-Reservoirs.wresl
define A_SHSTAlast {
  select area from res_info
    given storage = 1000*S_SHSTA(-1)
    use linear
    where res_num = 4
}
```

> **Note:** Using single-quoted string values in `where` clauses (e.g., `reservoir = 'SHSTA'`) is a WRESL+ feature. In classic WRESL, string matching in table lookups was handled differently.

### How to Use Conditional Includes
Include files conditionally based on runtime logic:

```text
! Source: CalSim3 DCR 9.5.0 — system_Sac2.wresl
if simulateSacVA_TisdaleWeir < 0.5 {
  include 'SystemTables_Sac\constraints-Weirs2.wresl'
}

! Source: WRESL+ Language Reference (2018)
if A + B > 15. {
  include 'swp_dellogic\allocation\co_extfcn.wresl'
  include 'Delta\DeltaExtFuncs_7inpANN.wresl'
}
```

### How to Use Unit Conversions
WRESL+ provides built-in conversion identifiers for flows and volumes:

```text
! Convert CFS to TAF (thousand acre-feet)
define I_SHSTA_TAF {value I_SHSTA * cfs_taf}

! Convert TAF to CFS
define C_SHSTA_CFS {value C_SHSTA_TAF * taf_cfs}

! Convert AF to CFS
define D_REDBLF_CFS {value D_REDBLF_AF * af_cfs}
```

### How to Reference Variables Across Model Cycles
When a downstream model needs a value computed in an upstream cycle:

```text
! Cross-cycle reference — Source: WRESL+ Language Reference (2018)
svar Y {value X[Upstream] + 100.}

! Fixed operations from upstream cycle — Source: CalSim3 DCR 9.5.0 — ITP/FixedOperations.wresl
goal fixC_CAA003_EXP1 {
  lhs C_CAA003_EXP1
  rhs C_CAA003_EXP1[WHEELJPOD]
  lhs < rhs penalty 999.
  lhs > rhs constrain
}

! Previous time step reference
define S_SHSTA_prev {value S_SHSTA(-1)}
```

### How to Use the Range Function
Check if the current month falls within a range:

```text
! Source: WRESL+ Language Reference (2018) — range() wraps around the water year
define floodSeason {
  case inRange {
    condition range(month, oct, mar) == 1
    value 1
  }
  case outOfRange {
    condition always
    value 0
  }
}
```

---


## Reference
*Information-oriented: technical details and valid syntax.*

This section is a comprehensive reference for the WRESL+ language. Use it to look up exact syntax, available keywords, and supported functions.

### WRESL+ Syntax Reference

#### Keywords
The following keywords are recognized by the WRESL+ parser (case-insensitive):

| Category | Keywords |
|---|---|
| Variable definition | `define`, `svar`, `dvar`, `const` |
| Variable content | `value`, `timeseries`, `alias`, `external`, `std`, `integer`, `binary` |
| Bounds | `lower`, `upper`, `unbounded` |
| Metadata | `kind`, `units`, `convert` |
| Goals/Objectives | `goal`, `objective`, `lhs`, `rhs`, `penalty`, `constrain` |
| Table lookups | `select`, `from`, `given`, `use`, `where` |
| Summation | `sum` |
| Conditions | `condition`, `always`, `case` |
| Logical operators | `.and.`, `.or.`, `.not.` |
| Control flow | `if`, `elseif`, `else` |
| Model structure | `model`, `group`, `sequence`, `initial`, `include`, `order`, `timestep` |
| Special identifiers | `month`, `wateryear`, `day`, `daysin`, `daysinmonth`, `daysintimestep`* |
| Unit conversions | `taf_cfs`, `cfs_taf`, `af_cfs`, `cfs_af` |
| Calendar months | `jan`, `feb`, `mar`, `apr`, `may`, `jun`, `jul`, `aug`, `sep`, `oct`, `nov`, `dec` |
| Previous months | `prevjan`, `prevfeb`, ..., `prevdec` |

#### Variable Definition Syntax

> **TimeArray** — An optional parenthesized integer or identifier, e.g., `(12)`, that declares the variable as a time array with multiple slots (one per sub-timestep). When omitted, the variable holds a single value per timestep. This is used for variables that need to store values across multiple sub-periods within a timestep.

**State Variable (SVAR):**
```
define|svar [(TimeArray)] [[local]] Name { SvarContent }
```
Where `SvarContent` is one of:
- `value Expression`
- `timeseries ['BPart'] kind 'K' units 'U' [convert 'C']`
- `select Column from Table [given Assign use ID] [where Assignments]`
- `sum(i=Expr1,Expr2) Expression`
- One or more `case Name { condition LogicalExpr|always  value|select|sum ... }`

Examples:
```text
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
```

**Decision Variable (DVAR):**
```
define|dvar [(TimeArray)] [[local]] Name { DvarContent }
```
Where `DvarContent` is one of:
- `std kind 'K' units 'U'`
- `[lower Expr] [upper Expr] kind 'K' units 'U'`
- `binary kind 'K' units 'U'`
- `integer std kind 'K' units 'U'`
- `integer [lower Expr] [upper Expr] kind 'K' units 'U'`

Examples:
```text
! Source: WRESL+ Language Reference (2018)
dvar C_Delta_SWP {std kind 'FLOW-CHANNEL' units 'CFS'}
dvar QPD {lower -100. upper unbounded kind 'FLOW' units 'CFS'}
dvar Integer2 {integer lower 0 upper 3 kind 'INTEGER' units 'NA'}
```

**Constant:**
```
const [[local]] Name { Number }
```
Example:
```text
! Source: CalSim3 DCR 9.5.0 — arcs-Reservoirs.wresl
define S_SHSTAlevel1 {value 550}
```

**Alias:**
```
define [(TimeArray)] [[local]] Name { alias Expression [kind 'K'] [units 'U'] }
```
Example:
```text
! Source: CalSim3 DCR 9.5.0 — SoDeltaChannels.wresl
define OMFlow {alias C_OMR014 kind 'FLOW-CHANNEL' units 'CFS'}

! Source: WRESL+ Language Reference (2018)
alias Estlim3 {max(300., Est_rel) kind 'DEBUG' units 'CFS'}
```

**External:**
```
define [[local]] Name { external LibraryName[.dll|.f90] }
```
Where `LibraryName` is the filename of an external DLL or Fortran 90 library (e.g., `interfacetogw_x64.dll`). The external function is called by the evaluator at runtime.

Example:
```text
! Source: CalSim3 DCR 9.5.0 — CVGroundwater_ExtFuncs.wresl
define InitGWSystem {EXTERNAL interfacetogw_x64.dll}
```

#### Goal Syntax

**Simple goal:**
```
goal [(TimeArray)] [[local]] Name { Expression < | > | = Expression }
```
Examples:
```text
! Source: CalSim3 DCR 9.5.0 — constraints-Connectivity.wresl
goal continuitySHSTA {I_SHSTA - C_SHSTA - D_SHSTA_WTPJMS - E_SHSTA = S_SHSTA * taf_cfs - S_SHSTA(-1) * taf_cfs + 0.}

! Source: WRESL+ Language Reference (2018)
goal Test {X > Y}
```

**Complex goal with LHS/RHS:**
```
goal [(TimeArray)] [[local]] Name {
  lhs Expression
  rhs Expression
  [lhs > rhs  constrain | penalty Expression]
  [lhs < rhs  constrain | penalty Expression]
}
```
Example:
```text
! Source: WRESL+ Language Reference (2018)
goal NMTest {
  lhs X + Y
  rhs Z
  lhs > rhs constrain
  lhs < rhs penalty 0
}
```

**Complex goal with cases:**
```
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
```
Example:
```text
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
```

#### Objective Syntax
```
objective [[local]] Name [=] {
  [VarRef [(TimeArray)], WeightExpression],
  ...
}
```
When a DVAR is declared as a time array (e.g., `dvar(12) myVar {...}`), the `(TimeArray)` in the objective entry must match that declaration to correctly assign the weight to each sub-timestep slot. For non-time-array DVARs, omit it.

Example:
```text
! Source: WRESL+ Language Reference (2018)
objective XGroup {[X1, 10] [X2, 20]}

! Source: CalSim3 DCR 9.5.0 — Weight-table.wresl (abbreviated)
Objective obj_SYS = {
  [S_TRNTY_1, 200000*taf_cfs],
  [S_TRNTY_2, 93*taf_cfs],
  [S_SHSTA_1, 200000*taf_cfs],
  [S_SHSTA_2, 93*taf_cfs]
}
```

#### Model Structure Syntax

**Sequence:**
```
sequence Name { model ModelRef [condition LogicalExpr] [order INT] [timestep 1MON|1DAY] }
```
Example:
```text
! Source: WRESL+ Language Reference (2018)
sequence CYCLE1 {model Upstream order 1}
sequence CYCLE4 {model Test2 condition MON==jun order 4}

! Source: CalSim3 DCR 9.5.0 — mainCS3_ReOrg_UWplusVF.wresl
SEQUENCE CYCLE33 {model UPSTREAM condition simulateSacramentoValley >= 0.5 order 33}
```

**Model:**
```
model Name { (Pattern | IfIncItems)+ }
```
Where `Pattern` is one or more of the following content items: `include` file, `define`/`svar`, `dvar`, `goal`, `alias`, `objective`, `external`, or `timeseries` declarations. `IfIncItems` are conditional include blocks (`if`/`elseif`/`else` wrapping any of the above).
Example:
```text
! Source: CalSim3 DCR 9.5.0 — mainCS3_ReOrg_UWplusVF.wresl (abbreviated)
model UPSTREAM {
  include group Tdomain
  include group Common_All
  include 'System\System_Sac_cycle1.wresl'
  if simulateSacVA_TisdaleWeir < 0.5 {
    include 'SystemTables_Sac\constraints-Weirs2.wresl'
  }
}
```

**Group:**
```
group Name { (Pattern | IfIncItems)+ }
```
Example:
```text
! Source: CalSim3 DCR 9.5.0 — mainCS3_ReOrg_UWplusVF.wresl
group Common_All {
  include 'CVGroundwater\CalSim3GWregionIndex.wresl'
  include 'Other\NewFacilitySwitches.wresl'
  include 'Other\wytypes\WyTypesGeneral.wresl'
}
```

**Initial:**
```
initial { (SimpleValueSvar | LookupTableSvar | IncludeStatement)+ }
```
The `initial` block runs once before the main simulation loop. Per the WRESL+ Language Reference, the `initial` block allows only **simple value state variables** and **lookup table state variables**. In practice, CalSim3 also uses `include` statements within `initial` to bring in files containing these variable types. It is typically used to set flags, switches, and starting conditions that are evaluated once before the simulation begins.
Example:
```text
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
```

**Include:**
```
include [[local]] 'filepath.wresl'
include model ModelName
include group GroupName
```

#### Data Types
- Integer (e.g., `42`, `550`)
- Float (e.g., `3.14`, `.5`, `1.98347`)

#### Operators

| Type | Operators |
|---|---|
| Arithmetic | `+`, `-`, `*`, `/` |
| Comparison | `==`, `/=` (not equal), `<`, `>`, `<=`, `>=` |
| Logical | `.and.`, `.or.`, `.not.` |

#### Comments
Single-line comments begin with `!`:
```text
! Sacramento River inflow to Shasta
define I_SHSTA {value 10000}   ! CFS
```

#### Strings
Strings use **single quotes**:
```text
kind 'FLOW-CHANNEL'
units 'CFS'
include 'System\SystemTables_Sac\arcs-Reservoirs.wresl'
```

### Supported Functions
| Function | Description | Example |
|---|---|---|
| `min(a, b, ...)` | Minimum of values | `min(I_SHSTA, maxCapacity)` |
| `max(a, b, ...)` | Maximum of values | `max(C_SHSTA, 0)` |
| `abs(x)` | Absolute value | `abs(S_SHSTA - target)` |
| `round(x)` | Round to nearest integer | `round(ratio)` |
| `int(x)` | Truncate to integer | `int(2.9)` → 2 |
| `mod(a, b)` | Modulo (remainder) | `mod(month, 12)` |
| `pow(a, b)` | Power (a raised to b) | `pow(base, exp)` |
| `log(x)` | Logarithm (also `log10`) | `log(value)` |
| `sin(x)` | Sine | `sin(angle)` |
| `cos(x)` | Cosine | `cos(angle)` |
| `tan(x)` | Tangent | `tan(angle)` |
| `cot(x)` | Cotangent | `cot(angle)` |
| `asin(x)` | Arc sine | `asin(ratio)` |
| `acos(x)` | Arc cosine | `acos(ratio)` |
| `atan(x)` | Arc tangent | `atan(slope)` |
| `acot(x)` | Arc cotangent | `acot(value)` |
| `range(var, start, end)` | Check if month/year is in range | `range(month, oct, mar)` |

### Special Built-in Identifiers
| Identifier | Description |
|---|---|
| `month` | Current simulation month (numeric) |
| `wateryear` | Current water year |
| `day` | Current simulation day |
| `daysin` / `daysinmonth` | Number of days in current month |
| `daysintimestep` | Number of days in current timestep. **Note:** This is a built-in evaluator keyword, not a parser-level keyword. It is case-sensitive and must be written in lowercase only. |
| `jan` ... `dec` | Calendar month constants |
| `prevjan` ... `prevdec` | Previous year's month references |
| `taf_cfs` | TAF to CFS conversion factor for the current timestep |
| `cfs_taf` | CFS to TAF conversion factor for the current timestep |
| `af_cfs` | AF to CFS conversion factor for the current timestep |
| `cfs_af` | CFS to AF conversion factor for the current timestep |
| `$m` | Multi-step reference |
| `i` | Loop variable (used inside `sum` expressions) |

### Variable Cross-References
| Syntax | Meaning | Example |
|---|---|---|
| `varName[modelName]` | Reference variable as solved in another model cycle | `C_CAA003_EXP1[WHEELJPOD]` |
| `varName(-1)` | Reference variable from the previous time step | `S_SHSTA(-1)` |
| `varName[modelName](offset)` | Cross-model reference with time offset | `EiExpCtrl[PRESETUP](-1)` |
| `varName[-1]` | Index-based variable reference | `S_SHSTA[-1]` |

---


## Debugging Tips
*Advice for solving common issues.*

### Common Error Messages

| Error Message | Likely Cause | Solution |
|---|---|---|
| Undefined variable | Variable not declared or typo in name | Check spelling; ensure variable is defined with `define`, `svar`, `dvar`, or `const` (e.g., `I_SHSTA` vs. `I_Shsta`) |
| Syntax error | Missing brace `{}`, wrong quotes, or invalid keyword | Review line for correct syntax; use single quotes `'...'` for strings |
| Circular reference detected | SVARs reference each other in a loop | Refactor definitions to break the cycle |
| File not found | Incorrect path in `include` statement | Check the file path (single-quoted, relative to model root, e.g., `'System\SystemTables_Sac\arcs-Reservoirs.wresl'`) |
| Missing kind/units | DVAR or timeseries missing required metadata | Add `kind '...'` and `units '...'` to the definition |
| Infeasible solution | Constraints cannot all be satisfied simultaneously | Review goals for conflicting constraints; consider using `penalty` instead of `constrain` |

### Troubleshooting Workflow
1. Read the error message carefully and note the file name and line number.
2. Check the referenced line for syntax issues (braces, quotes, keywords).
3. Verify all referenced variables are declared before use.
4. Simplify your model to isolate the issue and comment out sections with `!`.
5. Check that `case` blocks end with a `condition always` default.
6. Verify `include` file paths exist and are correctly quoted.
7. Consult the [Reference](#reference) and [Explanation](#explanation) sections.

### Frequently Asked Questions (FAQ)

**Q: How do I write comments in WRESL+?**
A: Use `!` for single-line comments — everything after `!` on that line is ignored. For multi-line comments, use `/* ... */` to wrap a block of text. Note: the original WRESL also supported `//` for single-line comments, but `//` is not part of the WRESL+ specification.

**Q: How do I include external data?**
A: Use `timeseries` for HEC-DSS data (e.g., `define I_SHSTA {timeseries kind 'FLOW' units 'CFS'}`) or `select...from...where` for lookup tables. Use `include` to bring in other `.wresl` files.

**Q: What kind of quotes does WRESL+ use?**
A: Single quotes only: `'FLOW-CHANNEL'`. Double quotes are not valid.

**Q: What are the logical operators?**
A: `.and.`, `.or.`, `.not.` — not `&&`, `||`, `!`.

**Q: How do I make a variable local to a model cycle?**
A: Add the `[local]` modifier: `define [local] myVar {value 42}` or `include [local] 'file.wresl'`.

**Q: Where can I find more examples?**
A: See the Tutorials and How-to Guides sections above, or examine existing CalSim3 study files for real-world patterns.

---


## Contributing to the Documentation
If you find errors or have suggestions for improving this documentation:
1. Open an issue or pull request on the [wrims-gui repository](https://github.com/CentralValleyModeling/wrims-gui).
2. Reference the [WRESL+ Xtext grammar](https://github.com/CentralValleyModeling/wrims-gui/blob/main/xtext-editor/src/main/resources/gov/ca/dwr/wresl/xtext/editor/WreslEditor.xtext) as the authoritative syntax source.
3. Follow the [Diátaxis](https://diataxis.fr/) framework when adding content — place material in the appropriate section (Explanation, Tutorials, How-to, or Reference).
4. Use clear, descriptive variable names in examples for consistency.


