package com.cedarsoftware.ncube

import com.cedarsoftware.ncube.exception.AxisOverlapException
import com.cedarsoftware.ncube.proximity.LatLon
import com.cedarsoftware.ncube.proximity.Point3D
import com.cedarsoftware.util.io.JsonWriter
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

/**
 * NCube Axis Tests
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License')
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an 'AS IS' BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TestAxis
{
    @Before
    public void setUp() throws Exception
    {
        TestingDatabaseHelper.setupDatabase()
    }

    @After
    public void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase()
    }

    private static boolean isValidPoint(Axis axis, Comparable value)
    {
        try
        {
            axis.addColumn value
            return true
        }
        catch (AxisOverlapException e)
        {
            return false
        }
    }

    @Test
    public void testAxisNameChange()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)
        axis.name = 'bar'
        assert 'bar' == axis.name
    }

    @Test
    public void testRemoveMetaPropertyWhenMetaPropertiesAreNull()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)
        assertNull axis.removeMetaProperty('foo')
    }

    @Test
    public void testRemoveMetaProperty()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)

        def map = [foo:'bar','bar':'baz']
        axis.addMetaProperties map

        assert 'bar' == axis.getMetaProperty('foo')
        assert 'bar' == axis.removeMetaProperty('foo')
        assertNull axis.getMetaProperty('foo')
    }

    @Test
    public void testClearMetaProperties()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)

        Map map = new HashMap()
        map.put('foo', 'bar')
        map.put('bar', 'baz')
        axis.addMetaProperties(map)

        assert 'bar' == axis.getMetaProperty('foo')
        assert 'baz' == axis.getMetaProperty('bar')

        axis.clearMetaProperties()

        assertNull axis.getMetaProperty('foo')
        assertNull axis.getMetaProperty('bar')
        assertNull axis.removeMetaProperty('foo')
    }

    @Test
    public void testGetMetaPropertyWhenMetaPropertiesAreNull()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)
        assertNull axis.getMetaProperty('foo')
    }


    @Test(expected = IllegalArgumentException.class)
    public void testConvertStringToColumnValueWithEmptyString()
    {
        Axis axis = new Axis('test axis', AxisType.DISCRETE, AxisValueType.LONG, true)
        axis.convertStringToColumnValue('')
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertStringToColumnValueWithNull()
    {
        Axis axis = new Axis('test axis', AxisType.DISCRETE, AxisValueType.LONG, true)
        axis.convertStringToColumnValue(null)
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertStringToColumnValueWihInvalidRangeDefinition()
    {
        Axis axis = new Axis('test axis', AxisType.SET, AxisValueType.LONG, true)
        axis.convertStringToColumnValue('[[5]]')
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertStringToColumnValueWihRangeException()
    {
        Axis axis = new Axis('test axis', AxisType.SET, AxisValueType.LONG, true)
        axis.convertStringToColumnValue('[null]')
    }

    @Test
    public void testStandardizeColumnValueErrorHandling()
    {
        Axis states = TestNCube.getStatesAxis()
        try
        {
            states.standardizeColumnValue(null)
            fail("should not make it here")
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('null')
            assert e.message.toLowerCase().contains('cannot be used')
        }
    }

    @Test
    public void testAxisValueOverlap()
    {
        Axis axis = new Axis('test axis', AxisType.DISCRETE, AxisValueType.LONG, true)
        axis.addColumn 0
        axis.addColumn 10
        axis.addColumn 100

        assert isValidPoint(axis, -1)
        assert !isValidPoint(axis, 0)
        assert !isValidPoint(axis, 10)
        assert isValidPoint(axis, 11)
        assert !isValidPoint(axis, 100)
        assert isValidPoint(axis, 101)

        try
        {
            axis.addColumn(new Range(3, 9))
            fail 'should not make it here'
        }
        catch (IllegalArgumentException expected)
        {
            expected.message.toLowerCase().contains("unsupported value type")
        }

        axis = new Axis('test axis', AxisType.DISCRETE, AxisValueType.STRING, true)
        axis.addColumn 'echo'
        axis.addColumn 'juliet'
        axis.addColumn 'tango'

        assert isValidPoint(axis, 'alpha')
        assert !isValidPoint(axis, 'echo')
        assert !isValidPoint(axis, 'juliet')
        assert isValidPoint(axis, 'kilo')
        assert !isValidPoint(axis, 'tango')
        assert isValidPoint(axis, 'uniform')

        try
        {
            axis.addColumn new Range(3, 9)
            fail 'should not make it here'
        }
        catch (IllegalArgumentException expected)
        {
            expected.message.toLowerCase().contains("unsupported value type")
        }
    }

    @Test
    public void testRangeOverlap()
    {
        Axis axis = new Axis("Age", AxisType.RANGE, AxisValueType.LONG, true)
        axis.addColumn(new Range(0, 18))
        axis.addColumn(new Range(18, 30))
        axis.addColumn(new Range(65, 80))

        assertFalse(isValidRange(axis, new Range(17, 20)))
        assertFalse(isValidRange(axis, new Range(18, 20)))
        assertTrue(isValidRange(axis, new Range(30, 65)))
        assertFalse(isValidRange(axis, new Range(40, 50)))
        assertTrue(isValidRange(axis, new Range(80, 100)))
        assertFalse(isValidRange(axis, new Range(-150, 150)))
        assertTrue(axis.size() == 6)

        // Edge and Corner cases
        try
        {
            axis.addColumn(17)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('only add range value')
        }

        try
        {
            axis.addColumn(new Range(-10, -10))
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('low and high must be different')
        }

        // Using Long's as Dates (Longs, Date, or Calendar allowed)
        axis = new Axis("Age", AxisType.RANGE, AxisValueType.DATE, true)
        axis.addColumn(new Range(0L, 18L))
        axis.addColumn(new Range(18L, 30L))
        axis.addColumn(new Range(65L, 80L))

        assertFalse(isValidRange(axis, new Range(17L, 20L)))
        assertFalse(isValidRange(axis, new Range(18L, 20L)))
        assertTrue(isValidRange(axis, new Range(30L, 65L)))
        assertFalse(isValidRange(axis, new Range(40L, 50L)))
        assertTrue(isValidRange(axis, new Range(80L, 100L)))
        assertFalse(isValidRange(axis, new Range(-150L, 150L)))
        assertTrue(axis.size() == 6)

        // Edge and Corner cases
        try
        {
            axis.addColumn(17)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('only add range value')
        }

        try
        {
            axis.addColumn(new Range(-10L, -10L))
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('low and high must be different')
        }

        axis = new Axis("Age", AxisType.RANGE, AxisValueType.DOUBLE, true)
        axis.addColumn(new Range(0, 18))
        axis.addColumn(new Range(18, 30))
        axis.addColumn(new Range(65, 80))

        assertFalse(isValidRange(axis, new Range(17, 20)))
        assertFalse(isValidRange(axis, new Range(18, 20)))
        assertTrue(isValidRange(axis, new Range(30, 65)))
        assertFalse(isValidRange(axis, new Range(40, 50)))
        assertTrue(isValidRange(axis, new Range(80, 100)))
        assertFalse(isValidRange(axis, new Range(-150, 150)))
        assertTrue(axis.size() == 6)

        // Edge and Corner cases
        try
        {
            axis.addColumn(17)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('only add range value')
        }

        try
        {
            axis.addColumn(new Range(-10, -10))
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('low and high must be different')
        }

        axis = new Axis("Age", AxisType.RANGE, AxisValueType.BIG_DECIMAL, true)
        axis.addColumn(new Range(0, 18))
        axis.addColumn(new Range(18, 30))
        axis.addColumn(new Range(65, 80))

        assertFalse(isValidRange(axis, new Range(17, 20)))
        assertFalse(isValidRange(axis, new Range(18, 20)))
        assertTrue(isValidRange(axis, new Range(30, 65)))
        assertFalse(isValidRange(axis, new Range(40, 50)))
        assertTrue(isValidRange(axis, new Range(80, 100)))
        assertFalse(isValidRange(axis, new Range(-150, 150)))
        assertTrue(axis.size() == 6)

        // Edge and Corner cases
        try
        {
            axis.addColumn(17)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('only add range value')
        }

        try
        {
            axis.addColumn(new Range(-10, -10))
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('low and high must be different')
        }
    }

    @Test
    public void testAxisInsertAtFront()
    {
        Axis states = new Axis('States', AxisType.SET, AxisValueType.STRING, false, Axis.SORTED)
        RangeSet set = new RangeSet('GA')
        set.add 'OH'
        set.add 'TX'
        states.addColumn set
        set = new RangeSet('AL')
        set.add 'WY'
        states.addColumn set
        assert states.size() == 2
        Column col = states.columns[0]
        assert col.value == set      // added first (because of SORTED)
    }

    @Test
    public void testAxisLongType()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false, Axis.DISPLAY)
        axis.addColumn 1
        axis.addColumn 2L
        axis.addColumn 3 as Byte
        axis.addColumn 4 as Short
        axis.addColumn '5'
        axis.addColumn new BigDecimal('6')
        axis.addColumn new BigInteger('7')
        assert AxisType.DISCRETE.equals(axis.type)
        assert AxisValueType.LONG.equals(axis.valueType)
        assert axis.size() == 7

        assert axis.columns[0].value instanceof Long
        assert axis.columns[1].value instanceof Long
        assert axis.columns[2].value instanceof Long
        assert axis.columns[3].value instanceof Long
        assert axis.columns[4].value instanceof Long
        assert axis.columns[5].value instanceof Long
        assert axis.columns[6].value instanceof Long

        assert axis.columns[0].value == 1
        assert axis.columns[1].value == 2
        assert axis.columns[2].value == 3
        assert axis.columns[3].value == 4
        assert axis.columns[4].value == 5
        assert axis.columns[5].value == 6
        assert axis.columns[6].value == 7
    }

    @Test
    public void testAddingNullToAxis()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)
        axis.addColumn null    // Add default column
        assert axis.hasDefaultColumn()
        try
        {
            axis.addColumn null
            fail 'should throw exception'
        }
        catch (IllegalArgumentException expected)
        {
            assert expected.message.contains('not')
            assert expected.message.contains('add')
            assert expected.message.contains('default')
            assert expected.message.contains('already')
        }
        axis.deleteColumn null
        assert !axis.hasDefaultColumn()
    }

    @Test
    public void testAxisGetValues()
    {
        NCube ncube = new NCube('foo')
        ncube.addAxis TestNCube.longDaysOfWeekAxis
        ncube.addAxis TestNCube.longMonthsOfYear
        ncube.addAxis TestNCube.getOddAxis(true)
        Axis axis = (Axis) ncube.axes.get(0)
        List values = axis.columns
        assert values.size() == 7
        assert TestNCube.countMatches(ncube.toHtml(), '<tr') == 44
    }

    @Test
    public void testAxisCaseInsensitivity()
    {
        NCube<String> ncube = new NCube<String>('TestAxisCase')
        Axis gender = TestNCube.getGenderAxis true
        ncube.addAxis gender
        Axis gender2 = new Axis('gender', AxisType.DISCRETE, AxisValueType.STRING, true)

        try
        {
            ncube.addAxis gender2
            fail 'should throw exception'
        }
        catch (IllegalArgumentException expected)
        {
            assert expected.message.contains('axis')
            assert expected.message.contains('already')
            assert expected.message.contains('exists')
        }

        def coord = [gendeR:null]
        ncube.setCell '1', coord
        assert '1'.equals(ncube.getCell(coord))

        coord.GendeR = 'Male'
        ncube.setCell '2', coord
        assert '2'.equals(ncube.getCell(coord))

        coord.GENdeR = 'Female'
        ncube.setCell '3', coord
        assert '3'.equals(ncube.getCell(coord))

        Axis axis = ncube.getAxis 'genDER'
        assert axis.name == 'Gender'
        ncube.deleteAxis 'GeNdEr'
        assert ncube.numDimensions == 0
    }

    @Test
    public void testRangeSetAxisErrors()
    {
        Axis age = new Axis('Age', AxisType.SET, AxisValueType.LONG, true)
        RangeSet set = new RangeSet(1)
        set.add 3.0
        set.add new Range(10, 20)
        set.add 25
        age.addColumn set

        set = new RangeSet(2)
        set.add 20L
        set.add 35 as Byte
        age.addColumn set

        try
        {
            set = new RangeSet(12)
            age.addColumn(set)
            fail('should throw exception')
        }
        catch (AxisOverlapException expected)
        {
            assert expected.message.contains('RangeSet')
            assert expected.message.contains('overlap')
            assert expected.message.contains('exist')
        }

        try
        {
            set = new RangeSet(15)
            age.addColumn set
            fail 'should throw exception'
        }
        catch (AxisOverlapException expected)
        {
            assert expected.message.contains('RangeSet')
            assert expected.message.contains('overlap')
            assert expected.message.contains('exist')
        }

        try
        {
            set = new RangeSet(new Character('c' as char)) // not a valid type for a LONG axis
            age.addColumn set
            fail 'should throw exception'
        }
        catch (Exception expected)
        {
            assert expected instanceof IllegalArgumentException
            assert expected.message.toLowerCase().contains('error promoting value')
        }

        try
        {
            Range range = new Range(0, 10)
            age.addColumn range
            fail 'should throw exception'
        }
        catch (IllegalArgumentException expected)
        {
            assert expected.message.contains('only')
            assert expected.message.contains('add')
            assert expected.message.contains('RangeSet')
        }

        RangeSet a = new RangeSet()
        RangeSet b = new RangeSet()
        assert a.compareTo(b) == 0
    }

    @Test
    public void testDeleteColumnFromRangeSetAxis() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'testCube4.json'
        ncube.deleteColumn 'code', 'b'
        Axis axis = ncube.getAxis 'code'
        assert axis.id != 0
        assert axis.columns.size() == 2
        axis.deleteColumn('o')
        assert axis.columns.size() == 1
        assert axis.idToCol.size() == 1
        assertNull axis.deleteColumnById(9)
    }

    @Test
    public void testDupeIdsOnAxis() throws Exception
    {
        try
        {
            NCubeManager.getNCubeFromResource('idBasedCubeError2.json')
            fail('should not make it here')
        }
        catch (RuntimeException e)
        {
            assert e.message.toLowerCase().contains('failed to load')
        }
    }

    @Test
    public void testAddDefaultToNearestAxis()
    {
        Axis nearest = new Axis('points', AxisType.NEAREST, AxisValueType.COMPARABLE, false)
        try
        {
            nearest.addColumn null
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains("cannot add default column")
            assert e.message.toLowerCase().contains("to nearest axis")
        }
    }

    @Test
    public void testMetaProperties()
    {
        Axis c = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, true)
        assertNull c.metaProperties.get('foo')

        c.clearMetaProperties()
        c.setMetaProperty('foo', 'bar')
        assert 'bar' == c.metaProperties.get('foo')

        c.clearMetaProperties()
        assertNull c.metaProperties.get('foo')

        c.clearMetaProperties()
        c.addMetaProperties([BaZ:'qux'])
        assert 'qux' == c.metaProperties.get('baz')
    }

    @Test
    public void testGetString()
    {
        assert '1' == Axis.getString(1)
        assert 'true' == Axis.getString(true)
        assert 'foo'.is(Axis.getString('foo'))
    }

    @Test
    public void testToString()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)
        assert 'Axis: foo [DISCRETE, LONG, no-default-column, sorted]' == axis.toString()

        Axis c = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, true)
        assertNull c.metaProperties.get('foo')
        c.setMetaProperty 'foo', 'bar'

        assert 'Axis: foo [DISCRETE, STRING, default-column, sorted]\n' +
                '  metaProps: {foo=bar}' == c.toString()
    }

    @Test
    public void testConvertDiscreteColumnValue() throws Exception
    {
        // Strings
        Axis states = TestNCube.statesAxis
        assert states.convertStringToColumnValue('OH') == 'OH'

        // Longs
        Axis longs = new Axis('longs', AxisType.DISCRETE, AxisValueType.LONG, false)
        assert -1L == longs.convertStringToColumnValue('-1')
        assert 0L == longs.convertStringToColumnValue('0')
        assert 1L == longs.convertStringToColumnValue('1')
        assert 12345678901234L == longs.convertStringToColumnValue('12345678901234')
        assert -12345678901234L == longs.convertStringToColumnValue('-12345678901234')
        try
        {
            longs.convertStringToColumnValue '-12345.678901234'
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('error promoting value')
        }

        // BigDecimals
        Axis bigDec = new Axis('bigDec', AxisType.DISCRETE, AxisValueType.BIG_DECIMAL, false)
        assert -1g == bigDec.convertStringToColumnValue('-1')
        assert 0g == bigDec.convertStringToColumnValue('0')
        assert 1g == bigDec.convertStringToColumnValue('1')
        assert 12345678901234g == bigDec.convertStringToColumnValue('12345678901234')
        assert -12345678901234g ==  bigDec.convertStringToColumnValue('-12345678901234')
        assert -12345.678901234g == bigDec.convertStringToColumnValue('-12345.678901234')

        // Doubles
        Axis doubles = new Axis('bigDec', AxisType.DISCRETE, AxisValueType.DOUBLE, false)
        assertEquals(-1.0, doubles.convertStringToColumnValue('-1'), 0.000001d)
        assertEquals(0.0, doubles.convertStringToColumnValue('0'), 0.000001d)
        assertEquals(1.0, doubles.convertStringToColumnValue('1'), 0.00001d)
        assertEquals(12345678901234.0, doubles.convertStringToColumnValue('12345678901234'), 0.00001d)
        assertEquals(-12345678901234.0, doubles.convertStringToColumnValue('-12345678901234'), 0.00001d)
        assertEquals(-12345.678901234d, doubles.convertStringToColumnValue('-12345.678901234'), 0.00001d)

        // Dates
        Axis dates = new Axis('Dates', AxisType.DISCRETE, AxisValueType.DATE, false)
        Calendar cal = Calendar.instance
        cal.clear()
        cal.set(2014, 0, 18, 0, 0, 0)
        assert dates.convertStringToColumnValue('1/18/2014') == cal.time
        cal.clear()
        cal.set(2014, 6, 9, 13, 10, 58)
        assert dates.convertStringToColumnValue('2014 Jul 9 13:10:58') == cal.time
        try
        {
            dates.convertStringToColumnValue('2014 Ju1y 9 13:10:58')
            fail('should not make it here')
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('error promoting value')
        }

        // Expression
        Axis exp = new Axis('Condition', AxisType.RULE, AxisValueType.EXPRESSION, false, Axis.DISPLAY)
        assert new GroovyExpression('println \'Hello\'', null) == exp.convertStringToColumnValue('println \'Hello\'')

        // Comparable (this allows user to create Java Comparable object instances as Column values!
        Axis comp = new Axis('Comparable', AxisType.DISCRETE, AxisValueType.COMPARABLE, false)
        cal.clear()
        cal.set 2014, 0, 18, 16, 26, 0
        String json = JsonWriter.objectToJson cal
        assert cal == comp.convertStringToColumnValue(json)
    }

    @Test
    public void testRangeParsing()
    {
        Axis axis = new Axis('ages', AxisType.RANGE, AxisValueType.LONG, true, Axis.SORTED)
        Range range = (Range) axis.convertStringToColumnValue('10,20')
        assert 10L == range.low
        assert 20L == range.high

        range = (Range) axis.convertStringToColumnValue('  10 ,\t20  \n')
        assert 10L == range.low
        assert 20L == range.high

        axis = new Axis('ages', AxisType.RANGE, AxisValueType.DATE, false)
        range = (Range) axis.convertStringToColumnValue('12/25/2014, 12/25/2016')
        Calendar calendar = Calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == range.low
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == range.high

        range = (Range) axis.convertStringToColumnValue('Dec 25 2014, 12/25/2016')
        calendar = calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == range.low
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == range.high

        range = (Range) axis.convertStringToColumnValue('Dec 25 2014, Dec 25 2016')
        calendar = calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == range.low
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == range.high

        range = (Range) axis.convertStringToColumnValue('12/25/2014, Dec 25 2016')
        calendar = calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == range.low
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == range.high
    }

    @Test
    public void testDiscreteSetParsing()
    {
        Axis axis = new Axis('ages', AxisType.SET, AxisValueType.LONG, true, Axis.SORTED)
        RangeSet set = (RangeSet) axis.convertStringToColumnValue('10,20')
        assert 10L == set.get(0)
        assert 20L == set.get(1)

        set = (RangeSet) axis.convertStringToColumnValue('  10 ,\t20  \n')
        assert 10L == set.get(0)
        assert 20L == set.get(1)

        // Support no outer brackets
        axis.convertStringToColumnValue('10, 20')
        assert 10L == set.get(0)
        assert 20L == set.get(1)

        axis = new Axis('ages', AxisType.SET, AxisValueType.DATE, false)
        set = (RangeSet) axis.convertStringToColumnValue(' "12/25/2014", "12/25/2016"')
        Calendar calendar = Calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == set.get(0)
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == set.get(1)

        set = (RangeSet) axis.convertStringToColumnValue(' "Dec 25, 2014", "Dec 25, 2016"')
        calendar = calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == set.get(0)
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == set.get(1)
    }

    @Test
    public void testRangeSetParsing()
    {
        Axis axis = new Axis('ages', AxisType.SET, AxisValueType.LONG, true, Axis.SORTED)
        RangeSet set = (RangeSet) axis.convertStringToColumnValue('[10,20]')
        Range range = (Range) set.get(0)
        assert 10L == range.low
        assert 20L == range.high

        set = (RangeSet) axis.convertStringToColumnValue(' [  10 ,\t20  \n] ')
        range = (Range) set.get(0)
        assert 10L == range.low
        assert 20L == range.high

        axis = new Axis('ages', AxisType.SET, AxisValueType.DATE, false)
        set = (RangeSet) axis.convertStringToColumnValue('[ "12/25/2014", "12/25/2016"]')
        range = (Range) set.get(0)
        Calendar calendar = Calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == range.low
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == range.high
    }

    @Test
    public void testRangeAndDiscreteSetParsing()
    {
        Axis axis = new Axis('ages', AxisType.SET, AxisValueType.LONG, true, Axis.SORTED)
        RangeSet set = (RangeSet) axis.convertStringToColumnValue('[10,20], 1979')
        Range range = (Range) set.get(0)
        assert 10L == range.low
        assert 20L == range.high
        assert 1979L == set.get(1)

        set = (RangeSet) axis.convertStringToColumnValue(' [  10 ,\t20  \n] , 1979 ')
        range = (Range) set.get(0)
        assert 10L == range.low
        assert 20L == range.high
        assert 1979L == set.get(1)

        axis = new Axis('ages', AxisType.SET, AxisValueType.DATE, false)
        set = (RangeSet) axis.convertStringToColumnValue('[ "12/25/2014", "12/25/2016"], "12/25/2020"')
        range = (Range) set.get(0)
        Calendar calendar = Calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == range.low
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == range.high
        calendar.clear()
        calendar.set 2020, 11, 25
        assert calendar.time == set.get(1)
    }

    @Test
    public void testRangeAndDiscreteSetParsing2()
    {
        Axis axis = new Axis('ages', AxisType.SET, AxisValueType.BIG_DECIMAL, true, Axis.SORTED)
        RangeSet set = (RangeSet) axis.convertStringToColumnValue('[10,20], 1979')
        Range range = (Range) set.get(0)
        assert 10g == range.low
        assert 20g == range.high
        assert 1979g == set.get(1)

        set = (RangeSet) axis.convertStringToColumnValue(' [  10.0 ,\t20  \n] , 1979 ')
        range = (Range) set.get(0)
        assert 10g == range.low
        assert 20g == range.high
        assert 1979g == set.get(1)
    }

    @Test
    public void testNearestWithDoubles()
    {
        Axis axis = new Axis('loc', AxisType.NEAREST, AxisValueType.COMPARABLE, false)
        LatLon latlon = (LatLon) axis.convertStringToColumnValue('1.0, 2.0')
        assertEquals 1.0, latlon.getLat(), 0.00001d
        assertEquals 2.0, latlon.getLon(), 0.00001d

        latlon = (LatLon) axis.convertStringToColumnValue('1,2')
        assertEquals 1.0, latlon.getLat(), 0.00001d
        assertEquals 2.0, latlon.getLon(), 0.00001d

        latlon = (LatLon) axis.convertStringToColumnValue('-1,-2')
        assertEquals(-1.0, latlon.getLat(), 0.00001d)
        assertEquals(-2.0, latlon.getLon(), 0.001d)

        axis = new Axis('loc', AxisType.NEAREST, AxisValueType.COMPARABLE, false)
        Point3D pt3d = (Point3D) axis.convertStringToColumnValue('1.0, 2.0, 3.0')
        assertEquals 1.0, pt3d.getX(), 0.00001d
        assertEquals 2.0, pt3d.getY(), 0.00001d
        assertEquals 3.0, pt3d.getZ(), 0.00001d
    }

    @Test
    public void testAddAxisSameWayAsUI()
    {
        Axis axis = new Axis('loc', AxisType.SET, AxisValueType.LONG, true)
        Axis axis2 = new Axis('loc', AxisType.SET, AxisValueType.LONG, false)
        Column colAdded = axis2.addColumn('[1, 2]')
        colAdded.id = -1
        axis.updateColumns axis2

        assert 2 == axis.columns.size()
        Column col = axis.columnsWithoutDefault.get(0)
        RangeSet rs = new RangeSet(new Range(1L, 2L))
        assert rs == col.value
    }

    @Test
    public void testUpdateColumnWithMetaPropertyName()
    {
        Axis axis1 = new Axis('loc', AxisType.SET, AxisValueType.LONG, true)
        axis1.addColumn('[1, 2]')
        Axis axis2 = new Axis('loc', AxisType.SET, AxisValueType.LONG, true)
        axis2.addColumn('[1, 2]')
        List<Column> cols = axis2.columnsWithoutDefault
        cols.get(0).id = axis1.columnsWithoutDefault.get(0).id
        cols.get(0).setMetaProperty('name', 'cheese')
        cols.get(0).setMetaProperty('foo', 'bar')
        axis1.updateColumns(axis2)

        assert 2 == axis1.columns.size()
        Column col = axis1.columnsWithoutDefault.get(0)
        assert 'cheese' == col.getMetaProperty('name')
        assert 'bar' == col.getMetaProperty('foo')
    }

    @Test
    public void testRemoveSetColumnWithMultipleRanges()
    {
        Axis axis = new Axis('loc', AxisType.SET, AxisValueType.LONG, false)
        RangeSet rs = new RangeSet()
        rs.add(new Range(10, 20))
        rs.add(new Range(30, 40))
        axis.addColumn(rs)
        rs = new RangeSet()
        rs.add(new Range(50, 60))
        axis.addColumn(rs)
        assert 2 == axis.columns.size()
        assert 3 == axis.rangeToCol.size()
        axis.deleteColumn(15)
        assert 1 == axis.rangeToCol.size()
        assert 1 == axis.columns.size()
    }

    @Test
    public void testRemoveSetColumnWithMultipleDiscretes()
    {
        Axis axis = new Axis('loc', AxisType.SET, AxisValueType.LONG, false)
        RangeSet rs = new RangeSet()
        rs.add 20
        rs.add 30
        axis.addColumn rs
        rs = new RangeSet()
        rs.add 50
        axis.addColumn rs
        assert 2 == axis.columns.size()
        assert 3 == axis.discreteToCol.size()
        axis.deleteColumn 30
        assert 1 == axis.discreteToCol.size()
        assert 1 == axis.columns.size()
    }

    @Test
    public void testAddAxisBadColumnIds()
    {
        Axis axis = new Axis('loc', AxisType.SET, AxisValueType.LONG, true)
        Axis axis2 = new Axis('loc', AxisType.SET, AxisValueType.LONG, true)
        axis2.addColumn '[1, 2]'
        try
        {
            axis.updateColumns(axis2)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.contains('added')
            assert e.message.contains('negative')
            assert e.message.contains('values')
        }
    }

    @Test
    public void testParseBadRange()
    {
        Axis axis = new Axis('foo', AxisType.RANGE, AxisValueType.LONG, false)
        try
        {
            axis.convertStringToColumnValue('this is not a range')
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.contains('not')
            assert e.message.contains('range')
        }
    }

    @Test
    public void testParseBadSet()
    {
        Axis axis = new Axis('foo', AxisType.SET, AxisValueType.LONG, false)
        try
        {
            axis.convertStringToColumnValue('[null, false]')
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains("cannot be parsed as a set")
        }

        try
        {
            axis.convertStringToColumnValue('null, false')
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains("cannot be parsed as a set")
        }
    }

    @Test
    public void testAxisGetStringWithBigDecimal()
    {
        String pi = Axis.getString(3.1415926535897932384626433g)
        assert '3.1415926535897932384626433' == pi
    }

    @Test
    public void testFindNonExistentRuleName()
    {
        Axis axis = new Axis('foo', AxisType.RULE, AxisValueType.EXPRESSION, false, Axis.DISPLAY)
        try
        {
            axis.getRuleColumnsStartingAt 'foo'
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.contains('rule')
            assert e.message.toLowerCase().contains('not')
            assert e.message.contains('found')
        }
    }

    @Test
    public void testFindRuleNameUsingNonString()
    {
        Axis axis = new Axis('foo', AxisType.RULE, AxisValueType.EXPRESSION, false, Axis.DISPLAY)
        try
        {
            axis.findColumn 25
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.contains('rule')
            assert e.message.toLowerCase().contains('only')
            assert e.message.contains('located')
            assert e.message.contains('name')
        }
    }

    @Test
    public void testAxisProps()
    {
        Axis axis1 = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY)
        Axis axis2 = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY)

        assert axis1.areAxisPropsEqual(axis1)
        assert axis1.areAxisPropsEqual(axis2)
        assert !axis1.areAxisPropsEqual('fudge')

        Axis axis3 = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.SORTED)
        assert !axis1.areAxisPropsEqual(axis3)

        Axis axis4 = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY)
        assert !axis1.areAxisPropsEqual(axis4)

        Axis axis5 = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY)
        assert axis1.areAxisPropsEqual(axis5)
        axis5.setMetaProperty 'foo', 'bar'
        assert axis1.areAxisPropsEqual(axis5) // Ensuring meta-props are not part of arePropsEquals()

        Axis axis6 = new Axis('foot', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY)
        assert !axis1.areAxisPropsEqual(axis6)

        Axis axis7 = new Axis('foo', AxisType.RANGE, AxisValueType.STRING, false, Axis.DISPLAY)
        assert !axis1.areAxisPropsEqual(axis7)

        Axis axis8 = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false, Axis.DISPLAY)
        assert !axis1.areAxisPropsEqual(axis8)
    }

    @Test
    public void testUpdateColumn()
    {
        Axis dow = TestNCube.getShortDaysOfWeekAxis()
        Column wed = dow.findColumn("Wed")
        dow.updateColumn(wed.id, "aWed")
        wed = dow.columns.get(2)
        assertEquals(wed.value, "aWed")

        Column mon = dow.findColumn("Mon")
        dow.updateColumn(mon.id, "aMon")
        mon = dow.columns.get(0)
        assertEquals(mon.value, "aMon")

        Column sun = dow.findColumn("Sun")
        dow.updateColumn(sun.id, "aSun")
        sun = dow.columns.get(6)
        assertEquals(sun.value, "aSun")

        List<Column> cols = dow.getColumnsWithoutDefault()
        assertEquals(cols.get(4).value, "aMon")
        assertEquals(cols.get(5).value, "aSun")
        assertEquals(cols.get(6).value, "aWed")

        assertEquals(-1, cols.get(4).compareTo(new Column(null, dow.getNextColId())))
    }

    @Test
    public void testUpdateColumnsFrontMiddleBack()
    {
        Axis axis = new Axis('Age', AxisType.RANGE, AxisValueType.LONG, false, Axis.SORTED, 1)
        axis.addColumn(new Range(5, 10))
        axis.addColumn(new Range(20, 30))
        axis.addColumn(new Range(30, 40))

        Axis axis2 = new Axis('Age', AxisType.RANGE, AxisValueType.LONG, false, Axis.SORTED, 1)
        axis2.addColumn(new Range(5, 10))
        axis2.addColumn(new Range(20, 30))
        axis2.addColumn(new Range(30, 40))

        Column newCol = axis.createColumnFromValue new Range(10, 20)
        newCol.id = -newCol.id
        axis.columnsInternal.add newCol

        axis2.updateColumns(axis)
        assert 4 == axis2.columns.size()

        newCol = axis.createColumnFromValue new Range(0, 5)
        newCol.id = -newCol.id
        axis.columnsInternal.add newCol

        axis2.updateColumns(axis)
        assert 5 == axis2.columns.size()

        newCol = axis.createColumnFromValue new Range(40, 50)
        newCol.id = -newCol.id
        axis.columnsInternal.add newCol

        axis2.updateColumns axis
        assert 6 == axis2.columns.size()

        for (Column column : axis2.columns)
        {
            assert column.id >= 0
        }

        // Test remove via updateColumns()
        axis = new Axis('Age', AxisType.RANGE, AxisValueType.LONG, false, Axis.SORTED, 1)
        axis.addColumn new Range(5, 10)
        axis.addColumn new Range(20, 30)
        axis.addColumn new Range(30, 40)

        axis2.updateColumns axis
        assert 3 == axis2.size()
    }

    @Test
    public void testUpdateColumnsOverlapCheck()
    {
        Axis axis = new Axis('Age', AxisType.RANGE, AxisValueType.STRING, false, Axis.DISPLAY, 1)
        axis.addColumn new Range('2', '4')
        axis.addColumn new Range('4', '6')
        axis.addColumn new Range('6', '8')
        axis.addColumn new Range('0', '2')

        Axis axis2 = new Axis('Age', AxisType.RANGE, AxisValueType.STRING, false, Axis.DISPLAY, 1)
        axis2.addColumn new Range('2', '4')
        axis2.addColumn new Range('4', '6')
        axis2.addColumn new Range('6', '8')
        axis2.addColumn new Range('0', '2')

        Column newCol = axis.createColumnFromValue new Range('8', '10')
        newCol.id = -newCol.id
        axis.columnsInternal.add newCol

        try
        {
            axis2.updateColumns axis
            fail()
        }
        catch (AxisOverlapException e)
        {
            assert e.message.toLowerCase().contains('overlap')
            assert e.message.toLowerCase().contains('exist')
            assert e.message.toLowerCase().contains('axis')
        }
    }

    @Test
    public void testUpColumnsMaintainsOrder()
    {
        Axis axis = new Axis('days', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 1)
        axis.addColumn 'Mon'
        axis.addColumn 'Tue'
        Column wed = axis.addColumn 'Wed'
        wed.id = -wed.id
        axis.addColumn 'Thu'
        axis.addColumn 'Fri'
        axis.addColumn 'Sat'
        axis.addColumn 'Sun'

        // Mon/Sat backwards
        // Wed missing
        // Bogus column added (named 'Whoops')
        // Fix these problems with updateColumns (simulate user moving columns in NCE)
        Axis axis2 = new Axis('days', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 1)
        axis2.addColumn 'Sat'
        axis2.addColumn 'Tue'
        axis2.addColumn 'Wed'
        axis2.addColumn 'Thu'
        axis2.addColumn 'Fri'
        axis2.addColumn 'Mon'
        axis2.addColumn 'Sun'
        axis2.addColumn 'Whoops'
        axis2.deleteColumn 'Wed'

        axis2.updateColumns axis
        assert 7 == axis2.size()
        assert 'Mon' == axis2.columns.get(0).value
        assert 'Tue' == axis2.columns.get(1).value
        assert 'Wed' == axis2.columns.get(2).value
        assert 'Thu' == axis2.columns.get(3).value
        assert 'Fri' == axis2.columns.get(4).value
        assert 'Sat' == axis2.columns.get(5).value
        assert 'Sun' == axis2.columns.get(6).value
    }

    @Test
    public void testUpColumnsMaintainsOrderWithDefault()
    {
        Axis axis = new Axis('days', AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY, 1)
        axis.addColumn 'Mon'
        axis.addColumn 'Tue'
        Column wed = axis.addColumn 'Wed'
        wed.id = -wed.id
        axis.addColumn 'Thu'
        axis.addColumn 'Fri'
        axis.addColumn 'Sat'
        axis.addColumn 'Sun'

        // Mon/Sat backwards
        // Wed missing
        // Bogus column added (named 'Whoops')
        // Fix these problems with updateColumns (simulate user moving columns in NCE)
        Axis axis2 = new Axis('days', AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY, 1)
        axis2.addColumn 'Sat'
        axis2.addColumn 'Tue'
        axis2.addColumn 'Wed'
        axis2.addColumn 'Thu'
        axis2.addColumn 'Fri'
        axis2.addColumn 'Mon'
        axis2.addColumn 'Sun'
        axis2.addColumn 'Whoops'
        axis2.deleteColumn 'Wed'

        axis2.updateColumns axis
        assert 8 == axis2.size()
        assert 'Mon' == axis2.columns.get(0).value
        assert 'Tue' == axis2.columns.get(1).value
        assert 'Wed' == axis2.columns.get(2).value
        assert 'Thu' == axis2.columns.get(3).value
        assert 'Fri' == axis2.columns.get(4).value
        assert 'Sat' == axis2.columns.get(5).value
        assert 'Sun' == axis2.columns.get(6).value
        assertNull axis2.columns.get(7).value
        assert Integer.MAX_VALUE == axis2.columns.get(7).displayOrder
    }

    @Test
    public void testUpColumnsMaintainsIgnoresDefault()
    {
        Axis axis = new Axis('days', AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY, 1)
        axis.addColumn 'Mon'
        axis.addColumn 'Tue'
        Column wed = axis.addColumn 'Wed'
        wed.id = -wed.id
        axis.addColumn 'Thu'
        axis.addColumn 'Fri'
        axis.addColumn 'Sat'
        axis.addColumn 'Sun'

        // Mon/Sat backwards
        // Wed missing
        // Bogus column added (named 'Whoops')
        // Fix these problems with updateColumns (simulate user moving columns in NCE)
        Axis axis2 = new Axis('days', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 1)
        axis2.addColumn 'Sat'
        axis2.addColumn 'Tue'
        axis2.addColumn 'Wed'
        axis2.addColumn 'Thu'
        axis2.addColumn 'Fri'
        axis2.addColumn 'Mon'
        axis2.addColumn 'Sun'
        axis2.addColumn 'Whoops'
        axis2.deleteColumn 'Wed'

        axis2.updateColumns axis
        assert 7 == axis2.size()
        assert 'Mon' == axis2.columns.get(0).value
        assert 'Tue' == axis2.columns.get(1).value
        assert 'Wed' == axis2.columns.get(2).value
        assert 'Thu' == axis2.columns.get(3).value
        assert 'Fri' == axis2.columns.get(4).value
        assert 'Sat' == axis2.columns.get(5).value
        assert 'Sun' == axis2.columns.get(6).value
    }

    @Test
    public void testProveDefaultLast()
    {
        Axis axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.SORTED)
        axis.addColumn("alpha")
        axis.addColumn("charlie")
        axis.addColumn("bravo")
        List<Column> cols = axis.columns
        assertEquals(cols.get(0).value, "alpha")
        assertEquals(cols.get(1).value, "bravo")
        assertEquals(cols.get(2).value, "charlie")
        assertEquals(cols.get(3).value, null)

        axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.SORTED)
        axis.addColumn("alpha")
        axis.addColumn("charlie")
        axis.addColumn("bravo")
        cols = axis.columns
        assertEquals(3, cols.size())
        assertEquals(cols.get(0).value, "alpha")
        assertEquals(cols.get(1).value, "bravo")
        assertEquals(cols.get(2).value, "charlie")

        axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY)
        axis.addColumn("alpha")
        axis.addColumn("charlie")
        axis.addColumn("bravo")
        cols = axis.columns
        assertEquals(cols.get(0).value, "alpha")
        assertEquals(cols.get(1).value, "charlie")
        assertEquals(cols.get(2).value, "bravo")
        assertEquals(cols.get(3).value, null)

        axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY)
        axis.addColumn("alpha")
        axis.addColumn("charlie")
        axis.addColumn("bravo")
        cols = axis.columns
        assertEquals(3, cols.size())
        assertEquals(cols.get(0).value, "alpha")
        assertEquals(cols.get(1).value, "charlie")
        assertEquals(cols.get(2).value, "bravo")
    }

    private boolean isValidRange(Axis axis, Range range)
    {
        try
        {
            axis.addColumn(range);
            return true;
        }
        catch (AxisOverlapException e)
        {
            return false;
        }
    }
}