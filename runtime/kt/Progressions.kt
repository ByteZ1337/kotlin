// Generated by org.jetbrains.jet.generators.runtime.progressions.GenerateProgressions

package jet

public class ByteProgression(
        public override val start: Byte,
        public override val end: Byte,
        public override val increment: Int
) : Progression<Byte> {
    {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): ByteIterator = ByteProgressionIterator(start, end, increment)

    fun equals(other: Any?): Boolean =
        other is ByteProgression && start == other.start && end == other.end && increment == other.increment

    fun hashCode() = 31 * (31 * start.toInt() + end) + increment

    fun toString() = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

public class CharProgression(
        public override val start: Char,
        public override val end: Char,
        public override val increment: Int
) : Progression<Char> {
    {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): CharIterator = CharProgressionIterator(start, end, increment)

    fun equals(other: Any?): Boolean =
        other is CharProgression && start == other.start && end == other.end && increment == other.increment

    fun hashCode() = 31 * (31 * start.toInt() + end) + increment

    fun toString() = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

public class ShortProgression(
        public override val start: Short,
        public override val end: Short,
        public override val increment: Int
) : Progression<Short> {
    {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): ShortIterator = ShortProgressionIterator(start, end, increment)

    fun equals(other: Any?): Boolean =
        other is ShortProgression && start == other.start && end == other.end && increment == other.increment

    fun hashCode() = 31 * (31 * start.toInt() + end) + increment

    fun toString() = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

public class IntProgression(
        public override val start: Int,
        public override val end: Int,
        public override val increment: Int
) : Progression<Int> {
    {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): IntIterator = IntProgressionIterator(start, end, increment)

    fun equals(other: Any?): Boolean =
        other is IntProgression && start == other.start && end == other.end && increment == other.increment

    fun hashCode() = 31 * (31 * start + end) + increment

    fun toString() = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

public class LongProgression(
        public override val start: Long,
        public override val end: Long,
        public override val increment: Long
) : Progression<Long> {
    {
        if (increment == 0L) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): LongIterator = LongProgressionIterator(start, end, increment)

    fun equals(other: Any?): Boolean =
        other is LongProgression && start == other.start && end == other.end && increment == other.increment

    fun hashCode() = (31 * (31 * (start xor (start ushr 32)) + (end xor (end ushr 32))) + (increment xor (increment ushr 32))).toInt()

    fun toString() = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

public class FloatProgression(
        public override val start: Float,
        public override val end: Float,
        public override val increment: Float
) : Progression<Float> {
    {
        if (java.lang.Float.isNaN(increment)) throw IllegalArgumentException("Increment must be not NaN")
        if (increment == 0.0f) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): FloatIterator = FloatProgressionIterator(start, end, increment)

    fun equals(other: Any?): Boolean =
        other is FloatProgression && java.lang.Float.compare(start, other.start) == 0 && java.lang.Float.compare(end, other.end) == 0 && java.lang.Float.compare(increment, other.increment) == 0

    fun hashCode() = (31 * (31 * (if (start != 0.0f) java.lang.Float.floatToIntBits(start) else 0) +
        (if (end != 0.0f) java.lang.Float.floatToIntBits(end) else 0)) + 
        (if (increment != 0.0f) java.lang.Float.floatToIntBits(increment) else 0)).toInt()

    fun toString() = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

public class DoubleProgression(
        public override val start: Double,
        public override val end: Double,
        public override val increment: Double
) : Progression<Double> {
    {
        if (java.lang.Double.isNaN(increment)) throw IllegalArgumentException("Increment must be not NaN")
        if (increment == 0.0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): DoubleIterator = DoubleProgressionIterator(start, end, increment)

    fun equals(other: Any?): Boolean =
        other is DoubleProgression && java.lang.Double.compare(start, other.start) == 0 && java.lang.Double.compare(end, other.end) == 0 && java.lang.Double.compare(increment, other.increment) == 0

    fun hashCode(): Int {
        var temp = if (start != 0.0) java.lang.Double.doubleToLongBits(start) else 0L
        var result = (temp xor (temp ushr 32))
        temp = if (end != 0.0) java.lang.Double.doubleToLongBits(end) else 0L
        result = 31 * result + (temp xor (temp ushr 32))
        temp = if (increment != 0.0) java.lang.Double.doubleToLongBits(increment) else 0L
        return (31 * result + (temp xor (temp ushr 32))).toInt()
    }

    fun toString() = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

