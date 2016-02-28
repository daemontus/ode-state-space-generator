package cz.muni.fi.ode.generator

import cz.muni.fi.checker.IDNode
import cz.muni.fi.checker.UniformPartitionFunction
import cz.muni.fi.checker.nodesOf
import cz.muni.fi.ode.model.Model
import cz.muni.fi.ode.model.Summand
import org.junit.Test
import kotlin.test.assertEquals


class OneDimWithParamGeneratorTest {

    //dv1 = p(v1/2 + 1) - 1
    //This one dimensional model should actually cover most of the behaviour
    //It only fails to cover a steady state in the middle of the model and
    //cases when parameter is multiplied by zero

    //dv2 = p(v1/2 - 2) - 1
    //This model covers the two remaining cases. A stable state and a zero on threshold.

    private val v1 = Model.Variable(
            name = "v1", range = Pair(0.0,6.0), varPoints = null,
            thresholds = listOf(0.0, 2.0, 4.0, 6.0),
            equation = listOf(
                    Summand(paramIndex = 0, variableIndices = listOf(0), constant = 0.5),
                    Summand(paramIndex = 0),
                    Summand(constant = -1.0))
    )

    private val v2 = v1.copy(name = "v2",
            equation = listOf(
                    Summand(paramIndex = 0, variableIndices = listOf(0), constant = 0.5),
                    Summand(paramIndex = 0, constant = -2.0),
                    Summand(constant = -1.0)))

    private val fragmentOne = OdeFragment(Model(listOf(v1), listOf(
            Model.Parameter("p1", Pair(0.0, 2.0))
    )), UniformPartitionFunction<IDNode>())

    private val fragmentTwo = OdeFragment(Model(listOf(v2), listOf(
            Model.Parameter("p2", Pair(-2.0, 2.0))
    )), UniformPartitionFunction<IDNode>())

    private val n0 = IDNode(0)
    private val n1 = IDNode(1)
    private val n2 = IDNode(2)

    private val c = RectangleColors()

    @Test
    fun parameterTestOne() {
        val s0 = fragmentOne.successors.invoke(n0)
        assertEquals(nodesOf(c,
                n0 to RectangleColors(Rectangle(doubleArrayOf(
                        0.0, 1.0
                ))),
                n1 to RectangleColors(Rectangle(doubleArrayOf(
                        1.0 / 2.0, 2.0
                )))), s0)
        val s1 = fragmentOne.successors.invoke(n1)
        assertEquals(nodesOf(c,
                n0 to RectangleColors(Rectangle(doubleArrayOf(
                        0.0, 1.0 / 2.0
                ))),
                n1 to RectangleColors(Rectangle(doubleArrayOf(
                        1.0 / 3.0, 1.0 / 2.0
                ))),
                n2 to RectangleColors(Rectangle(doubleArrayOf(
                        1.0 / 3.0, 2.0
                )))), s1)
        val s2 = fragmentOne.successors.invoke(n2)
        assertEquals(nodesOf(c,
                n1 to RectangleColors(Rectangle(doubleArrayOf(
                        0.0, 1.0 / 3.0
                ))),
                n2 to RectangleColors(Rectangle(doubleArrayOf(
                        1.0 / 4.0, 2.0
                )))), s2
        )
        val p0 = fragmentOne.predecessors.invoke(n0)
        assertEquals(nodesOf(c,
                n0 to RectangleColors(Rectangle(doubleArrayOf(
                        0.0, 1.0
                ))),
                n1 to RectangleColors(Rectangle(doubleArrayOf(
                        0.0, 1.0 / 2.0
                )))), p0
        )
        val p1 = fragmentOne.predecessors.invoke(n1)
        assertEquals(nodesOf(c,
                n0 to RectangleColors(Rectangle(doubleArrayOf(
                        1.0 / 2.0, 2.0
                ))),
                n1 to RectangleColors(Rectangle(doubleArrayOf(
                        1.0 / 3.0, 1.0 / 2.0
                ))),
                n2 to RectangleColors(Rectangle(doubleArrayOf(
                        0.0, 1.0 / 3.0
                )))), p1
        )
        val p2 = fragmentOne.predecessors.invoke(n2)
        assertEquals(nodesOf(c,
                n1 to RectangleColors(Rectangle(doubleArrayOf(
                        1.0 / 3.0, 2.0
                ))),
                n2 to RectangleColors(Rectangle(doubleArrayOf(
                        1.0 / 4.0, 2.0
                )))), p2
        )
    }

    @Test
    fun parameterTestTwo() {
        //dv2 = p(v1 - 2) - 1
        //(0) dv2 = p(-2) - 1 p>-1/2 => - // p < -1/2 => +
        //(1) dv2 = p(-1) - 1 p>-1 => - // p < -1 => +
        //(2) dv2 = p(0) - 1 // -1
        //(3) dv2 = p(1) - 1  p<1 => - // p > 1 => +
        val s0 = fragmentTwo.successors.invoke(n0)
        assertEquals(nodesOf(c,
                n0 to RectangleColors(Rectangle(doubleArrayOf(
                    -1.0, 2.0
                ))),
                n1 to RectangleColors(Rectangle(doubleArrayOf(
                    -2.0, -1.0
                )))), s0)
        val s1 = fragmentTwo.successors.invoke(n1)
        assertEquals(nodesOf(c,
                n0 to RectangleColors(Rectangle(doubleArrayOf(
                        -1.0, 2.0
                ))),
                n1 to RectangleColors(Rectangle(doubleArrayOf(
                        -2.0, -1.0
                )))), s1)
        val s2 = fragmentTwo.successors.invoke(n2)
        assertEquals(nodesOf(c,
                n1 to RectangleColors(Rectangle(doubleArrayOf(
                        -2.0, 2.0
                ))),
                n2 to RectangleColors(Rectangle(doubleArrayOf(
                        1.0, 2.0
                )))), s2)
        val p0 = fragmentTwo.predecessors.invoke(n0)
        assertEquals(nodesOf(c,
                n0 to RectangleColors(Rectangle(doubleArrayOf(
                        -1.0, 2.0
                ))),
                n1 to RectangleColors(Rectangle(doubleArrayOf(
                        -1.0, 2.0
                )))), p0)
        val p1 = fragmentTwo.predecessors.invoke(n1)
        assertEquals(nodesOf(c,
                n0 to RectangleColors(Rectangle(doubleArrayOf(
                        -2.0, -1.0
                ))),
                n1 to RectangleColors(Rectangle(doubleArrayOf(
                        -2.0, -1.0
                ))),
                n2 to RectangleColors(Rectangle(doubleArrayOf(
                        -2.0, 2.0
                )))), p1)
        val p2 = fragmentTwo.predecessors.invoke(n2)
        assertEquals(nodesOf(c,
                n2 to RectangleColors(Rectangle(doubleArrayOf(
                        1.0, 2.0
                )))), p2)
    }

}
