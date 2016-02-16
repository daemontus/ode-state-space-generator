package cz.muni.fi.ode.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ParserTest {

    val model1 = """
###############################################################################
# type 31 of TCP biodegradation model (using of DhaA from ????) #
###############################################################################

VARS: TCP, DCP, ECH
VARS: GLY, ATOX

CONSTS: k_cat_TCP_DCP,1.05; k_K_DCP,0.751; k_cat_ECH,14.37; Km_TCP,1.79; Km_ECH,0.09; IC_TCP_REC,0.7407; IC_DCP_REC,0.0556; IC_ECH_REC,0.7092; HheC,0.01; EchA,0.01

PARAMS: DhaA,0,0.01
#; HheC,0,0.01; EchA,0,0.01

EQ: TCP = -k_cat_TCP_DCP*DhaA*hillp(TCP,Km_TCP,1,0,1)
EQ: DCP = (k_cat_TCP_DCP*DhaA*hillp(TCP,Km_TCP,1,0,1) - k_K_DCP*HheC*hillp(DCP,1,1,0,1))
EQ: ECH = k_K_DCP*HheC*hillp(DCP,1,1,0,1) - k_cat_ECH*EchA*hillp(ECH,Km_ECH,1,0,1)
EQ: GLY = k_cat_ECH*EchA*hillp(ECH,Km_ECH,1,0,1)
EQ: ATOX = TCP*IC_TCP_REC + DCP*IC_DCP_REC + ECH*IC_ECH_REC

VAR_POINTS: TCP:500,5; DCP:500,5; ECH:500,5

THRES: TCP:   0,	0.5,	1,		1.5,	2
THRES: DCP:	  0,	0.5,	1,		1.5,	2
THRES: ECH:   0,	2
THRES: GLY:   0,	0.25,	0.5,	0.75,	1,	1.5,	2
THRES: ATOX:  0,	0.5,	1,		1.5,	2,	3,		5

# property: AF AG (GLY > 1.5 && ATOX < 3)
# additional constrains: (DhaA+HheC+EchA)<0.02
    """

    val parser = Parser()

    /*
        Basic errors in models
     */

    @Test
    fun noVariablesTest() {
        assertFails {
            parser.parse("""
            CONSTS: a,10; b,0.45
            PARAMS: Foo,0,1
            """)
        }
    }

    @Test
    fun missingThresholds() {
        assertFails {
            parser.parse("""
            VARS: V1
            EQ: V1 = V1
            """)
        }
    }

    @Test
    fun notEnoughThresholds() {
        assertFails {
            parser.parse("""
            VARS: V1
            EQ: V1 = V1
            THRES: V1: 1
            """)
        }
    }

    @Test
    fun missingEquation() {
        assertFails {
            parser.parse("""
            VARS: V1
            THRES: V1: 1,2,3,4
            """)
        }
    }

    @Test
    fun extraEquation() {
        assertFails {
            parser.parse("""
            VARS: V1
            THRES: V1: 1,2
            EQ: V1 = V1
            EQ: V2 = V1
            """)
        }
    }

    @Test
    fun extraThresholds() {
        assertFails {
            parser.parse("""
            VARS: V1
            THRES: V1: 1,2
            EQ: V1 = V1
            THRES: V2: 2,3
            """)
        }
    }

    @Test
    fun extraVarPoints() {
        assertFails {
            parser.parse("""
            VARS: V1
            THRES: V1: 1,2
            EQ: V1 = V1
            VAR_POINTS: V2:10,2
            """)
        }
    }

    @Test
    fun extraInitial() {
        assertFails {
            parser.parse("""
            VARS: V1
            THRES: V1: 1,2
            EQ: V1 = V1
            INIT: V2:10,20
            """)
        }
    }

    @Test
    fun nameClash() {
        assertFails {   //const vs. variable
            parser.parse("""
            VARS: V1
            CONSTS: V1,10
            THRES: V1: 1,2
            EQ: V1 = V1
            """)
        }
        assertFails {   //const vs. param
            parser.parse("""
            VARS: V1
            CONSTS: V2,10
            PARAMS: V2,2,3
            THRES: V1: 1,2
            EQ: V1 = V1
            """)
        }
        assertFails { //param vs. variable
            parser.parse("""
            VARS: V1
            PARAMS: V1,10,15
            THRES: V1: 1,2
            EQ: V1 = V1
            """)
        }
    }

    @Test
    fun undefinedReference() {
        assertFails {
            parser.parse("""
            VARS: V1
            THRES: V1: 1,2
            EQ: V1 = AB
            """)
        }
        assertFails {
            parser.parse("""
            VARS: V1
            THRES: V1: 1,2
            EQ: V1 = hillp(V2, 0, 0, 0, 0)
            """)
        }
        assertFails {
            parser.parse("""
            VARS: V1
            THRES: V1: 1,2
            EQ: V1 = hillp(V1, 0, 0, V2, 0)
            """)
        }
    }

    @Test
    fun variableRedefinition() {
        assertFails {
            parser.parse("""
            VARS: V1
            VARS: V1
            THRES: V1: 1,2
            EQ: V1 = V1
            """)
        }
        assertFails {
            parser.parse("""
            VARS: V1, V2, V1
            THRES: V1: 1,2
            THRES: V2: 1,2
            EQ: V1 = V1
            EQ: V2 = V2
            """)
        }
    }

    @Test
    fun constantRedefinition() {
        assertFails {
            parser.parse("""
            VARS: V1
            CONSTS: c1,10; c2,12; c1,20
            THRES: V1: 1,2
            EQ: V1 = V1
            """)
        }
        assertFails {
            parser.parse("""
            VARS: V1
            CONSTS: c1,10
            THRES: V1: 1,2
            CONSTS: c2,12; c1,15
            EQ: V1 = V1
            """)
        }
    }

    @Test
    fun parameterRedefinition() {
        assertFails {
            parser.parse("""
            VARS: V1
            PARAMS: c1,10,20; c2,12,13; c1,20,25
            THRES: V1: 1,2
            EQ: V1 = V1
            """)
        }
        assertFails {
            parser.parse("""
            VARS: V1
            PARAMS: c1,10,15
            THRES: V1: 1,2
            PARAMS: c2,12,13; c1,15,16
            EQ: V1 = V1
            """)
        }
    }

    @Test
    fun varPointsRedefinition() {
        assertFails {
            parser.parse("""
            VARS: V1
            VAR_POINTS: V1:10,20; V1:20,25
            THRES: V1: 1,2
            EQ: V1 = V1
            """)
        }
        assertFails {
            parser.parse("""
            VARS: V1
            VAR_POINTS: V1:10,15
            THRES: V1: 1,2
            VAR_POINTS: V1:15,16
            EQ: V1 = V1
            """)
        }
    }

    @Test
    fun initialIntervalRedefinition() {
        assertFails {
            parser.parse("""
            VARS: V1
            INIT: V1:10,20; V1:20,25
            THRES: V1: 1,2
            EQ: V1 = V1
            """)
        }
        assertFails {
            parser.parse("""
            VARS: V1
            INIT: V1:10,15
            THRES: V1: 1,2
            INIT: V1:15,16
            EQ: V1 = V1
            """)
        }
    }

    @Test
    fun thresholdsRedefinition() {
        assertFails {
            parser.parse("""
            VARS: V1
            THRES: V1: 1,2
            EQ: V1 = V1
            THRES: V1: 2,3
            """)
        }
    }

    @Test
    fun equationRedefinition() {
        assertFails {
            parser.parse("""
            VARS: V1
            EQ: V1 = 2.0 + 3.0
            THRES: V1: 1,2
            EQ: V1 = V1
            """)
        }
    }

    /*
        Equation parsing
     */

    @Test
    fun literalPropagation() {
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(
                                constant = 12.0,
                                variableIndices = listOf(0)
                        ),
                        Summand(constant = 24.0)
                )
                )), listOf()
        ),
            parser.parse("""
            VARS: V1
            THRES: V1: 0,2,3
            EQ: V1 = 4 * (-2*-2 + 3*V1) + 8
            """)
        )
    }

    @Test
    fun constantPropagation() {
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(
                                constant = 12.0,
                                variableIndices = listOf(0)
                        ),
                        Summand(constant = 24.0)
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            CONSTS: c1,8; c2,-2
            THRES: V1: 0,2,3
            EQ: V1 = 4 * (c2 * -2 + 3*V1) + c1
            """)
        )
    }

    @Test
    fun hillFunctionsTest() {
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Hill(0, 1.0, 2.0, 3.0, 4.0, true)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            THRES: V1: 0,2,3
            EQ: V1 = hillp(V1, 1.0, 2.0, 3.0, 4.0)
            """)
        )

        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Hill(0, 1.3, 2.0, 3.0, 4.0, false)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            THRES: V1: 0,2,3
            EQ: V1 = Hillm(V1, 1.3, 2.0, 4.0, 3.0)
            """)
        )

        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Hill(0, 1.0, 3.12, 3.0, 4.0, true)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            CONSTS: c1,3.12
            THRES: V1: 0,2,3
            EQ: V1 = Hillp(V1, 1.0, c1, 3.0, 4.0)
            """)
        )
    }

    @Test
    fun rampFunctionsTest() {
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Ramp.positive(0, 1.0, 2.0, 3.0, 4.0)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            THRES: V1: 0,2,3
            EQ: V1 = Rp(V1, 1.0, 2.0, 3.0, 4.0)
            """)
        )
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Ramp.negative(0, 1.3, 2.0, 3.0, 4.0)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            THRES: V1: 0,2,3
            CONSTS: c1,1.3
            EQ: V1 = rm(V1, c1, 2.0, 3.0, 4.0)
            """)
        )
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Ramp.positiveCoordinate(0, 1.0, 2.2, 3.0, 4.0)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            THRES: V1: 0,2,3
            CONSTS: c1,2.2
            EQ: V1 = rpcoor(V1, 1.0, c1, 3.0, 4.0)
            """)
        )
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Ramp.negativeCoordinate(0, 1.0, 2.0, 3.0, 4.0)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            THRES: V1: 0,2,3
            EQ: V1 = Rmcoor(V1, 1.0, 2.0, 3.0, 4.0)
            """)
        )
    }

    @Test
    fun stepFunctionTest() {
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Step(0, 1.0, 2.0, 3.0, true)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            THRES: V1: 0,2,3
            EQ: V1 = Hp(V1, 1.0, 2.0, 3.0)
            """)
        )
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Step(0, 1.0, 2.0, 3.0, false)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            THRES: V1: 0,2,3
            EQ: V1 = hm(V1, 1.0, 2.0, 3.0)
            """)
        )
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Step(0, 1.0, 2.2, 3.0, true)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            CONSTS: c1,2.2
            THRES: V1: 0,2,3
            EQ: V1 = hp(V1, 1.0, c1, 3.0)
            """)
        )
    }

    @Test
    fun sigmoidFunctionTest() {
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Sigmoid.positive(0, 1.0, 2.2, 3.0, 4.0)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            CONSTS: c1,2.2
            THRES: V1: 0,2,3
            EQ: V1 = Sp(V1, 1.0, c1, 3.0, 4.0)
            """)
        )
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Sigmoid.negative(0, 1.0, 2.0, 3.0, 4.0)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            THRES: V1: 0,2,3
            EQ: V1 = sm(V1, 1.0, 2.0, 3.0, 4.0)
            """)
        )
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Sigmoid.positiveInverse(0, 1.0, 2.0, 3.0, 4.0)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            THRES: V1: 0,2,3
            EQ: V1 = spinv(V1, 1.0, 2.0, 3.0, 4.0)
            """)
        )
        assertEquals(Model(
                listOf(Model.Variable(
                        name = "V1", range = Pair(0.0, 3.0),
                        thresholds = listOf(0.0, 2.0, 3.0),
                        varPoints = null, equation = listOf(
                        Summand(evaluable = listOf(Sigmoid.negativeInverse(0, 1.0, 2.0, 3.14, 4.0)))
                )
                )), listOf()
        ),
                parser.parse("""
            VARS: V1
            CONSTS: c1,3.14
            THRES: V1: 0,2,3
            EQ: V1 = Sminv(V1, 1.0, 2.0, c1, 4.0)
            """)
        )
    }



    /*
        Real life examples
     */

    @Test
    fun model1Test() {

        val parser = Parser()

        val model = parser.parse(model1)

        println(model)

    }
}