.. _how-to-index:

How-To
======
*Task-oriented: recipes for accomplishing specific goals.*

Each guide below addresses a specific task. Skim the headings to find what you need.

How to Define State Variables
-----------------------------

State variables hold known or computed values. They can be defined using `define` or `svar`:

.. code-block:: wresl

    ! Simple fixed value — Source: WRESL+ Language Reference (2018)
    svar X {value 9.0}
    svar Y {value max(X, 5.0)}

    ! Time series from HEC-DSS — Source: CalSim3 DCR 9.5.0 — arcs-Inflows.wresl
    define I_SHSTA {timeseries kind 'INFLOW' units 'TAF' convert 'CFS'}

    ! Table lookup — Source: CalSim3 DCR 9.5.0 — arcs-Reservoirs.wresl
    define A_SHSTAlast {
        select area 
        from res_info
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


How to Define Decision Variables
--------------------------------

Decision variables have their values determined by the optimization solver:

.. code-block:: wresl

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


How to Define Constants
-----------------------

Constants can be defined using `define` with a `value`; this approach works in both WRESL and WRESL+:
.. code-block:: wresl

    ! Source: CalSim3 DCR 9.5.0 — arcs-Reservoirs.wresl
    define S_SHSTAlevel1 {value 550}    ! dead pool (TAF)
    define S_SHSTAlevel6 {value 4552}   ! full pool (TAF)


How to Define Goals
-------------------

Goals define constraints for the optimizer:

.. code-block:: wresl

    ! Simple equality constraint: mass balance — Source: CalSim3 DCR 9.5.0 — constraints-Connectivity.wresl
    goal continuitySHSTA {
        I_SHSTA - C_SHSTA - D_SHSTA_WTPJMS - E_SHSTA = S_SHSTA * taf_cfs - S_SHSTA(-1) * taf_cfs + 0.
    }

    ! Goal with lhs/rhs and penalty structure — Source: WRESL+ Language Reference (2018)
    goal NMTest {
        lhs X + Y
        rhs Z
        lhs > rhs constrain     ! hard: do not exceed
        lhs < rhs penalty 0     ! soft: free slack
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


How to Define Aliases
---------------------

Aliases provide alternative names for computed expressions and are useful for output:

.. code-block:: wresl

    ! Source: CalSim3 DCR 9.5.0 — SoDeltaChannels.wresl
    define OMFlow {
        alias C_OMR014 
        kind 'FLOW-CHANNEL' 
        units 'CFS'
    }

    ! Alias with an expression — Source: WRESL+ Language Reference (2018)
    alias Estlim3 {
        max(300., Est_rel) 
        kind 'DEBUG' 
        units 'CFS'
    }


How to Define Objectives
------------------------

Objectives specify what the model should optimize, with weighted decision variables:

.. code-block:: wresl

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


How to Include Files
--------------------

Use `include` to modularize your model across multiple files:

.. code-block:: wresl

    ! Source: CalSim3 DCR 9.5.0 — mainCS3_ReOrg_UWplusVF.wresl
    include 'CVGroundwater\CalSim3GWregionIndex.wresl'
    include 'Other\NewFacilitySwitches.wresl'


How to Structure a Model
------------------------

A complete WRESL+ model uses `sequence`, `model`, and `group` blocks:

.. code-block:: wresl

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


How to Use Table Lookups
------------------------

Look up values from external data tables:

.. code-block:: wresl

    ! Simple table lookup — Source: WRESL+ Language Reference (2018)
    svar D_M {select DCU_M from DCU_MAC where MON = jan}

    ! Lookup with interpolation — Source: CalSim3 DCR 9.5.0 — arcs-Reservoirs.wresl
    define A_SHSTAlast {
        select area 
        from res_info
        given storage = 1000*S_SHSTA(-1)
        use linear
        where res_num = 4
    }

.. note:: Using single-quoted string values in `where` clauses (e.g., `reservoir = 'SHSTA'`) is a WRESL+ feature. In classic WRESL, string matching in table lookups was handled differently.

How to Use Conditional Includes
-------------------------------

Include files conditionally based on runtime logic:

.. code-block:: wresl

    ! Source: CalSim3 DCR 9.5.0 — system_Sac2.wresl
    if simulateSacVA_TisdaleWeir < 0.5 {
        include 'SystemTables_Sac\constraints-Weirs2.wresl'
    }

    ! Source: WRESL+ Language Reference (2018)
    if A + B > 15. {
        include 'swp_dellogic\allocation\co_extfcn.wresl'
        include 'Delta\DeltaExtFuncs_7inpANN.wresl'
    }


How to Use Unit Conversions
---------------------------

WRESL+ provides built-in conversion identifiers for flows and volumes:

.. code-block:: wresl

    ! Convert CFS to TAF (thousand acre-feet)
    define I_SHSTA_TAF {value I_SHSTA * cfs_taf}

    ! Convert TAF to CFS
    define C_SHSTA_CFS {value C_SHSTA_TAF * taf_cfs}

    ! Convert AF to CFS
    define D_REDBLF_CFS {value D_REDBLF_AF * af_cfs}


How to Reference Variables Across Model Cycles
----------------------------------------------

When a downstream model needs a value computed in an upstream cycle:

.. code-block:: wresl

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


How to Use the Range Function
-----------------------------

Check if the current month falls within a range:

.. code-block:: wresl

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

